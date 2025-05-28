/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.DecodeSettings
import dev.gitlive.firebase.EncodeSettings
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.firestore.internal.NativeCollectionReferenceWrapper
import dev.gitlive.firebase.firestore.internal.NativeDocumentReference
import dev.gitlive.firebase.firestore.internal.NativeDocumentSnapshotWrapper
import dev.gitlive.firebase.firestore.internal.NativeFirebaseFirestoreWrapper
import dev.gitlive.firebase.firestore.internal.NativeQueryWrapper
import dev.gitlive.firebase.firestore.internal.NativeTransactionWrapper
import dev.gitlive.firebase.firestore.internal.NativeWriteBatchWrapper
import dev.gitlive.firebase.firestore.internal.SetOptions
import dev.gitlive.firebase.firestore.internal.safeValue
import dev.gitlive.firebase.internal.decode
import dev.gitlive.firebase.internal.encodeAsObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy

/** Returns the [FirebaseFirestore] instance of the default [FirebaseApp] connected to the default database. */
public expect val Firebase.firestore: FirebaseFirestore

/** Returns the [FirebaseFirestore] instance of a given [FirebaseApp] connected to the default database. */
public expect fun Firebase.firestore(app: FirebaseApp): FirebaseFirestore

/** Returns the [FirebaseFirestore] instance of the default [FirebaseApp] connected to a specific database. */
public expect fun Firebase.firestore(database: String): FirebaseFirestore

/** Returns the [FirebaseFirestore] instance of a given [FirebaseApp] connected to a specific database. */
public expect fun Firebase.firestore(app: FirebaseApp, database: String): FirebaseFirestore

internal expect class NativeFirebaseFirestore

public interface FirebaseFirestore {

    public var settings: FirebaseFirestoreSettings

    public fun collection(collectionPath: String): CollectionReference
    public fun collectionGroup(collectionId: String): Query
    public fun document(documentPath: String): DocumentReference
    public fun batch(): WriteBatch
    public fun setLoggingEnabled(loggingEnabled: Boolean)
    public fun useEmulator(host: String, port: Int)

    public suspend fun disableNetwork()
    public suspend fun enableNetwork()

    public companion object
}

internal val FirebaseFirestore.native: NativeFirebaseFirestore
    get() = (this as FirebaseFirestoreImpl).native

internal class FirebaseFirestoreImpl internal constructor(private val wrapper: NativeFirebaseFirestoreWrapper) : FirebaseFirestore {

    internal constructor(native: NativeFirebaseFirestore) : this(NativeFirebaseFirestoreWrapper(native))

    override var settings: FirebaseFirestoreSettings
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            wrapper.applySettings(value)
        }

    override fun collection(collectionPath: String): CollectionReference = CollectionReferenceImpl(wrapper.collection(collectionPath))
    override fun collectionGroup(collectionId: String): Query = QueryImpl(wrapper.collectionGroup(collectionId))
    override fun document(documentPath: String): DocumentReference = DocumentReferenceImpl(wrapper.document(documentPath))
    override fun batch(): WriteBatch = WriteBatchImpl(wrapper.batch())
    override fun setLoggingEnabled(loggingEnabled: Boolean) {
        wrapper.setLoggingEnabled(loggingEnabled)
    }
    suspend fun clearPersistence() {
        wrapper.clearPersistence()
    }
    suspend fun <T> runTransaction(func: suspend Transaction.() -> T): T = wrapper.runTransaction { func(TransactionImpl(this)) }
    override fun useEmulator(host: String, port: Int) {
        wrapper.useEmulator(host, port)
    }

    override suspend fun disableNetwork() {
        wrapper.disableNetwork()
    }
    override suspend fun enableNetwork() {
        wrapper.enableNetwork()
    }
}

public expect class FirebaseFirestoreSettings {

    public companion object {
        public val CACHE_SIZE_UNLIMITED: Long
        internal val DEFAULT_HOST: String
        internal val MINIMUM_CACHE_BYTES: Long
        internal val DEFAULT_CACHE_SIZE_BYTES: Long
    }

    public class Builder {
        public constructor()
        public constructor(settings: FirebaseFirestoreSettings)

        public var sslEnabled: Boolean
        public var host: String
        public var cacheSettings: LocalCacheSettings

        public fun build(): FirebaseFirestoreSettings
    }

    public val sslEnabled: Boolean
    public val host: String
    public val cacheSettings: LocalCacheSettings
}

public expect fun firestoreSettings(settings: FirebaseFirestoreSettings? = null, builder: FirebaseFirestoreSettings.Builder.() -> Unit): FirebaseFirestoreSettings

internal expect class NativeTransaction

public interface Transaction {

    public fun set(
        documentRef: DocumentReference,
        data: Any,
        merge: Boolean = false,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun set(
        documentRef: DocumentReference,
        data: Any,
        vararg mergeFields: String,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun set(
        documentRef: DocumentReference,
        data: Any,
        vararg mergeFieldPaths: FieldPath,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun <T : Any> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        merge: Boolean = false,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun <T : Any> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        vararg mergeFields: String,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun <T : Any> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        vararg mergeFieldPaths: FieldPath,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun update(
        documentRef: DocumentReference,
        data: Any,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun <T : Any> update(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun updateFields(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<String, Any?>,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun updateFieldPaths(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<FieldPath, Any?>,
        buildSettings: EncodeSettings.Builder.() -> Unit = {},
    ): Transaction

    public fun delete(documentRef: DocumentReference): Transaction
    public suspend fun get(documentRef: DocumentReference): DocumentSnapshot

    public companion object
}

internal val Transaction.native: NativeTransaction
    get() = (this as TransactionImpl).nativeWrapper.native

internal data class TransactionImpl internal constructor(internal val nativeWrapper: NativeTransactionWrapper) : Transaction {

    internal constructor(native: NativeTransaction) : this(NativeTransactionWrapper(native))

    override fun set(documentRef: DocumentReference, data: Any, merge: Boolean, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    override fun set(documentRef: DocumentReference, data: Any, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    override fun set(documentRef: DocumentReference, data: Any, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, merge: Boolean, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @PublishedApi
    internal fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions): Transaction = TransactionImpl(nativeWrapper.setEncoded(documentRef, encodedData, setOptions))

    override fun update(documentRef: DocumentReference, data: Any, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = updateEncoded(documentRef, encodeAsObject(data, buildSettings))

    override fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = updateEncoded(documentRef, encodeAsObject(strategy, data, buildSettings))

    override fun updateFields(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = updateEncodedFieldsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    override fun updateFieldPaths(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit): Transaction = updateEncodedFieldPathsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    @PublishedApi
    internal fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): Transaction = TransactionImpl(nativeWrapper.updateEncoded(documentRef, encodedData))

    @PublishedApi
    internal fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>): Transaction = TransactionImpl(nativeWrapper.updateEncodedFieldsAndValues(documentRef, encodedFieldsAndValues))

    @PublishedApi
    internal fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>): Transaction = TransactionImpl(nativeWrapper.updateEncodedFieldPathsAndValues(documentRef, encodedFieldsAndValues))

    override fun delete(documentRef: DocumentReference): Transaction = TransactionImpl(nativeWrapper.delete(documentRef))
    override suspend fun get(documentRef: DocumentReference): DocumentSnapshot = DocumentSnapshotImpl(nativeWrapper.get(documentRef))
}

internal expect open class NativeQuery

public interface Query {

    public fun limit(limit: Number): Query
    public val snapshots: Flow<QuerySnapshot>
    public fun snapshots(includeMetadataChanges: Boolean = false): Flow<QuerySnapshot>
    public suspend fun get(source: Source = Source.DEFAULT): QuerySnapshot

    public fun orderBy(field: String, direction: Direction = Direction.ASCENDING): Query
    public fun orderBy(field: FieldPath, direction: Direction = Direction.ASCENDING): Query

    public fun endBefore(document: DocumentSnapshot): Query
    public fun endBefore(vararg fieldValues: Any): Query
    public fun endAt(document: DocumentSnapshot): Query
    public fun endAt(vararg fieldValues: Any): Query

    public fun where(builder: FilterBuilder.() -> Filter?): Query

    public companion object
}

internal val Query.native: NativeQuery
    get() = (this as QueryImpl).nativeQuery.native

internal open class QueryImpl internal constructor(internal val nativeQuery: NativeQueryWrapper) : Query {

    internal constructor(native: NativeQuery) : this(NativeQueryWrapper(native))

    override fun limit(limit: Number): Query = QueryImpl(nativeQuery.limit(limit))
    override val snapshots: Flow<QuerySnapshot> = nativeQuery.snapshots
    override fun snapshots(includeMetadataChanges: Boolean): Flow<QuerySnapshot> = nativeQuery.snapshots(includeMetadataChanges)
    override suspend fun get(source: Source): QuerySnapshot = nativeQuery.get(source)

    override fun where(builder: FilterBuilder.() -> Filter?): Query = builder(FilterBuilder())?.let { QueryImpl(nativeQuery.where(it)) } ?: this

    override fun orderBy(field: String, direction: Direction): Query = QueryImpl(nativeQuery.orderBy(field, direction))
    override fun orderBy(field: FieldPath, direction: Direction): Query = QueryImpl(nativeQuery.orderBy(field.encoded, direction))

    fun startAfter(document: DocumentSnapshot): Query = QueryImpl(nativeQuery.startAfter(document.native))
    fun startAfter(vararg fieldValues: Any): Query = QueryImpl(nativeQuery.startAfter(*(fieldValues.map { it.safeValue }.toTypedArray())))
    fun startAt(document: DocumentSnapshot): Query = QueryImpl(nativeQuery.startAt(document.native))
    fun startAt(vararg fieldValues: Any): Query = QueryImpl(nativeQuery.startAt(*(fieldValues.map { it.safeValue }.toTypedArray())))

    override fun endBefore(document: DocumentSnapshot): Query = QueryImpl(nativeQuery.endBefore(document.native))
    override fun endBefore(vararg fieldValues: Any): Query = QueryImpl(nativeQuery.endBefore(*(fieldValues.map { it.safeValue }.toTypedArray())))
    override fun endAt(document: DocumentSnapshot): Query = QueryImpl(nativeQuery.endAt(document.native))
    override fun endAt(vararg fieldValues: Any): Query = QueryImpl(nativeQuery.endAt(*(fieldValues.map { it.safeValue }.toTypedArray())))
}

internal expect class NativeWriteBatch

public interface WriteBatch {

    public fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun updateField(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun updateFieldPath(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}): WriteBatch

    public fun delete(documentRef: DocumentReference): WriteBatch

    public suspend fun commit()

    public companion object
}

internal val WriteBatch.native: NativeWriteBatch
    get() = (this as WriteBatchImpl).nativeWrapper.native

internal data class WriteBatchImpl internal constructor(internal val nativeWrapper: NativeWriteBatchWrapper) : WriteBatch {

    internal constructor(native: NativeWriteBatch) : this(NativeWriteBatchWrapper(native))

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, merge: Boolean, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    override fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @PublishedApi
    internal fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions): WriteBatch = WriteBatchImpl(nativeWrapper.setEncoded(documentRef, encodedData, setOptions))

    override fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch =
        updateEncoded(documentRef, encodeAsObject(strategy, data, buildSettings))

    override fun updateField(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch = updateEncodedFieldsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    override fun updateFieldPath(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit): WriteBatch = updateEncodedFieldPathsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    @PublishedApi
    internal fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): WriteBatch = WriteBatchImpl(nativeWrapper.updateEncoded(documentRef, encodedData))

    @PublishedApi
    internal fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>): WriteBatch = WriteBatchImpl(nativeWrapper.updateEncodedFieldsAndValues(documentRef, encodedFieldsAndValues))

    @PublishedApi
    internal fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>): WriteBatch = WriteBatchImpl(nativeWrapper.updateEncodedFieldPathsAndValues(documentRef, encodedFieldsAndValues))

    override fun delete(documentRef: DocumentReference): WriteBatch = WriteBatchImpl(nativeWrapper.delete(documentRef))

    override suspend fun commit() {
        nativeWrapper.commit()
    }
}

/** A class representing a platform specific Firebase DocumentReference. */
internal expect class NativeDocumentReferenceType

public interface DocumentReference {

    public val id: String
    public val path: String
    public val snapshots: Flow<DocumentSnapshot>
    public val parent: CollectionReference
    public fun snapshots(includeMetadataChanges: Boolean = false): Flow<DocumentSnapshot>

    public fun collection(collectionPath: String): CollectionReference
    public suspend fun get(source: Source = Source.DEFAULT): DocumentSnapshot

    public suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun <T : Any> update(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun updateFields(vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun updateFieldPaths(vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {})

    public suspend fun delete()

    public companion object
}

internal val DocumentReference.native: NativeDocumentReference
    get() = (this as DocumentReferenceImpl).native

/** A class representing a Firebase DocumentReference. */
@Serializable(with = DocumentReferenceSerializer::class)
internal data class DocumentReferenceImpl internal constructor(internal val native: NativeDocumentReference) : DocumentReference {

    internal constructor(native: NativeDocumentReferenceType) : this(NativeDocumentReference(native))

    override val id: String get() = native.id
    override val path: String get() = native.path
    override val snapshots: Flow<DocumentSnapshot> get() = native.snapshots.map(::DocumentSnapshotImpl)
    override val parent: CollectionReference get() = CollectionReferenceImpl(native.parent)
    override fun snapshots(includeMetadataChanges: Boolean): Flow<DocumentSnapshot> = native.snapshots(includeMetadataChanges).map(::DocumentSnapshotImpl)

    override fun collection(collectionPath: String): CollectionReference = CollectionReferenceImpl(native.collection(collectionPath))
    override suspend fun get(source: Source): DocumentSnapshot = DocumentSnapshotImpl(native.get(source))

    override suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, merge: Boolean, buildSettings: EncodeSettings.Builder.() -> Unit) {
        setEncoded(
            encodeAsObject(strategy, data, buildSettings),
            if (merge) SetOptions.Merge else SetOptions.Overwrite,
        )
    }

    override suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit) {
        setEncoded(
            encodeAsObject(strategy, data, buildSettings),
            SetOptions.MergeFields(mergeFields.asList()),
        )
    }

    override suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit) {
        setEncoded(
            encodeAsObject(strategy, data, buildSettings),
            SetOptions.MergeFieldPaths(mergeFieldPaths.asList()),
        )
    }

    @PublishedApi
    internal suspend fun setEncoded(encodedData: EncodedObject, setOptions: SetOptions) {
        native.setEncoded(encodedData, setOptions)
    }

    override suspend fun <T : Any> update(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit) {
        updateEncoded(
            encodeAsObject(strategy, data, buildSettings),
        )
    }

    @PublishedApi
    internal suspend fun updateEncoded(encodedData: EncodedObject) {
        native.updateEncoded(encodedData)
    }

    override suspend fun updateFields(vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit) {
        updateEncodedFieldsAndValues(
            encodeFieldAndValue(
                fieldsAndValues,
                buildSettings,
            ).orEmpty(),
        )
    }

    @PublishedApi
    internal suspend fun updateEncodedFieldsAndValues(encodedFieldsAndValues: List<Pair<String, Any?>>) {
        native.updateEncodedFieldsAndValues(encodedFieldsAndValues)
    }

    override suspend fun updateFieldPaths(vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit) {
        updateEncodedFieldPathsAndValues(
            encodeFieldAndValue(
                fieldsAndValues,
                buildSettings,
            ).orEmpty(),
        )
    }

    @PublishedApi
    internal suspend fun updateEncodedFieldPathsAndValues(encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>) {
        native.updateEncodedFieldPathsAndValues(encodedFieldsAndValues)
    }

    override suspend fun delete() {
        native.delete()
    }
}

internal expect class NativeCollectionReference : NativeQuery

public interface CollectionReference : Query {

    public val path: String
    public val document: DocumentReference
    public val parent: DocumentReference?

    public fun document(documentPath: String): DocumentReference

    public suspend fun <T : Any> add(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}): DocumentReference

    public companion object
}

internal val CollectionReference.native: NativeCollectionReference
    get() = (this as CollectionReferenceImpl).nativeWrapper.native

internal data class CollectionReferenceImpl internal constructor(internal val nativeWrapper: NativeCollectionReferenceWrapper) :
    QueryImpl(nativeWrapper),
    CollectionReference {

    internal constructor(native: NativeCollectionReference) : this(NativeCollectionReferenceWrapper(native))

    override val path: String get() = nativeWrapper.path
    override val document: DocumentReference get() = DocumentReferenceImpl(nativeWrapper.document)
    override val parent: DocumentReference? get() = nativeWrapper.parent?.let(::DocumentReferenceImpl)

    override fun document(documentPath: String): DocumentReference = DocumentReferenceImpl(nativeWrapper.document(documentPath))

    override suspend fun <T : Any> add(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit): DocumentReference = addEncoded(
        encodeAsObject(strategy, data, buildSettings),
    )

    @PublishedApi
    internal suspend fun addEncoded(data: EncodedObject): DocumentReference = DocumentReferenceImpl(nativeWrapper.addEncoded(data))
}

public expect class FirebaseFirestoreException : FirebaseException

public expect val FirebaseFirestoreException.code: FirestoreExceptionCode

public expect enum class FirestoreExceptionCode {
    OK,
    CANCELLED,
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    FAILED_PRECONDITION,
    ABORTED,
    OUT_OF_RANGE,
    UNIMPLEMENTED,
    INTERNAL,
    UNAVAILABLE,
    DATA_LOSS,
    UNAUTHENTICATED,
}

public expect enum class Direction {
    ASCENDING,
    DESCENDING,
}

public expect class QuerySnapshot {
    public val documents: List<DocumentSnapshot>
    public val documentChanges: List<DocumentChange>
    public val metadata: SnapshotMetadata
}

public expect enum class ChangeType {
    ADDED,
    MODIFIED,
    REMOVED,
}

public expect class DocumentChange {
    public val document: DocumentSnapshot
    public val newIndex: Int
    public val oldIndex: Int
    public val type: ChangeType
}

internal expect class NativeDocumentSnapshot

public interface DocumentSnapshot {

    public val exists: Boolean
    public val id: String
    public val reference: DocumentReference
    public val metadata: SnapshotMetadata

    public fun contains(field: String): Boolean
    public fun contains(fieldPath: FieldPath): Boolean

    public fun <T> get(field: String, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T

    public fun <T> get(fieldPath: FieldPath, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T

    public fun <T> data(strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T

    public companion object
}

internal val DocumentSnapshot.native: NativeDocumentSnapshot
    get() = (this as DocumentSnapshotImpl).nativeWrapper.native

internal data class DocumentSnapshotImpl internal constructor(internal val nativeWrapper: NativeDocumentSnapshotWrapper) : DocumentSnapshot {

    internal constructor(native: NativeDocumentSnapshot) : this(NativeDocumentSnapshotWrapper(native))

    override val exists: Boolean get() = nativeWrapper.exists
    override val id: String get() = nativeWrapper.id
    override val reference: DocumentReference get() = DocumentReferenceImpl(nativeWrapper.reference)
    override val metadata: SnapshotMetadata get() = nativeWrapper.metadata

    override fun contains(field: String): Boolean = nativeWrapper.contains(field)
    override fun contains(fieldPath: FieldPath): Boolean = nativeWrapper.contains(fieldPath.encoded)

    override fun <T> get(field: String, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior, buildSettings: DecodeSettings.Builder.() -> Unit): T = decode(strategy, getEncoded(field, serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun getEncoded(field: String, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.getEncoded(field, serverTimestampBehavior)

    override fun <T> get(fieldPath: FieldPath, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior, buildSettings: DecodeSettings.Builder.() -> Unit): T = decode(strategy, getEncoded(fieldPath, serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun getEncoded(fieldPath: FieldPath, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.getEncoded(fieldPath.encoded, serverTimestampBehavior)

    override fun <T> data(strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior, buildSettings: DecodeSettings.Builder.() -> Unit): T = decode(strategy, encodedData(serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun encodedData(serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.encodedData(serverTimestampBehavior)
}

public enum class ServerTimestampBehavior {
    ESTIMATE,
    NONE,
    PREVIOUS,
}

public expect class SnapshotMetadata {
    public val hasPendingWrites: Boolean
    public val isFromCache: Boolean
}

public expect class FieldPath(vararg fieldNames: String) {
    public companion object {
        public val documentId: FieldPath
    }

    @Deprecated("Use companion object instead", replaceWith = ReplaceWith("FieldPath.documentId"))
    public val documentId: FieldPath
    public val encoded: EncodedFieldPath
}

public expect class EncodedFieldPath

public enum class Source {
    CACHE,
    SERVER,
    DEFAULT,
}
