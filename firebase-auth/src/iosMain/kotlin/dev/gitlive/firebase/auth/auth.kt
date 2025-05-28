/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.ActionCodeResult.*
import dev.gitlive.firebase.ios
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSURL

public val FirebaseAuth.ios: FIRAuth get() = FIRAuth.auth()

public actual val Firebase.auth: FirebaseAuth
    get() = FirebaseAuthImpl(FIRAuth.auth())

public actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth = FirebaseAuthImpl(
    FIRAuth.authWithApp(app.ios as objcnames.classes.FIRApp),
)

internal actual class FirebaseAuthImpl internal constructor(internal val ios: FIRAuth) : FirebaseAuth {

    actual override val currentUser: FirebaseUser?
        get() = ios.currentUser()?.let { FirebaseUserImpl(it) }

    actual override val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val handle = ios.addAuthStateDidChangeListener { _, user -> trySend(user?.let { FirebaseUserImpl(it) }) }
        awaitClose { ios.removeAuthStateDidChangeListener(handle) }
    }

    actual override val idTokenChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val handle = ios.addIDTokenDidChangeListener { _, user -> trySend(user?.let { FirebaseUserImpl(it) }) }
        awaitClose { ios.removeIDTokenDidChangeListener(handle) }
    }

    actual override var languageCode: String
        get() = ios.languageCode() ?: ""
        set(value) {
            ios.setLanguageCode(value)
        }

    actual override suspend fun applyActionCode(code: String): Unit = ios.await { applyActionCode(code, it) }
    actual override suspend fun confirmPasswordReset(code: String, newPassword: String): Unit = ios.await { confirmPasswordResetWithCode(code, newPassword, it) }

    actual override suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult =
        AuthResultImpl(ios.awaitResult { createUserWithEmail(email = email, password = password, completion = it) })

    actual override suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        ios.await { actionCodeSettings?.let { actionSettings -> sendPasswordResetWithEmail(email, actionSettings.toIos(), it) } ?: sendPasswordResetWithEmail(email = email, completion = it) }
    }

    actual override suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings): Unit = ios.await { sendSignInLinkToEmail(email, actionCodeSettings.toIos(), it) }

    actual override fun isSignInWithEmailLink(link: String): Boolean = ios.isSignInWithEmailLink(link)

    actual override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult =
        AuthResultImpl(ios.awaitResult { signInWithEmail(email = email, password = password, completion = it) })

    actual override suspend fun signInWithCustomToken(token: String): AuthResult =
        AuthResultImpl(ios.awaitResult { signInWithCustomToken(token, it) })

    actual override suspend fun signInAnonymously(): AuthResult =
        AuthResultImpl(ios.awaitResult { signInAnonymouslyWithCompletion(it) })

    actual override suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult =
        AuthResultImpl(ios.awaitResult { signInWithCredential(authCredential.ios, it) })

    actual override suspend fun signInWithEmailLink(email: String, link: String): AuthResult =
        AuthResultImpl(ios.awaitResult { signInWithEmail(email = email, link = link, completion = it) })

    actual override suspend fun signOut(): Unit = ios.throwError { signOut(it) }

    actual override suspend fun updateCurrentUser(user: FirebaseUser): Unit = ios.await { updateCurrentUser(user.ios, it) }
    actual override suspend fun verifyPasswordResetCode(code: String): String = ios.awaitResult { verifyPasswordResetCode(code, it) }

    actual override suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result: FIRActionCodeInfo = ios.awaitResult { checkActionCode(code, it) }
        @Suppress("UNCHECKED_CAST")
        return when (result.operation()) {
            FIRActionCodeOperationEmailLink -> SignInWithEmailLink
            FIRActionCodeOperationVerifyEmail -> VerifyEmail(result.email())
            FIRActionCodeOperationPasswordReset -> PasswordReset(result.email())
            FIRActionCodeOperationRecoverEmail -> RecoverEmail(result.email(), result.previousEmail()!!)
            FIRActionCodeOperationVerifyAndChangeEmail -> VerifyBeforeChangeEmail(result.email(), result.previousEmail()!!)
            FIRActionCodeOperationRevertSecondFactorAddition -> RevertSecondFactorAddition(result.email(), null)
            FIRActionCodeOperationUnknown -> throw UnsupportedOperationException(result.operation().toString())
            else -> throw UnsupportedOperationException(result.operation().toString())
        } as T
    }

    actual override fun useEmulator(host: String, port: Int): Unit = ios.useEmulatorWithHost(host, port.toLong())
}

public val AuthResult.ios: FIRAuthDataResult get() = ios

internal actual class AuthResultImpl(internal val ios: FIRAuthDataResult): AuthResult {
    actual override val user: FirebaseUser?
        get() = FirebaseUserImpl(ios.user())
    actual override val credential: AuthCredential?
        get() = ios.credential()?.let { AuthCredential(it) }
    actual override val additionalUserInfo: AdditionalUserInfo?
        get() = ios.additionalUserInfo()?.let { AdditionalUserInfoImpl(it) }
}

public val AdditionalUserInfo.ios: FIRAdditionalUserInfo get() = ios

internal actual class AdditionalUserInfoImpl(
    internal val ios: FIRAdditionalUserInfo,
): AdditionalUserInfo {
    actual override val providerId: String?
        get() = ios.providerID()
    actual override val username: String?
        get() = ios.username()
    actual override val profile: Map<String, Any?>?
        get() = ios.profile()
            ?.mapNotNull { (key, value) ->
                if (key is NSString && value != null) {
                    key.toString() to value
                } else {
                    null
                }
            }
            ?.toMap()
    actual override val isNewUser: Boolean
        get() = ios.newUser()
}

public val AuthTokenResult.ios: FIRAuthTokenResult get() = ios
internal actual class AuthTokenResultImpl(internal val ios: FIRAuthTokenResult): AuthTokenResult{
//    actual val authTimestamp: Long
//        get() = ios.authDate
    actual override val claims: Map<String, Any>
        get() = ios.claims().map { it.key.toString() to it.value as Any }.toMap()

//    actual val expirationTimestamp: Long
//        get() = ios.expirationDate
//    actual val issuedAtTimestamp: Long
//        get() = ios.issuedAtDate
    actual override val signInProvider: String?
        get() = ios.signInProvider()
    actual override val token: String?
        get() = ios.token()
}

internal fun ActionCodeSettings.toIos() = FIRActionCodeSettings().also {
    it.setURL(NSURL.URLWithString(url))
    androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) }
    it.setDynamicLinkDomain(dynamicLinkDomain)
    it.setHandleCodeInApp(canHandleCodeInApp)
    iOSBundleId?.run { it.setIOSBundleID(this) }
}

public actual open class FirebaseAuthException(message: String) : FirebaseException(message)
public actual open class FirebaseAuthActionCodeException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthEmailException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthInvalidCredentialsException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthWeakPasswordException(message: String) : FirebaseAuthInvalidCredentialsException(message)
public actual open class FirebaseAuthInvalidUserException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthMultiFactorException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthRecentLoginRequiredException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthUserCollisionException(message: String) : FirebaseAuthException(message)
public actual open class FirebaseAuthWebException(message: String) : FirebaseAuthException(message)

internal fun <T, R> T.throwError(block: T.(errorPointer: CPointer<ObjCObjectVar<NSError?>>) -> R): R {
    memScoped {
        val errorPointer: CPointer<ObjCObjectVar<NSError?>> = alloc<ObjCObjectVar<NSError?>>().ptr
        val result = block(errorPointer)
        val error: NSError? = errorPointer.pointed.value
        if (error != null) {
            throw error.toException()
        }
        return result
    }
}

internal suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if (error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as R
}

internal suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if (error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
}

private fun NSError.toException() = when (domain) {
    // codes from AuthErrors.swift: https://github.com/firebase/firebase-ios-sdk/blob/
    // 2f6ac4c2c61cd57c7ea727009e187b7e1163d613/FirebaseAuth/Sources/Swift/Utilities/
    // AuthErrors.swift#L51
    FIRAuthErrorDomain -> when (code) {
        17030L, // AuthErrorCode.invalidActionCode
        17029L, // AuthErrorCode.expiredActionCode
        -> FirebaseAuthActionCodeException(toString())

        17008L, // AuthErrorCode.invalidEmail
        -> FirebaseAuthEmailException(toString())

        17056L, // AuthErrorCode.captchaCheckFailed
        17042L, // AuthErrorCode.invalidPhoneNumber
        17041L, // AuthErrorCode.missingPhoneNumber
        17046L, // AuthErrorCode.invalidVerificationID
        17044L, // AuthErrorCode.invalidVerificationCode
        17045L, // AuthErrorCode.missingVerificationID
        17043L, // AuthErrorCode.missingVerificationCode
        17021L, // AuthErrorCode.userTokenExpired
        17004L, // AuthErrorCode.invalidCredential
        -> FirebaseAuthInvalidCredentialsException(toString())

        17026L, // AuthErrorCode.weakPassword
        -> FirebaseAuthWeakPasswordException(toString())

        17017L, // AuthErrorCode.invalidUserToken
        -> FirebaseAuthInvalidUserException(toString())

        17014L, // AuthErrorCode.requiresRecentLogin
        -> FirebaseAuthRecentLoginRequiredException(toString())

        17087L, // AuthErrorCode.secondFactorAlreadyEnrolled
        17078L, // AuthErrorCode.secondFactorRequired
        17088L, // AuthErrorCode.maximumSecondFactorCountExceeded
        17084L, // AuthErrorCode.multiFactorInfoNotFound
        -> FirebaseAuthMultiFactorException(toString())

        17007L, // AuthErrorCode.emailAlreadyInUse
        17012L, // AuthErrorCode.accountExistsWithDifferentCredential
        17025L, // AuthErrorCode.credentialAlreadyInUse
        -> FirebaseAuthUserCollisionException(toString())

        17057L, // AuthErrorCode.webContextAlreadyPresented
        17058L, // AuthErrorCode.webContextCancelled
        17062L, // AuthErrorCode.webInternalError
        -> FirebaseAuthWebException(toString())

        17020L, // AuthErrorCode.networkError
        -> FirebaseNetworkException(toString())

        else -> FirebaseAuthException(toString())
    }
    else -> FirebaseAuthException(toString())
}
