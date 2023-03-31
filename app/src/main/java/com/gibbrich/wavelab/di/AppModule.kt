package com.gibbrich.wavelab.di

import android.content.Context
import com.gibbrich.wavelab.data.ResourceManager
import com.gibbrich.wavelab.data.ResourceManagerImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
@Singleton
class AppModule(private val appContext: Context) {

    @Provides
    @Singleton
    fun provideContext() = appContext

    @Provides
    @Singleton
    fun provideResourceManager(): ResourceManager = ResourceManagerImpl(appContext)
}