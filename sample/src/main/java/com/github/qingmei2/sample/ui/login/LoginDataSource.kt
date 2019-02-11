package com.github.qingmei2.sample.ui.login

import arrow.core.Either
import com.github.qingmei2.mvi.base.repository.BaseRepositoryBoth
import com.github.qingmei2.mvi.base.repository.ILocalDataSource
import com.github.qingmei2.mvi.base.repository.IRemoteDataSource
import com.github.qingmei2.sample.entity.Errors
import com.github.qingmei2.sample.entity.LoginEntity
import com.github.qingmei2.sample.entity.LoginUser
import com.github.qingmei2.sample.http.scheduler.SchedulerProvider
import com.github.qingmei2.sample.http.service.ServiceManager
import com.github.qingmei2.sample.manager.PrefsHelper
import com.github.qingmei2.sample.manager.UserManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface ILoginRemoteDataSource : IRemoteDataSource {

    fun login(username: String, password: String): Flowable<Either<Errors, LoginUser>>
}

interface ILoginLocalDataSource : ILocalDataSource {

    fun savePrefsUser(username: String,
                      password: String): Completable

    fun fetchPrefsUser(): Flowable<Either<Errors, LoginEntity>>

    fun isAutoLogin(): Single<Boolean>
}

class LoginDataSourceRepository(
        remoteDataSource: ILoginRemoteDataSource,
        localDataSource: ILoginLocalDataSource
) : BaseRepositoryBoth<ILoginRemoteDataSource, ILoginLocalDataSource>(remoteDataSource, localDataSource) {

    fun login(username: String, password: String): Flowable<Either<Errors, LoginUser>> =
            remoteDataSource.login(username, password)
                    .doOnNext { either ->
                        either.fold({

                        }, {
                            UserManager.INSTANCE = it
                        })
                    }
                    .flatMap {
                        localDataSource.savePrefsUser(username, password)  // save user
                                .andThen(Flowable.just(it))
                    }

    fun prefsUser(): Flowable<Either<Errors, LoginEntity>> =
            localDataSource.fetchPrefsUser()

    fun prefsAutoLogin(): Single<Boolean> =
            localDataSource.isAutoLogin()
}

class LoginRemoteDataSource(
        private val serviceManager: ServiceManager,
        private val schedulers: SchedulerProvider
) : ILoginRemoteDataSource {

    override fun login(username: String, password: String): Flowable<Either<Errors, LoginUser>> =
            serviceManager.loginService
                    .login(username, password)
                    .subscribeOn(schedulers.io())
                    .map {
                        Either.right(it)
                    }
}

class LoginLocalDataSource(
        private val prefs: PrefsHelper
) : ILoginLocalDataSource {

    override fun isAutoLogin(): Single<Boolean> =
            Single.just(prefs.autoLogin)

    override fun savePrefsUser(username: String, password: String): Completable =
            Completable.fromAction {
                prefs.username = username
                prefs.password = password
            }

    override fun fetchPrefsUser(): Flowable<Either<Errors, LoginEntity>> =
            Flowable.just(prefs)
                    .map {
                        when (it.username.isNotEmpty() && it.password.isNotEmpty()) {
                            true -> Either.right(
                                    LoginEntity(it.username, it.password)
                            )
                            false -> Either.left(Errors.EmptyResultsError)
                        }
                    }
}