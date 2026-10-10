# AuraMusic AI Coding Agent Workflow

Standardized, step-by-step operating protocol for AI coding assistants working in the AuraMusic Android repository.

---

## The 10-Step Development Protocol

```
1. Consult AGENTS.md
       │
       ▼
2. Query Index (tools/project_index.py search <keyword>)
       │
       ▼
3. Open Target Files & Direct Dependencies Only
       │
       ▼
4. Inspect Related Tests & Existing Call Sites
       │
       ▼
5. Formulate a Minimal, Focused Plan
       │
       ▼
6. Implement Minimal, Surgical Code Changes
       │
       ▼
7. Run Verification Checks (Gradle compile & unit tests)
       │
       ▼
8. Review Diff (git diff) for Unintended Regressions
       │
       ▼
9. Update Documentation & Component Index (if modified)
       │
       ▼
10. Deliver Report (Root cause, diffs, test results)
```

---

## Detailed Step Guidance

### Step 1: Read `AGENTS.md`
- Always verify technical constraints: Kotlin version, SDK targets (`compileSdk 36`, `minSdk 26`), dependency versions (Media3 1.2.1, LibVLC 3.6.0), and architectural boundaries.
- Adhere to the core rule: **Never rewrite existing functional architecture; make targeted repairs.**

### Step 2: Query the Component Index
- Do **NOT** run wide filesystem traversals or read entire directories.
- Run the local query tool:
  ```bash
  python tools/project_index.py search <keyword-or-symbol>
  python tools/project_index.py show <component-id>
  ```
- Obtain the exact file paths, class names, and dependencies immediately.

### Step 3: Open Target Files & Direct Dependencies Only
- Limit file reading to the target file and directly related dependencies identified in the index.
- Read files in chunks or slices using line numbers rather than reading entire 2000-line files.

### Step 4: Inspect Related Tests & Call Sites
- Check the `related_tests` field of the component.
- Review existing unit tests under `app/src/test/java/com/aman/auramusic/` to understand expected behavior and regression guards.

### Step 5: Formulate a Minimal Implementation Plan
- Clearly state the hypothesis or root cause.
- Identify the exact lines of code that need modification.
- Avoid multi-file refactoring when a localized fix solves the problem.

### Step 6: Make Surgical Code Changes
- Apply changes using targeted replace tools (`replace_file_content`).
- Preserve existing comments, docstrings, and non-target logic.
- Avoid introducing arbitrary delays (e.g. `delay(3000)` hacks).
- Avoid competing mechanisms or duplicate listeners.

### Step 7: Run Focused Verification Commands
Execute the verified project Gradle commands directly:
```bash
# Verify Kotlin compilation
./gradlew.bat compileDebugKotlin

# Run unit tests
./gradlew.bat testDebugUnitTest

# Assemble debug APK (if verifying full packaging)
./gradlew.bat assembleDebug
```
Do not claim an issue is fixed unless the build passes and tests succeed.

### Step 8: Review Diff for Regressions
- Run `git diff` or `git status` to verify that no unrelated files, IDE metadata, or formatting changes were touched.
- Ensure only intentional edits are staged.

### Step 9: Update Documentation & Index (If Relevant)
- If a new component or symbol was introduced: add it to `docs/architecture/COMPONENT_INDEX.json` and verify with `python tools/project_index.py validate`.
- If a known bug was resolved or discovered: update `docs/architecture/KNOWN_ISSUES.md`.
- If an architectural decision was altered: record it in `docs/architecture/CHANGELOG.md`.

### Step 10: Deliver Concise, Clear Report
- State the verified root cause clearly.
- Highlight the exact files modified with clickable markdown links (`[File](file:///path)`).
- Present key code changes.
- Report all passed tests and any remaining limitations.

---

## Index Maintenance & Self-Repair Rules

1. **Active Code is Ground Truth**: If you discover that a class path or symbol has changed in active code while the index is outdated, the active code is always authoritative.
2. **Repair, Don't Ignore**: When you discover a stale entry, update `docs/architecture/COMPONENT_INDEX.json` to keep the project intelligence system healthy for future tasks.
3. **Verify Integrity**: Always run:
   ```bash
   python tools/project_index.py validate
   python tools/project_index.py check-paths
   ```
   to guarantee zero broken links and zero dangling dependencies.
