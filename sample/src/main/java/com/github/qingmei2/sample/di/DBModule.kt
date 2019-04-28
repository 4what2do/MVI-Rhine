package com.github.qingmei2.sample.di

import androidx.room.Room
import com.github.qingmei2.sample.base.BaseApplication
import com.github.qingmei2.sample.db.UserDatabase
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

private const val DB_MODULE_TAG = "DBModule"

val dbModule = Kodein.Module(DB_MODULE_TAG) {

    bind<UserDatabase>() with singleton {
        Room.databaseBuilder(BaseApplication.INSTANCE, UserDatabase::class.java, "user")
            .fallbackToDestructiveMigration()
            .build()
    }
}