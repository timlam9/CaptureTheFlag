package com.lamti.capturetheflag.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatastoreHelper(private val context: Context) {

    companion object {

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("captureTheFlagDataStore")
        val HAS_PREFERENCES = booleanPreferencesKey("has_preferences")
        val IS_LOADING = booleanPreferencesKey("is_loading")
        val INITIAL_SCREEN = stringPreferencesKey("initial_screen")
    }

    val hasPreferences: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_PREFERENCES] ?: false
        }

    suspend fun saveHasPreferences(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_PREFERENCES] = value
        }
    }

    val isLoading: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOADING] ?: false
        }

    suspend fun saveIsLoading(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOADING] = value
        }
    }

    val initialScreen: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_SCREEN] ?: "onboarding_screen"
        }

    suspend fun saveInitialScreen(value: String) {
        context.dataStore.edit { preferences ->
            preferences[INITIAL_SCREEN] = value
        }
    }

}
