package com.example.photoclassification.domain

import android.net.Uri
import com.example.photoclassification.domain.data.Resource
import com.example.photoclassification.domain.data.UploadResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

interface ImageRepository {

    suspend fun uploadImage(image: MultipartBody.Part): Flow<Resource<UploadResponse>>

    suspend fun uploadImageClient(uri: Uri, fileName: String): Flow<Resource<UploadResponse>>

}
