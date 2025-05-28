/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("android")

package dev.gitlive.firebase.auth

import com.google.firebase.auth.ActionCodeEmailInfo
import com.google.firebase.auth.ActionCodeMultiFactorInfo
import com.google.firebase.auth.ActionCodeResult.*
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.android as publicAndroid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

public val FirebaseAuth.android: com.google.firebase.auth.FirebaseAuth get() = com.google.firebase.auth.FirebaseAuth.getInstance()

public actual val Firebase.auth: FirebaseAuth
    get() = FirebaseAuthImpl(com.google.firebase.auth.FirebaseAuth.getInstance())

public actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth =
    FirebaseAuthImpl(com.google.firebase.auth.FirebaseAuth.getInstance(app.publicAndroid))

internal actual class FirebaseAuthImpl internal constructor(internal val android: com.google.firebase.auth.FirebaseAuth) : FirebaseAuth {
    actual override val currentUser: FirebaseUser?
        get() = android.currentUser?.let { FirebaseUserImpl(it) }

    actual override val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val listener = AuthStateListener { auth -> trySend(auth.currentUser?.let { FirebaseUserImpl(it) }) }
        android.addAuthStateListener(listener)
        awaitClose { android.removeAuthStateListener(listener) }
    }

    actual override val idTokenChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val listener = com.google.firebase.auth.FirebaseAuth.IdTokenListener { auth -> trySend(auth.currentUser?.let { FirebaseUserImpl(it) }) }
        android.addIdTokenListener(listener)
        awaitClose { android.removeIdTokenListener(listener) }
    }

    actual override var languageCode: String
        get() = android.languageCode ?: ""
        set(value) {
            android.setLanguageCode(value)
        }

    actual override suspend fun applyActionCode(code: String) {
        android.applyActionCode(code).await()
    }
    actual override suspend fun confirmPasswordReset(code: String, newPassword: String) {
        android.confirmPasswordReset(code, newPassword).await()
    }

    actual override suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult =
        AuthResultImpl(android.createUserWithEmailAndPassword(email, password).await())

    actual override suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        android.sendPasswordResetEmail(email, actionCodeSettings?.toAndroid()).await()
    }

    actual override suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings) {
        android.sendSignInLinkToEmail(email, actionCodeSettings.toAndroid()).await()
    }

    actual override fun isSignInWithEmailLink(link: String): Boolean = android.isSignInWithEmailLink(link)

    actual override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult =
        AuthResultImpl(android.signInWithEmailAndPassword(email, password).await())

    actual override suspend fun signInWithCustomToken(token: String): AuthResult =
        AuthResultImpl(android.signInWithCustomToken(token).await())

    actual override suspend fun signInAnonymously(): AuthResult = AuthResultImpl(android.signInAnonymously().await())

    actual override suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult =
        AuthResultImpl(android.signInWithCredential(authCredential.android).await())

    actual override suspend fun signInWithEmailLink(email: String, link: String): AuthResult =
        AuthResultImpl(android.signInWithEmailLink(email, link).await())

    actual override suspend fun signOut(): Unit = android.signOut()

    actual override suspend fun updateCurrentUser(user: FirebaseUser) {
        android.updateCurrentUser(user.android).await()
    }
    actual override suspend fun verifyPasswordResetCode(code: String): String = android.verifyPasswordResetCode(code).await()

    actual override suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result = android.checkActionCode(code).await()
        @Suppress("UNCHECKED_CAST")
        return when (result.operation) {
            SIGN_IN_WITH_EMAIL_LINK -> ActionCodeResult.SignInWithEmailLink
            VERIFY_EMAIL -> ActionCodeResult.VerifyEmail(result.info!!.email)
            PASSWORD_RESET -> ActionCodeResult.PasswordReset(result.info!!.email)
            RECOVER_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.RecoverEmail(email, previousEmail)
            }
            VERIFY_BEFORE_CHANGE_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.VerifyBeforeChangeEmail(email, previousEmail)
            }
            REVERT_SECOND_FACTOR_ADDITION -> (result.info as ActionCodeMultiFactorInfo).run {
                ActionCodeResult.RevertSecondFactorAddition(email, MultiFactorInfo(multiFactorInfo))
            }
            ERROR -> throw UnsupportedOperationException(result.operation.toString())
            else -> throw UnsupportedOperationException(result.operation.toString())
        } as T
    }

    actual override fun useEmulator(host: String, port: Int): Unit = android.useEmulator(host, port)
}

public val AuthResult.android: com.google.firebase.auth.AuthResult get() = android

internal actual class AuthResultImpl(internal val android: com.google.firebase.auth.AuthResult) : AuthResult {
    actual override val user: FirebaseUser?
        get() = android.user?.let { FirebaseUserImpl(it) }
    actual override val credential: AuthCredential?
        get() = android.credential?.let { AuthCredential(it) }
    actual override val additionalUserInfo: AdditionalUserInfo?
        get() = android.additionalUserInfo?.let { AdditionalUserInfoImpl(it) }
}

public val AdditionalUserInfo.android: com.google.firebase.auth.AdditionalUserInfo
    get() = android

internal actual class AdditionalUserInfoImpl(
    internal val android: com.google.firebase.auth.AdditionalUserInfo,
) : AdditionalUserInfo {
    actual override val providerId: String?
        get() = android.providerId
    actual override val username: String?
        get() = android.username
    actual override val profile: Map<String, Any?>?
        get() = android.profile
    actual override val isNewUser: Boolean
        get() = android.isNewUser
}

public val AuthTokenResult.android: com.google.firebase.auth.GetTokenResult get() = android

internal actual class AuthTokenResultImpl(internal val android: com.google.firebase.auth.GetTokenResult) : AuthTokenResult {
//    actual val authTimestamp: Long
//        get() = android.authTimestamp
    actual override val claims: Map<String, Any>
        get() = android.claims

//    actual val expirationTimestamp: Long
//        get() = android.expirationTimestamp
//    actual val issuedAtTimestamp: Long
//        get() = android.issuedAtTimestamp
    actual override val signInProvider: String?
        get() = android.signInProvider
    actual override val token: String?
        get() = android.token
}

internal fun ActionCodeSettings.toAndroid() = com.google.firebase.auth.ActionCodeSettings.newBuilder()
    .setUrl(url)
    .also { androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) } }
    .also { dynamicLinkDomain?.run { it.setDynamicLinkDomain(this) } }
    .setHandleCodeInApp(canHandleCodeInApp)
    .also { iOSBundleId?.run { it.setIOSBundleId(this) } }
    .build()

public actual typealias FirebaseAuthException = com.google.firebase.auth.FirebaseAuthException
public actual typealias FirebaseAuthActionCodeException = com.google.firebase.auth.FirebaseAuthActionCodeException
public actual typealias FirebaseAuthEmailException = com.google.firebase.auth.FirebaseAuthEmailException
public actual typealias FirebaseAuthInvalidCredentialsException = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
public actual typealias FirebaseAuthWeakPasswordException = com.google.firebase.auth.FirebaseAuthWeakPasswordException
public actual typealias FirebaseAuthInvalidUserException = com.google.firebase.auth.FirebaseAuthInvalidUserException
public actual typealias FirebaseAuthMultiFactorException = com.google.firebase.auth.FirebaseAuthMultiFactorException
public actual typealias FirebaseAuthRecentLoginRequiredException = com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
public actual typealias FirebaseAuthUserCollisionException = com.google.firebase.auth.FirebaseAuthUserCollisionException
public actual typealias FirebaseAuthWebException = com.google.firebase.auth.FirebaseAuthWebException
