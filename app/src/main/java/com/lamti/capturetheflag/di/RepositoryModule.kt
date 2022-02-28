package com.lamti.capturetheflag.di

import com.lamti.capturetheflag.data.FirestoreRepositoryImpl
import com.lamti.capturetheflag.data.anchors.CloudAnchorRepositoryImpl
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCloudAnchorRepository(cloudAnchorRepositoryImpl: CloudAnchorRepositoryImpl): CloudAnchorRepository

    @Binds
    abstract fun bindFirestoreRepository(firestoreRepositoryImpl: FirestoreRepositoryImpl): FirestoreRepository

}
