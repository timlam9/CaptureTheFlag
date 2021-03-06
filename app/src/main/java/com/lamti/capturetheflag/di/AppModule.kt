package com.lamti.capturetheflag.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl
import com.lamti.capturetheflag.data.location.service.NotificationHelper
import com.lamti.capturetheflag.presentation.ui.DatastoreHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideDataStoreHelper(@ApplicationContext context: Context): DatastoreHelper = DatastoreHelper(context)

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper = NotificationHelper(context)

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationRepository(client: FusedLocationProviderClient, scope: CoroutineScope): LocationRepository =
        LocationRepository(client, scope)

    @Provides
    @Singleton
    fun provideLocationService(): LocationServiceImpl = LocationServiceImpl()

}
