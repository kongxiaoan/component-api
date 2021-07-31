package com.kpa.user

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route

/**
 * @author      kpa
 * @date        2021/7/31 3:26 下午
 * @email       billkp@yeah.net
 * @description
 **/
@Route(path = "/user/data")
class UserServiceImpl: UserService {
    override fun checkLogin(): Boolean {
        return false
    }

    override fun getUserInfo(): UserInfo {
        return UserInfo("kpa", 1000)
    }

    override fun init(context: Context?) {}
}