package com.lamti.capturetheflag.data.authentication

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(private val auth: FirebaseAuth, ) {
    val currentUser = auth.currentUser

    suspend fun registerUser(email: String, password: String): String? {
        val authResult: AuthResult? = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            Timber.e("Authentication error: ${e.message}")
            null
        }
        return authResult?.user?.uid ?: return null
    }

    suspend fun loginUser(email: String, password: String): Boolean = try {
        auth.signInWithEmailAndPassword(email, password).await()
        true
    } catch (e: Exception) {
        false
    }

    fun logout() = auth.signOut()

}
