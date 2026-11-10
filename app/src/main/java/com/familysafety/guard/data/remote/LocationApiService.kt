package com.familysafety.guard.data.remote

import com.familysafety.guard.data.model.LocationPayload
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface LocationApiService {

    @POST
    suspend fun postLocation(
        @Url fullUrl: String,
        @Body payload: LocationPayload
    ): Response<Map<String, Any>>

    companion object {
        fun create(): LocationApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .retryOnConnectionFailure(false)
                .build()

            // Dummy base url; dynamic URL is provided in @Url fullUrl parameter
            return Retrofit.Builder()
                .baseUrl("http://127.0.0.1/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LocationApiService::class.java)
        }
    }
}