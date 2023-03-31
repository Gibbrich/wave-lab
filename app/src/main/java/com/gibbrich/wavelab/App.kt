package com.gibbrich.wavelab

import android.app.Application
import com.gibbrich.wavelab.di.AppComponent
import com.gibbrich.wavelab.di.AppModule
import com.gibbrich.wavelab.di.DI
import com.gibbrich.wavelab.di.DaggerAppComponent


open class App: Application() {
  override fun onCreate() {
    super.onCreate()

    DI.init(provideAppComponent())
  }

  open fun provideAppComponent(): AppComponent = DaggerAppComponent
    .builder()
    .appModule(AppModule(this))
    .build()
}
