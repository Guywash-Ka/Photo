package com.example.photoclassification.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.photoclassification.ImageApplication.Companion.TAG
import com.example.photoclassification.domain.data.Resource
import com.example.photoclassification.domain.data.UploadResponse
import com.example.photoclassification.domain.network.RetrofitInstance.imageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class ImageRepositoryImpl private constructor(
    private val context: Context
) : ImageRepository {

    override suspend fun uploadImage(image: MultipartBody.Part): Flow<Resource<UploadResponse>> {
        // val requestBody = image.asRequestBody("image/*".toMediaTypeOrNull())
        // val requestBody = RequestBody.create(null, "") // TODO change request body
        return flow {
            try {
                emit(Resource.Loading(true))
                Log.d(TAG, "image body ${image.body}")
                Log.d(TAG, "content type: ${image.body.contentType()}")
                val uploadImageResponse = imageService.uploadImage(
                    // image = image,
                    // requestBody = requestBody
                    body = image
                )

                emit(Resource.Success(data = uploadImageResponse))
                emit(Resource.Loading(false))
            }
            catch (e: IOException) {
                emit(Resource.Error(errorMessage = "Couldn't upload image. Error: ${e.message}"))
            }
            catch (e: HttpException) {
                emit(Resource.Error(errorMessage = "${ e.message}"))
            }
        }
    }

    override suspend fun uploadImageClient(uri: Uri, fileName: String): Flow<Resource<UploadResponse>> {
        return flow {
            try {
                emit(Resource.Loading(true))
                val client = OkHttpClient()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, File(uri.path!!).asRequestBody())
                    .build()

                val uploadImageResponse = getsmth(requestBody, client)

                emit(Resource.Success(data = UploadResponse(uploadImageResponse)))
                emit(Resource.Loading(false))
            }
            catch (e: IOException) {
                emit(Resource.Error(errorMessage = "Couldn't upload image. Error: ${e.message}"))
            }
            catch (e: HttpException) {
                emit(Resource.Error(errorMessage = "${ e.message}"))
            }
        }
    }

    suspend fun getsmth(requestBody: RequestBody, client: OkHttpClient): String{
        val request = Request.Builder()
            .url("http://158.160.17.229:8502/detect_punk_client/")
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            val result = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body!!.string()
            }
            return@withContext result
        }
        return ""
    }

    companion object {
        private var INSTANCE: ImageRepositoryImpl? = null

        // Application's context lives long enough
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ImageRepositoryImpl(context)
            }
        }

        fun get(): ImageRepository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }
}
