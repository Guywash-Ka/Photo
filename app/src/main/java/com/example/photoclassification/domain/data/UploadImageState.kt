package com.example.photoclassification.domain.data

data class UploadImageState(
    val uploadResponse: UploadResponse = UploadResponse(""),
    var isLoading: Boolean = false,
    val error: String? = null
)
