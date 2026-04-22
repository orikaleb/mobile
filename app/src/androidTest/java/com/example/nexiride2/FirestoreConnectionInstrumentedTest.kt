package com.example.nexiride2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the app can reach Cloud Firestore (network + [google-services.json] + rules allow read).
 * Run: `./gradlew :app:connectedDebugAndroidTest --tests "*.FirestoreConnectionInstrumentedTest"`
 * Requires an emulator or device with network access.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreConnectionInstrumentedTest {

    @Test
    fun firestore_canReadRoutesCollection() = runBlocking {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("routes")
            .limit(1)
            .get()
            .await()
        assertNotNull(snapshot)
    }
}
