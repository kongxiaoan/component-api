package com.kpa.base

import android.app.Application
import com.alibaba.android.arouter.launcher.ARouter

/**
 * @author      kpa
 * @date        2021/7/31 3:10 下午
 * @email       billkp@yeah.net
 * @description
 **/
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initARouter()
    }

    private fun initARouter() {
        if (BuildConfig.DEBUG) {
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)
    }
}