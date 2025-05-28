/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import android.net.Uri
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

public val FirebaseUser.android: com.google.firebase.auth.FirebaseUser get() = android

internal actual class FirebaseUserImpl internal constructor(internal val android: com.google.firebase.auth.FirebaseUser) : FirebaseUser {
    actual override val uid: String
        get() = android.uid
    actual override val displayName: String?
        get() = android.displayName
    actual override val email: String?
        get() = android.email
    actual override val phoneNumber: String?
        get() = android.phoneNumber
    actual override val photoURL: String?
        get() = android.photoUrl?.toString()
    actual override val isAnonymous: Boolean
        get() = android.isAnonymous
    actual override val isEmailVerified: Boolean
        get() = android.isEmailVerified
    actual override val metaData: UserMetaData?
        get() = android.metadata?.let { UserMetaDataImpl(it) }
    actual override val multiFactor: MultiFactor
        get() = MultiFactor(android.multiFactor)
    actual override val providerData: List<UserInfo>
        get() = android.providerData.map { UserInfoImpl(it) }
    actual override val providerId: String
        get() = android.providerId
    actual override suspend fun delete() {
        android.delete().await()
    }
    actual override suspend fun reload() {
        android.reload().await()
    }
    actual override suspend fun getIdToken(forceRefresh: Boolean): String? = android.getIdToken(forceRefresh).await().token
    actual override suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = android.getIdToken(forceRefresh).await().run { AuthTokenResultImpl(this) }
    actual override suspend fun linkWithCredential(credential: AuthCredential): AuthResult = AuthResultImpl(android.linkWithCredential(credential.android).await())
    actual override suspend fun reauthenticate(credential: AuthCredential) {
        android.reauthenticate(credential.android).await()
    }
    actual override suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = AuthResultImpl(android.reauthenticateAndRetrieveData(credential.android).await())
    actual override suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?) {
        val request = actionCodeSettings?.let { android.sendEmailVerification(it.toAndroid()) } ?: android.sendEmailVerification()
        request.await()
    }
    actual override suspend fun unlink(provider: String): FirebaseUser? = android.unlink(provider).await().user?.let { FirebaseUserImpl(it) }

    actual override suspend fun updatePassword(password: String) {
        android.updatePassword(password).await()
    }
    actual override suspend fun updatePhoneNumber(credential: PhoneAuthCredential) {
        android.updatePhoneNumber(credential.android).await()
    }
    actual override suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = UserProfileChangeRequest.Builder()
            .apply { setDisplayName(displayName) }
            .apply { photoUri = photoUrl?.let { Uri.parse(it) } }
            .build()
        android.updateProfile(request).await()
    }
    actual override suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?) {
        android.verifyBeforeUpdateEmail(newEmail, actionCodeSettings?.toAndroid()).await()
    }
}

public val UserInfo.android: com.google.firebase.auth.UserInfo get() = android

internal actual class UserInfoImpl(internal val android: com.google.firebase.auth.UserInfo) : UserInfo {
    actual override val displayName: String?
        get() = android.displayName
    actual override val email: String?
        get() = android.email
    actual override val phoneNumber: String?
        get() = android.phoneNumber
    actual override val photoURL: String?
        get() = android.photoUrl?.toString()
    actual override val providerId: String
        get() = android.providerId
    actual override val uid: String
        get() = android.uid
}

public val UserMetaData.android: com.google.firebase.auth.FirebaseUserMetadata get() = android

internal actual class UserMetaDataImpl(internal val android: com.google.firebase.auth.FirebaseUserMetadata) : UserMetaData {
    actual override val creationTime: Double?
        get() = android.creationTimestamp.toDouble()
    actual override val lastSignInTime: Double?
        get() = android.lastSignInTimestamp.toDouble()
}
