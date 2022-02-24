package com.lamti.capturetheflag.di

import com.lamti.capturetheflag.data.CloudAnchorRepositoryImpl
import com.lamti.capturetheflag.domain.CloudAnchorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCloudAnchorRepository(cloudAnchorRepositoryImpl: CloudAnchorRepositoryImpl): CloudAnchorRepository

}
