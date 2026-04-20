package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient
) : AuthRepository {

    private fun configured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun UserInfo.toDomainUser(): User {
        val meta = userMetadata as? JsonObject
        fun metaString(key: String): String? =
            (meta?.get(key) as? JsonPrimitive)?.content
        val fullName = metaString("full_name").orEmpty().ifBlank { email?.substringBefore("@") ?: "Traveler" }
        val phone = metaString("phone").orEmpty()
        return User(
            id = id,
            name = fullName,
            email = email.orEmpty(),
            phone = phone
        )
    }

    override suspend fun login(email: String, password: String): Result<User> {
        if (!configured()) return Result.failure(Exception("Add supabase.url and supabase.anon.key to local.properties."))
        return runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            awaitSession()
            client.auth.currentUserOrNull()?.toDomainUser()
                ?: error("No session after sign-in")
        }
    }

    override suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User> {
        if (!configured()) return Result.failure(Exception("Add supabase.url and supabase.anon.key to local.properties."))
        return runCatching {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("full_name", name)
                    put("phone", phone)
                }
            }
            awaitSession()
            client.auth.currentUserOrNull()?.toDomainUser()
                ?: error(
                    "Account created. If email confirmation is enabled in Supabase, open the link in your email, " +
                        "or disable confirmation (Auth → Providers → Email) for development."
                )
        }
    }

    override suspend fun forgotPassword(email: String): Result<Boolean> {
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return runCatching {
            client.auth.resetPasswordForEmail(email.trim())
            true
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Boolean> {
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return Result.failure(Exception("Use the magic link from your email, or sign in with password."))
    }

    override suspend fun resetPassword(email: String, newPassword: String): Result<Boolean> {
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return runCatching {
            client.auth.updateUser { password = newPassword }
            true
        }
    }

    override suspend fun logout() {
        runCatching { client.auth.signOut() }
    }

    override fun isLoggedIn(): Boolean =
        configured() && client.auth.currentSessionOrNull() != null

    override fun getCurrentUser(): User? =
        client.auth.currentUserOrNull()?.toDomainUser()

    private suspend fun awaitSession() {
        withTimeout(15_000) {
            while (client.auth.currentSessionOrNull() == null) {
                delay(50)
            }
        }
    }
}
