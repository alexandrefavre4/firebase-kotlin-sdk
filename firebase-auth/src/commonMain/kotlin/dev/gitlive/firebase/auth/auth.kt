/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.flow.Flow

public expect val Firebase.auth: FirebaseAuth

public expect fun Firebase.auth(app: FirebaseApp): FirebaseAuth

public interface FirebaseAuth {

    public val currentUser: FirebaseUser?
    public val authStateChanged: Flow<FirebaseUser?>
    public val idTokenChanged: Flow<FirebaseUser?>
    public var languageCode: String

    public suspend fun applyActionCode(code: String)
    public suspend fun <T : ActionCodeResult> checkActionCode(code: String): T
    public suspend fun confirmPasswordReset(code: String, newPassword: String)
    public suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult
    public suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings? = null)
    public suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings)
    public fun isSignInWithEmailLink(link: String): Boolean
    public suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult
    public suspend fun signInWithCustomToken(token: String): AuthResult
    public suspend fun signInAnonymously(): AuthResult
    public suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult
    public suspend fun signInWithEmailLink(email: String, link: String): AuthResult
    public suspend fun signOut()
    public suspend fun updateCurrentUser(user: FirebaseUser)
    public suspend fun verifyPasswordResetCode(code: String): String
    public fun useEmulator(host: String, port: Int)
}

internal expect class FirebaseAuthImpl : FirebaseAuth {
    override val currentUser: FirebaseUser?
    override val authStateChanged: Flow<FirebaseUser?>
    override val idTokenChanged: Flow<FirebaseUser?>
    override var languageCode: String
    override suspend fun applyActionCode(code: String)
    override suspend fun <T : ActionCodeResult> checkActionCode(code: String): T
    override suspend fun confirmPasswordReset(code: String, newPassword: String)
    override suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult
    override suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?)
    override suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings)
    override fun isSignInWithEmailLink(link: String): Boolean
    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult
    override suspend fun signInWithCustomToken(token: String): AuthResult
    override suspend fun signInAnonymously(): AuthResult
    override suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult
    override suspend fun signInWithEmailLink(email: String, link: String): AuthResult
    override suspend fun signOut()
    override suspend fun updateCurrentUser(user: FirebaseUser)
    override suspend fun verifyPasswordResetCode(code: String): String
    override fun useEmulator(host: String, port: Int)
}

public interface AuthResult {
    public val user: FirebaseUser?
    public val credential: AuthCredential?
    public val additionalUserInfo: AdditionalUserInfo?
}

internal expect class AuthResultImpl : AuthResult {
    override val user: FirebaseUser?
    override val credential: AuthCredential?
    override val additionalUserInfo: AdditionalUserInfo?
}

public interface AdditionalUserInfo {
    public val providerId: String?
    public val username: String?
    public val profile: Map<String, Any?>?
    public val isNewUser: Boolean
}

internal expect class AdditionalUserInfoImpl : AdditionalUserInfo {
    override val providerId: String?
    override val username: String?
    override val profile: Map<String, Any?>?
    override val isNewUser: Boolean
}

public interface AuthTokenResult {
    public val claims: Map<String, Any>
    public val signInProvider: String?
    public val token: String?
}

internal expect class AuthTokenResultImpl : AuthTokenResult {
//    val authTimestamp: Long
    override val claims: Map<String, Any>

//    val expirationTimestamp: Long
//    val issuedAtTimestamp: Long
    override val signInProvider: String?
    override val token: String?
}

public sealed class ActionCodeResult {
    public data object SignInWithEmailLink : ActionCodeResult()
    public class PasswordReset internal constructor(public val email: String) : ActionCodeResult()
    public class VerifyEmail internal constructor(public val email: String) : ActionCodeResult()
    public class RecoverEmail internal constructor(public val email: String, public val previousEmail: String) : ActionCodeResult()
    public class VerifyBeforeChangeEmail internal constructor(public val email: String, public val previousEmail: String) : ActionCodeResult()
    public class RevertSecondFactorAddition internal constructor(public val email: String, public val multiFactorInfo: MultiFactorInfo?) : ActionCodeResult()
}

public data class ActionCodeSettings(
    val url: String,
    val androidPackageName: AndroidPackageName? = null,
    val dynamicLinkDomain: String? = null,
    val canHandleCodeInApp: Boolean = false,
    val iOSBundleId: String? = null,
)

public data class AndroidPackageName(
    val packageName: String,
    val installIfNotAvailable: Boolean = true,
    val minimumVersion: String? = null,
)

public expect open class FirebaseAuthException : FirebaseException
public expect class FirebaseAuthActionCodeException : FirebaseAuthException
public expect class FirebaseAuthEmailException : FirebaseAuthException
public expect open class FirebaseAuthInvalidCredentialsException : FirebaseAuthException
public expect class FirebaseAuthWeakPasswordException : FirebaseAuthInvalidCredentialsException
public expect class FirebaseAuthInvalidUserException : FirebaseAuthException
public expect class FirebaseAuthMultiFactorException : FirebaseAuthException
public expect class FirebaseAuthRecentLoginRequiredException : FirebaseAuthException
public expect class FirebaseAuthUserCollisionException : FirebaseAuthException
public expect class FirebaseAuthWebException : FirebaseAuthException
