#!/usr/bin/env python3
"""
tools/project_index.py - Project Intelligence and Fast Code Navigation Tool for AuraMusic

Fast, zero-dependency CLI tool for AI coding agents and developers to query,
validate, and navigate AuraMusic architecture components, dependencies, and file paths.

Usage:
    python tools/project_index.py search <query>
    python tools/project_index.py show <component-id>
    python tools/project_index.py validate
    python tools/project_index.py check-paths
    python tools/project_index.py list [--type <type>]
    python tools/project_index.py stats
"""

import argparse
import json
import os
import sys
from pathlib import Path

# Force UTF-8 encoding on standard streams if possible
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Base directories
SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
INDEX_FILE = REPO_ROOT / "docs" / "architecture" / "COMPONENT_INDEX.json"

REQUIRED_FIELDS = [
    "id",
    "name",
    "type",
    "paths",
    "responsibility",
    "symbols",
    "depends_on",
    "related_tests",
    "keywords",
    "last_verified"
]

VALID_TYPES = {
    "app",
    "service",
    "manager",
    "engine",
    "repository",
    "viewmodel",
    "screen",
    "component",
    "bridge",
    "network",
    "crypto",
    "util",
    "di",
    "filter",
    "cache"
}


def load_index():
    if not INDEX_FILE.exists():
        print(f"[ERROR] Index file not found at: {INDEX_FILE}", file=sys.stderr)
        print("[HINT] Ensure docs/architecture/COMPONENT_INDEX.json exists.", file=sys.stderr)
        sys.exit(1)
    try:
        with open(INDEX_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as e:
        print(f"[ERROR] Invalid JSON in {INDEX_FILE}: {e}", file=sys.stderr)
        sys.exit(1)


def cmd_search(args, data):
    query = args.query.lower().strip()
    components = data.get("components", [])
    matches = []

    for comp in components:
        score = 0
        match_reasons = []

        # Check ID
        if query in comp.get("id", "").lower():
            score += 10
            match_reasons.append("id")

        # Check Name
        if query in comp.get("name", "").lower():
            score += 8
            match_reasons.append("name")

        # Check Symbols
        for sym in comp.get("symbols", []):
            if query in sym.lower():
                score += 7
                match_reasons.append(f"symbol:{sym}")
                break

        # Check Keywords
        for kw in comp.get("keywords", []):
            if query in kw.lower():
                score += 5
                match_reasons.append(f"kw:{kw}")
                break

        # Check Paths
        for p in comp.get("paths", []):
            if query in p.lower():
                score += 6
                match_reasons.append(f"path:{Path(p).name}")
                break

        # Check Responsibility
        if query in comp.get("responsibility", "").lower():
            score += 3
            match_reasons.append("responsibility")

        if score > 0:
            matches.append((score, comp, match_reasons))

    # Sort by score descending
    matches.sort(key=lambda x: x[0], reverse=True)

    if not matches:
        print(f"No components found matching '{args.query}'.")
        print("Try broader keywords, e.g.: playback, vlc, online, lyrics, queue, coloros, repository")
        return

    print(f"Found {len(matches)} matching component(s) for '{args.query}':\n")
    for score, comp, reasons in matches:
        comp_id = comp.get("id")
        name = comp.get("name")
        comp_type = comp.get("type", "unknown")
        paths = comp.get("paths", [])
        primary_path = paths[0] if paths else "no-path"
        matched_by = ", ".join(reasons[:3])

        print(f"* [{comp_id}] {name} ({comp_type})")
        print(f"    Path: {primary_path}")
        print(f"    Responsibility: {comp.get('responsibility', '')}")
        print(f"    Matched by: {matched_by}")
        deps = comp.get("depends_on", [])
        if deps:
            print(f"    Depends on: {', '.join(deps)}")
        print()


def cmd_show(args, data):
    target_id = args.component_id.strip()
    components = data.get("components", [])
    comp = next((c for c in components if c.get("id") == target_id), None)

    if not comp:
        print(f"[ERROR] Component with ID '{target_id}' not found.", file=sys.stderr)
        # Suggest close matches
        similar = [c.get("id") for c in components if target_id.lower() in c.get("id", "").lower()]
        if similar:
            print(f"Did you mean: {', '.join(similar)}?", file=sys.stderr)
        sys.exit(1)

    print(f"============================================================")
    print(f"Component: {comp.get('name')} [{comp.get('id')}]")
    print(f"Type:      {comp.get('type')}")
    print(f"Verified:  {comp.get('last_verified')}")
    print(f"============================================================")
    print(f"Responsibility:\n  {comp.get('responsibility')}\n")

    print("Source Files:")
    for p in comp.get("paths", []):
        exists = (REPO_ROOT / p).exists()
        status = "[EXISTS]" if exists else "[MISSING]"
        print(f"  {status} {p}")
    print()

    print("Key Symbols:")
    for s in comp.get("symbols", []):
        print(f"  * {s}")
    print()

    deps = comp.get("depends_on", [])
    print(f"Depends On: {', '.join(deps) if deps else 'None'}")

    # Reverse dependencies
    dependents = [c.get("id") for c in components if target_id in c.get("depends_on", [])]
    print(f"Depended On By: {', '.join(dependents) if dependents else 'None'}\n")

    tests = comp.get("related_tests", [])
    print(f"Related Tests:")
    if tests:
        for t in tests:
            exists = (REPO_ROOT / t).exists()
            status = "[EXISTS]" if exists else "[MISSING]"
            print(f"  {status} {t}")
    else:
        print("  None indexed")
    print()

    keywords = comp.get("keywords", [])
    print(f"Keywords: {', '.join(keywords)}")


def cmd_validate(args, data):
    components = data.get("components", [])
    errors = []
    warnings = []
    known_ids = set()

    # Check top-level metadata
    if "version" not in data:
        warnings.append("Missing top-level 'version' field.")
    if "last_updated" not in data:
        warnings.append("Missing top-level 'last_updated' field.")

    # First pass: collect IDs and check duplicates
    for i, comp in enumerate(components):
        cid = comp.get("id")
        if not cid:
            errors.append(f"Component #{i} is missing required 'id' field.")
        elif cid in known_ids:
            errors.append(f"Duplicate component ID: '{cid}'.")
        else:
            known_ids.add(cid)

    # Second pass: check fields and dependency integrity
    for comp in components:
        cid = comp.get("id", "unknown")

        for field in REQUIRED_FIELDS:
            if field not in comp:
                errors.append(f"Component '{cid}' is missing required field: '{field}'.")

        ctype = comp.get("type")
        if ctype and ctype not in VALID_TYPES:
            warnings.append(f"Component '{cid}' has unconventional type '{ctype}'. Expected one of {sorted(VALID_TYPES)}")

        # Check dependencies
        for dep in comp.get("depends_on", []):
            if dep not in known_ids:
                errors.append(f"Component '{cid}' references unknown dependency: '{dep}'.")

        # Check paths format
        paths = comp.get("paths", [])
        if not isinstance(paths, list) or len(paths) == 0:
            errors.append(f"Component '{cid}' must specify at least one path in 'paths' list.")

    print(f"Validation Results for {INDEX_FILE.name}:")
    print(f"  Total Components: {len(components)}")
    print(f"  Total Errors:     {len(errors)}")
    print(f"  Total Warnings:   {len(warnings)}")

    if errors:
        print("\n[ERRORS]:")
        for err in errors:
            print(f"  [FAIL] {err}")

    if warnings:
        print("\n[WARNINGS]:")
        for w in warnings:
            print(f"  [WARN] {w}")

    if errors:
        sys.exit(1)
    else:
        print("\n[OK] Index schema and referential integrity PASSED.")


def cmd_check_paths(args, data):
    components = data.get("components", [])
    missing_paths = []
    total_paths = 0

    for comp in components:
        cid = comp.get("id")
        # Check source paths
        for p in comp.get("paths", []):
            total_paths += 1
            full_path = REPO_ROOT / p
            if not full_path.exists():
                missing_paths.append((cid, p, "source"))

        # Check test paths
        for t in comp.get("related_tests", []):
            total_paths += 1
            full_path = REPO_ROOT / t
            if not full_path.exists():
                missing_paths.append((cid, t, "test"))

    print(f"Path Check Results across {len(components)} components:")
    print(f"  Total Indexed Paths: {total_paths}")
    print(f"  Missing Paths:       {len(missing_paths)}")

    if missing_paths:
        print("\n[MISSING PATHS]:")
        for cid, path, ptype in missing_paths:
            print(f"  [FAIL] [{cid}] ({ptype}) Path does not exist: {path}")
        sys.exit(1)
    else:
        print("\n[OK] All indexed source and test paths EXIST on disk.")


def cmd_list(args, data):
    components = data.get("components", [])
    target_type = getattr(args, "type", None)

    if target_type:
        components = [c for c in components if c.get("type") == target_type]

    print(f"Indexed Components ({len(components)} total):\n")
    fmt = "{:<28} {:<12} {:<32} {:<35}"
    print(fmt.format("ID", "TYPE", "PRIMARY SYMBOL", "PRIMARY PATH"))
    print("-" * 110)

    for c in components:
        cid = c.get("id", "")
        ctype = c.get("type", "")
        sym = (c.get("symbols", []) or [""])[0]
        path = (c.get("paths", []) or [""])[0]
        # Shorten path if needed
        if len(path) > 34:
            path = "..." + path[-31:]
        print(fmt.format(cid, ctype, sym[:31], path))


def cmd_stats(args, data):
    components = data.get("components", [])
    types = {}
    total_paths = set()
    total_symbols = 0
    total_tests = set()

    for c in components:
        t = c.get("type", "unknown")
        types[t] = types.get(t, 0) + 1
        for p in c.get("paths", []):
            total_paths.add(p)
        total_symbols += len(c.get("symbols", []))
        for test in c.get("related_tests", []):
            total_tests.add(test)

    print("============================================================")
    print(" AuraMusic Component Index Statistics")
    print("============================================================")
    print(f"Total Components:     {len(components)}")
    print(f"Unique Source Files:  {len(total_paths)}")
    print(f"Total Symbols:        {total_symbols}")
    print(f"Associated Tests:     {len(total_tests)}")
    print("\nBreakdown by Component Type:")
    for ctype, count in sorted(types.items(), key=lambda x: x[1], reverse=True):
        print(f"  * {ctype:<15}: {count}")
    print("============================================================")


def main():
    parser = argparse.ArgumentParser(
        description="AuraMusic Project Intelligence and Code Navigation Tool"
    )
    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    # search
    search_parser = subparsers.add_parser("search", help="Search components by keyword/symbol/path")
    search_parser.add_argument("query", help="Search keyword, symbol, ID, or file name")

    # show
    show_parser = subparsers.add_parser("show", help="Show full details for a component ID")
    show_parser.add_argument("component_id", help="Exact component ID")

    # validate
    subparsers.add_parser("validate", help="Validate schema and dependency integrity")

    # check-paths
    subparsers.add_parser("check-paths", help="Verify all indexed file paths exist")

    # list
    list_parser = subparsers.add_parser("list", help="List indexed components")
    list_parser.add_argument("--type", help="Filter by component type", default=None)

    # stats
    subparsers.add_parser("stats", help="Display index statistics")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(0)

    data = load_index()

    if args.command == "search":
        cmd_search(args, data)
    elif args.command == "show":
        cmd_show(args, data)
    elif args.command == "validate":
        cmd_validate(args, data)
    elif args.command == "check-paths":
        cmd_check_paths(args, data)
    elif args.command == "list":
        cmd_list(args, data)
    elif args.command == "stats":
        cmd_stats(args, data)


if __name__ == "__main__":
    main()
