package com.lamti.capturetheflag.di

import com.lamti.capturetheflag.presentation.location.LocationService
import com.lamti.capturetheflag.presentation.location.LocationServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Module
@InstallIn(ActivityComponent::class)
abstract class NotificationServiceModule {

    @ExperimentalCoroutinesApi
    @Binds
    abstract fun bindLocationService(service: LocationServiceImpl): LocationService

}
