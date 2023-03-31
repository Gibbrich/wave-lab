package com.gibbrich.wavelab.di

import com.gibbrich.wavelab.main.MainViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun inject(entry: MainViewModel)
}