package com.example.photoclassification.domain

import android.app.VoiceInteractor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoclassification.ImageApplication.Companion.TAG
import com.example.photoclassification.domain.data.Resource
import com.example.photoclassification.domain.data.UploadImageState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class UploadViewModel constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {


    private val _sharedFlowUploadImage = MutableSharedFlow<UploadImageState>()
    val sharedFlowUploadImage = _sharedFlowUploadImage.asSharedFlow()

    fun uploadImage(
        uri: Uri,
        fileName: String,
        // bitmap: Bitmap
    ) {
        Log.d(TAG, "try create multipart body")
        // val image = createMultipartBody(uri, uri.path ?: "photo")
        // val image = createMultipartBodyFromBitmap(bitmap)
        Log.d(TAG, "finish try create multipart body")
        viewModelScope.launch {
            _sharedFlowUploadImage.emit(UploadImageState(isLoading = true))

            imageRepository.uploadImageClient(uri, fileName).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        Log.i("upload", "success")

                        result.data?.let { uploadImageInfo ->
                            _sharedFlowUploadImage.emit(
                                UploadImageState(
                                    isLoading = false,
                                    uploadResponse = uploadImageInfo
                                )
                            )
                        }
                    }

                    is Resource.Error -> {
                        Log.i("upload", "fail, ${result.message}")

                        _sharedFlowUploadImage.emit(
                            UploadImageState(
                                error = result.message,
                                isLoading = false,
                            )
                        )
                    }

                    is Resource.Loading -> {
                        _sharedFlowUploadImage.emit(
                            UploadImageState(
                                isLoading = result.isLoading
                            )
                        )
                    }
                }
            }
        }
    }

    fun uploadFile(file: File, url: String) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                RequestBody.create(
                    "application/octet-stream"
                        .toMediaTypeOrNull(),
                    file
                )
            )
            .build()
        val request = Request.Builder().url(url) .post(requestBody) .build()

    }

    fun uploadFileViaBitmap(uri: Uri) {
        val body = createMultipartBody(uri, "photo")

    }

    fun createMultipartBody(
        uri: Uri,
        multipartName: String
    ): MultipartBody.Part {
        Log.d(TAG, "uri: $uri, name: $multipartName")
        Log.d(TAG, "file exists in createMultipartBody: ${File(uri.path).exists()}")
        val documentImage = BitmapFactory.decodeFile(uri.path!!)
        val file = File(uri.path!!)
        val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
        documentImage.compress(Bitmap.CompressFormat.JPEG, 100, os)
        os.close()
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        // val requestBody = RequestBody.create(null, "") // TODO change request body
        return MultipartBody.Part.createFormData(multipartName, file.name, requestBody)
    }

    fun createMultipartBodyFromBitmap(
        bitmap: Bitmap
    ): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val body = MultipartBody.Part.createFormData(
            "photo[content]", "photo",
            byteArray.toRequestBody("image/*".toMediaTypeOrNull(), 0, byteArray.size)
        )
        return body
    }
}

