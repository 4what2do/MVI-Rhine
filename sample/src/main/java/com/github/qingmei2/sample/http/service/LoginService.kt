package com.github.qingmei2.sample.http.service

import com.github.qingmei2.sample.entity.LoginUser
import io.reactivex.Flowable
import retrofit2.http.GET

interface LoginService {

    @GET("user")
    fun login(): Flowable<LoginUser>
}