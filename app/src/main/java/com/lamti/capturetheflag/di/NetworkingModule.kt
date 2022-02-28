package com.lamti.capturetheflag.di


import android.content.Context
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelper
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    fun provideGeofencingClient(@ApplicationContext context: Context) = LocationServices.getGeofencingClient(context)

    @Provides
    fun provideGeofencingHelper(geofencingClient: GeofencingClient): GeofencingHelper = GeofencingHelperImpl(geofencingClient)

}
