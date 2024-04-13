package com.example.photoclassification

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.photoclassification.domain.ImageRepositoryImpl
import com.example.photoclassification.domain.UploadViewModel
import com.example.photoclassification.ui.theme.PhotoClassificationTheme
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.photoclassification.ImageApplication.Companion.TAG
import com.example.photoclassification.domain.ImageRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import kotlin.coroutines.coroutineContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImageRepositoryImpl.initialize(applicationContext)
        val imageRepository = ImageRepositoryImpl.get()
        val viewModel = UploadViewModel(imageRepository)
        setContent {
            PhotoClassificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Greeting("Android")
                    // SomeFunction(uploadViewModel)
                    PhotoPicker(
                        imageRepository,
                        viewModel,
                        applicationContext
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhotoClassificationTheme {
        Greeting("Android")
    }
}

@Composable
fun PhotoPicker(
    imageRepository: ImageRepository,
    uploadViewModel: UploadViewModel,
    context: Context
) {
    val composableScope = rememberCoroutineScope()
    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var selectedImageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> selectedImageUris = uris }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text(text = "Pick one photo")
                }
                Button(onClick = {
                    // multiplePhotoPickerLauncher.launch(
                    //     PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    // )
                }) {
                    Text(text = "Pick multiple photo")
                }
            }
        }

        item {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            val fileName = selectedImageUri?.let { context.contentResolver.getFileName(it) } ?: ""
            Log.d(TAG, "fileName: $fileName, selectedImageUri: $selectedImageUri")
            if (selectedImageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri)
                // uploadViewModel.uploadImage(bitmap)
                // uploadViewModel.uploadImageClient(selectedImageUri!!, fileName)
            }
            selectedImageUri?.let { uploadViewModel.uploadImage(it, fileName) }
            //  {
            //     selectedImageUri?.let { imageRepository.uploadImageClient(it, fileName) }
            // }
            // selectedImageUri?.let { uploadViewModel.uploadImage(selectedImageUri!!) }
            // val multipartBody = selectedImageUri?.let { uploadViewModel.createMultipartBody(it, fileName) }

            // if (multipartBody != null) {
            //     uploadViewModel.uploadImage(multipartBody)
            // }
            Log.d(TAG, "selected image uri: $selectedImageUri")
        }

        // items(selectedImageUris) { uri ->
        //     AsyncImage(
        //         model = uri,
        //         contentDescription = null,
        //         modifier = Modifier.fillMaxWidth(),
        //         contentScale = ContentScale.Crop
        //     )
        // }
    }
    AppContent(
        uploadViewModel = uploadViewModel
    )
}

fun ContentResolver.getFileName(uri: Uri): String {
    var name = ""
    val cursor = query(uri, null, null, null, null)
    cursor?.let {
        it.moveToFirst()
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) name = it.getString(idx)
        cursor.close()
    }
    return name
}

@Composable
fun AppContent(
    uploadViewModel: UploadViewModel
) {

    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        BuildConfig.APPLICATION_ID + ".provider", file
    )

    Log.d(TAG, "uri from camera: $uri")

    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val permissionCheckResult =
                ContextCompat.checkSelfPermission(context, CAMERA)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(uri)
            } else {
                // Request a permission
                permissionLauncher.launch(CAMERA)
            }
        }) {
            Text(text = "Capture Image From Camera")
        }
    }

    if (capturedImageUri.path?.isNotEmpty() == true) {
        Image(
            modifier = Modifier
                .padding(16.dp, 8.dp),
            painter = rememberAsyncImagePainter(capturedImageUri),
            contentDescription = null
        )
        // val requestFile =RequestBody.create("multipart/form-data".toMediaTypeOrNull(),file)
        val requestFile= file.asRequestBody("image/*".toMediaTypeOrNull())
        // val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
        Log.d(TAG, "file path: ${file.path} isFileExists: ${File(file.path).exists()}")
        // MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri)
        // uploadViewModel.uploadImage(file.toUri())
        // uploadViewModel.uploadFile(capturedImageUri.toFile(), uri.toString())
        // uploadViewModel.uploadFileViaBitmap(capturedImageUri)
    }
}

@SuppressLint("SimpleDateFormat")
fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val file = File(externalCacheDir,  imageFileName + ".jpg")
    file.createNewFile()
    Log.d(TAG, "filePath while creating: ${file.path}")
    // return File.createTempFile(
    //     imageFileName, /* prefix */
    //     ".jpg", /* suffix */
    //     externalCacheDir      /* directory */
    // )
    return file
}
