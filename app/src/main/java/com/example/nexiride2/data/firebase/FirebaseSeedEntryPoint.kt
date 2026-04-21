package com.example.nexiride2.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirebaseSeedEntryPoint {
    fun firestore(): FirebaseFirestore
    fun gson(): Gson
}
