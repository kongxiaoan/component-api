package com.kpa.component_api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.alibaba.android.arouter.launcher.ARouter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.test).setOnClickListener {
            ARouter.getInstance().build("/test/main").navigation()
        }
        findViewById<Button>(R.id.user).setOnClickListener {
            ARouter.getInstance().build("/user/login").navigation()
        }
    }
}