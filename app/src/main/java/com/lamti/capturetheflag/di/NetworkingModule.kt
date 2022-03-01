package com.lamti.capturetheflag.di


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lamti.capturetheflag.data.FirestoreRepositoryImpl
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.presentation.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelper
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelperImpl
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
object NetworkingModule {

    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    fun provideGeofencingClient(@ApplicationContext context: Context) = LocationServices.getGeofencingClient(context)

    @SuppressLint("UnspecifiedImmutableFlag")
    @Provides
    @Singleton
    fun provideGeofencePendingIntent(@ApplicationContext context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @Provides
    fun provideGeofencingHelper(
        geofencingClient: GeofencingClient,
        geofencePendingIntent: PendingIntent
    ): GeofencingHelper =
        GeofencingHelperImpl(geofencingClient, geofencePendingIntent)

    @ExperimentalCoroutinesApi
    @Provides
    fun provideFirestoreRepository(firestore: FirebaseFirestore): FirestoreRepository = FirestoreRepositoryImpl(firestore)

}
