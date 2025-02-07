package com.example.photoclassification.domain.network

import com.example.photoclassification.domain.data.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {

    @Multipart
    @POST("/detect")
    suspend fun uploadImage(
        @Part body: MultipartBody.Part
    ): UploadResponse
}
