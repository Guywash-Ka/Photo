package com.example.photoclassification.domain.data

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    val isLoading: Boolean = false

    // We'll wrap our data in this 'Success'
    // class in case of success response from api
    class Success(data: UploadResponse) : Resource<UploadResponse>(data = data)

    // We'll pass error message wrapped in this 'Error'
    // class to the UI in case of failure response
    class Error(errorMessage: String) : Resource<UploadResponse>(message = errorMessage)

    // We'll just pass object of this Loading
    // class, just before making an api call
    class Loading(isLoading: Boolean) : Resource<UploadResponse>()
}
