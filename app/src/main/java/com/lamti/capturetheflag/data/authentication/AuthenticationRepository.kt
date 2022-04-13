package com.lamti.capturetheflag.data.authentication

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val ioDispatcher: CoroutineDispatcher
) {

    fun getCurrentUser() = auth.currentUser

    suspend fun registerUser(email: String, password: String): String? = withContext(ioDispatcher) {
        val authResult: AuthResult? = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            Timber.e("Authentication error: ${e.message}")
            null
        }
        return@withContext authResult?.user?.uid ?: return@withContext null
    }

    suspend fun loginUser(email: String, password: String): Boolean = withContext(ioDispatcher) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() = auth.signOut()

}
