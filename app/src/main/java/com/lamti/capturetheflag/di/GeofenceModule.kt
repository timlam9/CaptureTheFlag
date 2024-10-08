package com.lamti.capturetheflag.di

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.lamti.capturetheflag.data.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeofenceModule {

    @Provides
    @Singleton
    fun provideGeofencingClient(@ApplicationContext context: Context) = LocationServices.getGeofencingClient(context)

    @SuppressLint("UnspecifiedImmutableFlag")
    @Provides
    @Singleton
    fun provideGeofencePendingIntent(@ApplicationContext context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(context, 0, intent, flag)
    }

    @Provides
    @Singleton
    fun provideGeofencingRepository(
        geofencingClient: GeofencingClient,
        geofencePendingIntent: PendingIntent
    ): GeofencingRepository =
        GeofencingRepository(geofencingClient, geofencePendingIntent)

}
