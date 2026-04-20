package com.example.nexiride2.di

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.supabase.SupabasePostgrestApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseOkHttpClient(supabaseClient: SupabaseClient): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val anon = BuildConfig.SUPABASE_ANON_KEY
                val bearer = supabaseClient.auth.currentSessionOrNull()?.accessToken ?: anon
                val req = chain.request().newBuilder()
                    .addHeader("apikey", anon)
                    .addHeader("Authorization", "Bearer $bearer")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(req)
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseRetrofit(
        @Named("supabase") client: OkHttpClient,
        gson: Gson
    ): Retrofit {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrestApi(@Named("supabase") retrofit: Retrofit): SupabasePostgrestApi =
        retrofit.create(SupabasePostgrestApi::class.java)
}
