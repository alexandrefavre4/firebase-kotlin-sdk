/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIRUser
import cocoapods.FirebaseAuth.FIRUserInfoProtocol
import cocoapods.FirebaseAuth.FIRUserMetadata
import platform.Foundation.NSURL

public val FirebaseUser.ios: FIRUser get() = ios

internal actual class FirebaseUserImpl internal constructor(internal val ios: FIRUser): FirebaseUser {
    actual override val uid: String
        get() = ios.uid()
    actual override val displayName: String?
        get() = ios.displayName()
    actual override val email: String?
        get() = ios.email()
    actual override val phoneNumber: String?
        get() = ios.phoneNumber()
    actual override val photoURL: String?
        get() = ios.photoURL()?.absoluteString
    actual override val isAnonymous: Boolean
        get() = ios.anonymous()
    actual override val isEmailVerified: Boolean
        get() = ios.emailVerified()
    actual override val metaData: UserMetaData?
        get() = UserMetaDataImpl(ios.metadata())
    actual override val multiFactor: MultiFactor
        get() = MultiFactor(ios.multiFactor())
    actual override val providerData: List<UserInfo>
        get() = ios.providerData().mapNotNull { provider -> (provider as? FIRUserInfoProtocol)?.let { UserInfoImpl(it) } }
    actual override val providerId: String
        get() = ios.providerID()

    actual override suspend fun delete(): Unit = ios.await { deleteWithCompletion(it) }

    actual override suspend fun reload(): Unit = ios.await { reloadWithCompletion(it) }

    actual override suspend fun getIdToken(forceRefresh: Boolean): String? =
        ios.awaitResult { getIDTokenForcingRefresh(forceRefresh, it) }

    actual override suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult =
        AuthTokenResultImpl(ios.awaitResult { getIDTokenResultForcingRefresh(forceRefresh, it) })

    actual override suspend fun linkWithCredential(credential: AuthCredential): AuthResult =
        AuthResultImpl(ios.awaitResult { linkWithCredential(credential.ios, it) })

    actual override suspend fun reauthenticate(credential: AuthCredential) {
        ios.awaitResult<FIRUser, FIRAuthDataResult?> { reauthenticateWithCredential(credential.ios, it) }
    }

    actual override suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult =
        AuthResultImpl(ios.awaitResult { reauthenticateWithCredential(credential.ios, it) })

    actual override suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Unit = ios.await {
        actionCodeSettings?.let { settings -> sendEmailVerificationWithActionCodeSettings(settings.toIos(), it) }
            ?: sendEmailVerificationWithCompletion(it)
    }

    actual override suspend fun unlink(provider: String): FirebaseUser? {
        val user: FIRUser? = ios.awaitResult { unlinkFromProvider(provider, it) }
        return user?.let {
            FirebaseUserImpl(it)
        }
    }
    actual override suspend fun updatePassword(password: String): Unit = ios.await { updatePassword(password, it) }
    actual override suspend fun updatePhoneNumber(credential: PhoneAuthCredential): Unit = ios.await { updatePhoneNumberCredential(credential.ios, it) }
    actual override suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = ios.profileChangeRequest()
            .apply { setDisplayName(displayName) }
            .apply { setPhotoURL(photoUrl?.let { NSURL.URLWithString(it) }) }
        ios.await { request.commitChangesWithCompletion(it) }
    }
    actual override suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Unit = ios.await {
        actionCodeSettings?.let { actionSettings -> sendEmailVerificationBeforeUpdatingEmail(newEmail, actionSettings.toIos(), it) } ?: sendEmailVerificationBeforeUpdatingEmail(newEmail, it)
    }
}

public val UserInfo.ios: FIRUserInfoProtocol get() = ios

internal actual class UserInfoImpl(internal val ios: FIRUserInfoProtocol): UserInfo {
    actual override val displayName: String?
        get() = ios.displayName()
    actual override val email: String?
        get() = ios.email()
    actual override val phoneNumber: String?
        get() = ios.phoneNumber()
    actual override val photoURL: String?
        get() = ios.photoURL()?.absoluteString
    actual override val providerId: String
        get() = ios.providerID()
    actual override val uid: String
        get() = ios.uid()
}

public val UserMetaData.ios: FIRUserMetadata get() = ios

internal actual class UserMetaDataImpl(internal val ios: FIRUserMetadata): UserMetaData {
    actual override val creationTime: Double?
        get() = ios.creationDate()?.timeIntervalSinceReferenceDate
    actual override val lastSignInTime: Double?
        get() = ios.lastSignInDate()?.timeIntervalSinceReferenceDate
}
