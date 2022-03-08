package com.lamti.capturetheflag.di


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.data.firestore.FirestoreRepositoryImpl
import com.lamti.capturetheflag.data.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    fun provideFirebaseAuthentication(): FirebaseAuth = Firebase.auth

    @Provides
    fun provideFirebaseAuthenticationRepository(auth: FirebaseAuth) = AuthenticationRepository(auth)

    @Provides
    fun provideFirestoreRepository(
        firestore: FirebaseFirestore,
        authenticationRepository: AuthenticationRepository
    ): FirestoreRepository = FirestoreRepositoryImpl(firestore, authenticationRepository)

}

@Module
@InstallIn(SingletonComponent::class)
object GeofenceModule {

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
    fun provideGeofencingRepository(
        geofencingClient: GeofencingClient,
        geofencePendingIntent: PendingIntent
    ): GeofencingRepository =
        GeofencingRepository(geofencingClient, geofencePendingIntent)

}
