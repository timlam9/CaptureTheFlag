package com.lamti.capturetheflag.di

import com.lamti.capturetheflag.presentation.location.service.LocationService
import com.lamti.capturetheflag.presentation.location.service.LocationServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@Module
@InstallIn(ActivityComponent::class)
abstract class NotificationServiceModule {

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    @Binds
    abstract fun bindLocationService(service: LocationServiceImpl): LocationService

}
