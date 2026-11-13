package com.carijajan.app

import android.app.Application
import android.content.Context
import com.carijajan.app.data.ApiClient
import com.carijajan.app.data.ApiService
import com.carijajan.app.data.AuthRepository
import com.carijajan.app.data.TokenStore
import com.carijajan.app.data.VendorRepository

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)
    val api: ApiService = ApiClient.create(tokenStore)
    val authRepository = AuthRepository(api, tokenStore)
    val vendorRepository = VendorRepository(api)
}

class CariJajanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
