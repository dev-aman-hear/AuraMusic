package com.aman.auramusic.di

import android.content.Context
import androidx.room.Room
import com.aman.auramusic.data.local.PlaylistDao
import com.aman.auramusic.data.local.PlaylistDatabase
import com.aman.auramusic.data.remote.PlaylistApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    fun providePlaylistApi(okHttpClient: OkHttpClient): PlaylistApi {
        return Retrofit.Builder()
            .baseUrl("https://yourserver.com/") // Replace with actual server URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlaylistApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlaylistDatabase {
        return Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            "aura_music_db"
        ).build()
    }

    @Provides
    fun providePlaylistDao(database: PlaylistDatabase): PlaylistDao {
        return database.playlistDao()
    }
}
