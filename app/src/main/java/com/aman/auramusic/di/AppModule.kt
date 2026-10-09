package com.aman.auramusic.di

import android.content.Context
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.data.repository.MusicRepository
import com.aman.auramusic.data.repository.UserPreferencesRepository
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(@ApplicationContext context: Context): MusicRepository {
        return MusicRepository(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(): LyricsRepository {
        return LyricsRepository()
    }

    @Provides
    @Singleton
    fun provideOnlineMusicRepository(): OnlineMusicRepository {
        return OnlineMusicRepository()
    }

    @Provides
    @Singleton
    fun provideYouTubeArtistRepository(
        onlineRepository: OnlineMusicRepository
    ): com.aman.auramusic.online.network.repository.YouTubeArtistRepository {
        return com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl(
            pipedService = com.aman.auramusic.online.network.piped.PipedService(),
            onlineRepository = onlineRepository
        )
    }
}
