package com.kpa.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.kpa.user.UserService


@Route(path = "/test/main")
class TestActivity : AppCompatActivity() {

    @Autowired
    lateinit var mUserService: UserService

    override fun onCreate(savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        findViewById<TextView>(R.id.userName).text = mUserService?.getUserInfo()?.userName
        findViewById<TextView>(R.id.userId).text = mUserService?.getUserInfo()?.id.toString()
    }
}