package com.lamti.capturetheflag.data.authentication

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(
    private val auth: FirebaseAuth,
) {

    val currentUser = auth.currentUser

    suspend fun registerUser(email: String, password: String): String {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        return authResult.user?.uid ?: return "no_id"
    }

    suspend fun loginUser(email: String, password: String): Boolean = try {
        auth.signInWithEmailAndPassword(email, password).await()
        true
    } catch (e: Exception) {
        false
    }

    fun logout() = auth.signOut()

}
