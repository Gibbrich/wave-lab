package com.gibbrich.wavelab.di

object DI {
    lateinit var appComponent: AppComponent
        private set

    fun init(appComponent: AppComponent) {
        DI.appComponent = appComponent
    }
}