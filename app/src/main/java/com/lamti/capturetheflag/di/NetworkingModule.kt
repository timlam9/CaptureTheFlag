package com.lamti.capturetheflag.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lamti.capturetheflag.data.anchors.CloudAnchorRepositoryImpl
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.data.firestore.FirebaseDatabaseRepository
import com.lamti.capturetheflag.data.firestore.FirestoreRepositoryImpl
import com.lamti.capturetheflag.data.firestore.GamesRepository
import com.lamti.capturetheflag.data.firestore.PlayersRepository
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.GameEngine
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkingModule {

    @Singleton
    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Singleton
    @Provides
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance("https://corded-racer-341709-default-rtdb.europe-west1.firebasedatabase.app")

    @Singleton
    @Provides
    fun provideFirebaseAuthentication(): FirebaseAuth = Firebase.auth

    @Singleton
    @Provides
    fun provideFirebaseAuthenticationRepository(
        auth: FirebaseAuth,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AuthenticationRepository = AuthenticationRepository(auth, ioDispatcher)

    @Singleton
    @Provides
    fun provideDatabaseRepository(
        database: FirebaseDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FirebaseDatabaseRepository = FirebaseDatabaseRepository(database, ioDispatcher)

    @Singleton
    @Provides
    fun providePlayersRepository(
        firestore: FirebaseFirestore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PlayersRepository = PlayersRepository(firestore, ioDispatcher)

    @Singleton
    @Provides
    fun provideGamesRepository(
        firestore: FirebaseFirestore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): GamesRepository = GamesRepository(firestore, ioDispatcher)

    @Singleton
    @Provides
    fun provideCloudAnchorRepository(gamesRepository: GamesRepository): CloudAnchorRepository =
        CloudAnchorRepositoryImpl(gamesRepository)

    @Singleton
    @Provides
    fun provideFirestoreRepository(
        authenticationRepository: AuthenticationRepository,
        databaseRepository: FirebaseDatabaseRepository,
        playersRepository: PlayersRepository,
        gamesRepository: GamesRepository,
    ): FirestoreRepository = FirestoreRepositoryImpl(
        authenticationRepository = authenticationRepository,
        databaseRepository = databaseRepository,
        playersRepository = playersRepository,
        gamesRepository = gamesRepository,
    )

    @Singleton
    @Provides
    fun provideGameEngine(
        firestoreRepository: FirestoreRepository,
        locationRepository: LocationRepository,
        geofencingRepository: GeofencingRepository,
        coroutineScope: CoroutineScope
    ): GameEngine = GameEngine(
        firestoreRepository = firestoreRepository,
        locationRepository = locationRepository,
        geofencingRepository = geofencingRepository,
        coroutineScope = coroutineScope
    )
}
