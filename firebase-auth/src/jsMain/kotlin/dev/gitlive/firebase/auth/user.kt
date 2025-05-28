package dev.gitlive.firebase.auth

import dev.gitlive.firebase.auth.externals.*
import kotlinx.coroutines.await
import kotlin.js.Date
import dev.gitlive.firebase.auth.externals.UserInfo as JsUserInfo
import kotlin.js.json

public val FirebaseUser.js: User get() = js

internal actual class FirebaseUserImpl internal constructor(internal val js: User) : FirebaseUser {
    actual override val uid: String
        get() = rethrow { js.uid }
    actual override val displayName: String?
        get() = rethrow { js.displayName }
    actual override val email: String?
        get() = rethrow { js.email }
    actual override val phoneNumber: String?
        get() = rethrow { js.phoneNumber }
    actual override val photoURL: String?
        get() = rethrow { js.photoURL }
    actual override val isAnonymous: Boolean
        get() = rethrow { js.isAnonymous }
    actual override val isEmailVerified: Boolean
        get() = rethrow { js.emailVerified }
    actual override val metaData: UserMetaData?
        get() = rethrow { UserMetaDataImpl(js.metadata) }
    actual override val multiFactor: MultiFactor
        get() = rethrow { MultiFactor(multiFactor(js)) }
    actual override val providerData: List<UserInfo>
        get() = rethrow { js.providerData.map { UserInfoImpl(it) } }
    actual override val providerId: String
        get() = rethrow { js.providerId }
    actual override suspend fun delete(): Unit = rethrow { js.delete().await() }
    actual override suspend fun reload(): Unit = rethrow { js.reload().await() }
    actual override suspend fun getIdToken(forceRefresh: Boolean): String? = rethrow { js.getIdToken(forceRefresh).await() }
    actual override suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = rethrow { AuthTokenResultImpl(getIdTokenResult(js, forceRefresh).await()) }
    actual override suspend fun linkWithCredential(credential: AuthCredential): AuthResult = rethrow { AuthResultImpl(linkWithCredential(js, credential.js).await()) }
    actual override suspend fun reauthenticate(credential: AuthCredential): Unit = rethrow {
        reauthenticateWithCredential(js, credential.js).await()
        Unit
    }
    actual override suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = rethrow { AuthResultImpl(reauthenticateWithCredential(js, credential.js).await()) }

    actual override suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Unit = rethrow { sendEmailVerification(js, actionCodeSettings?.toJson()).await() }
    actual override suspend fun unlink(provider: String): FirebaseUser? = rethrow { FirebaseUserImpl(unlink(js, provider).await()) }
    actual override suspend fun updatePassword(password: String): Unit = rethrow { updatePassword(js, password).await() }
    actual override suspend fun updatePhoneNumber(credential: PhoneAuthCredential): Unit = rethrow { updatePhoneNumber(js, credential.js).await() }
    actual override suspend fun updateProfile(displayName: String?, photoUrl: String?): Unit = rethrow {
        val request = listOf(
            "displayName" to displayName,
            "photoURL" to photoUrl,
        )
        updateProfile(js, json(*request.toTypedArray())).await()
    }
    actual override suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Unit = rethrow { verifyBeforeUpdateEmail(js, newEmail, actionCodeSettings?.toJson()).await() }
}

public val UserInfo.js: UserInfo get() = js

internal actual class UserInfoImpl(internal val js: JsUserInfo) : UserInfo {
    actual override val displayName: String?
        get() = rethrow { js.displayName }
    actual override val email: String?
        get() = rethrow { js.email }
    actual override val phoneNumber: String?
        get() = rethrow { js.phoneNumber }
    actual override val photoURL: String?
        get() = rethrow { js.photoURL }
    actual override val providerId: String
        get() = rethrow { js.providerId }
    actual override val uid: String
        get() = rethrow { js.uid }
}

public val UserMetaData.js: UserMetaData get() = js

internal actual class UserMetaDataImpl(internal val js: UserMetadata) : UserMetaData {
    actual override val creationTime: Double?
        get() = rethrow { js.creationTime?.let { (Date(it).getTime() / 1000.0) } }
    actual override val lastSignInTime: Double?
        get() = rethrow { js.lastSignInTime?.let { (Date(it).getTime() / 1000.0) } }
}
