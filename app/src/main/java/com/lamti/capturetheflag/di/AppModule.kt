package com.lamti.capturetheflag.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.lamti.capturetheflag.presentation.location.service.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper = NotificationHelper(context)

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

}
