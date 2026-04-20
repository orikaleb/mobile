package com.example.nexiride2.data.supabase

import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SupabaseSeedEntryPoint {
    fun supabaseApi(): SupabasePostgrestApi
    fun gson(): Gson
    fun supabaseClient(): SupabaseClient
}
