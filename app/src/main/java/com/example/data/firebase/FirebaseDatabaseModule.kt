package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Dependency module responsible for initializing the Cloud Firestore instance
 * bound strictly to the provisioned named database (`R.string.firestore_database_id`)
 * and providing CRUD helpers for Firebase messages and comments.
 */
object FirebaseDatabaseModule {

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    /**
     * Ensures [FirebaseApp] is initialized and returns the singleton [FirebaseFirestore]
     * bound to `R.string.firestore_database_id`.
     */
    fun initializeFirestore(context: Context): FirebaseFirestore {
        firestoreInstance?.let { return it }
        return synchronized(this) {
            firestoreInstance?.let { return@synchronized it }
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            val databaseId = appContext.getString(R.string.firestore_database_id)
            require(databaseId.isNotBlank()) {
                "R.string.firestore_database_id must not be blank."
            }
            FirebaseFirestore.getInstance(databaseId).also { instance ->
                firestoreInstance = instance
            }
        }
    }

    /**
     * Returns the initialized [FirebaseFirestore] instance for the custom database ID.
     */
    fun provideFirestore(context: Context): FirebaseFirestore = initializeFirestore(context)

    /**
     * Provides a [FirebaseFirestoreCrudHelper] configured with the initialized Firestore instance
     * for performing Create, Read, Update, and Delete operations on messages and comments.
     */
    fun provideCrudHelper(context: Context): FirebaseFirestoreCrudHelper =
        FirebaseFirestoreCrudHelper(db = initializeFirestore(context))

    /**
     * Provides a [FirebaseMessagingRepository] configured with the initialized Firestore instance.
     */
    fun provideMessagingRepository(context: Context): FirebaseMessagingRepository =
        FirebaseMessagingRepository(db = initializeFirestore(context))

    internal fun clearForTesting() {
        synchronized(this) {
            firestoreInstance = null
        }
    }
}
