package com.gibbrich.wavelab.di

import com.gibbrich.wavelab.activity.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun inject(entry: MainActivity)
}