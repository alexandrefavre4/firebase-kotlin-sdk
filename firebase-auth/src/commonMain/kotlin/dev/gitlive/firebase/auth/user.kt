/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

public interface FirebaseUser {
    public val uid: String
    public val displayName: String?
    public val email: String?
    public val phoneNumber: String?
    public val photoURL: String?
    public val isAnonymous: Boolean
    public val isEmailVerified: Boolean
    public val metaData: UserMetaData?
    public val multiFactor: MultiFactor
    public val providerData: List<UserInfo>
    public val providerId: String
    public suspend fun delete()
    public suspend fun reload()
    public suspend fun getIdToken(forceRefresh: Boolean): String?
    public suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult
    public suspend fun linkWithCredential(credential: AuthCredential): AuthResult
    public suspend fun reauthenticate(credential: AuthCredential)
    public suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult
    public suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings? = null)
    public suspend fun unlink(provider: String): FirebaseUser?
    public suspend fun updatePassword(password: String)
    public suspend fun updatePhoneNumber(credential: PhoneAuthCredential)
    public suspend fun updateProfile(displayName: String? = this.displayName, photoUrl: String? = this.photoURL)
    public suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings? = null)
}

internal expect class FirebaseUserImpl : FirebaseUser {
    override val uid: String
    override val displayName: String?
    override val email: String?
    override val phoneNumber: String?
    override val photoURL: String?
    override val isAnonymous: Boolean
    override val isEmailVerified: Boolean
    override val metaData: UserMetaData?
    override val multiFactor: MultiFactor
    override val providerData: List<UserInfo>
    override val providerId: String
    override suspend fun delete()
    override suspend fun reload()
    override suspend fun getIdToken(forceRefresh: Boolean): String?
    override suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult
    override suspend fun linkWithCredential(credential: AuthCredential): AuthResult
    override suspend fun reauthenticate(credential: AuthCredential)
    override suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult
    override suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?)
    override suspend fun unlink(provider: String): FirebaseUser?
    override suspend fun updatePassword(password: String)
    override suspend fun updatePhoneNumber(credential: PhoneAuthCredential)
    override suspend fun updateProfile(displayName: String?, photoUrl: String?)
    override suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?)
}

public interface UserInfo {
    public val displayName: String?
    public val email: String?
    public val phoneNumber: String?
    public val photoURL: String?
    public val providerId: String
    public val uid: String
}

internal expect class UserInfoImpl : UserInfo {
    override val displayName: String?
    override val email: String?
    override val phoneNumber: String?
    override val photoURL: String?
    override val providerId: String
    override val uid: String
}

public interface UserMetaData {
    public val creationTime: Double?
    public val lastSignInTime: Double?
}

internal expect class UserMetaDataImpl : UserMetaData {
    override val creationTime: Double?
    override val lastSignInTime: Double?
}
