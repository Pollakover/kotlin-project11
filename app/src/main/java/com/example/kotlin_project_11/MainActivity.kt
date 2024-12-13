package com.example.kotlin_project_11

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageDownloaderApp()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDownloaderApp() {

    val networkDispatcher = remember { newSingleThreadContext("Network") }
    val diskDispatcher = remember { newSingleThreadContext("Disk") }

    var imageUrl by remember { mutableStateOf(TextFieldValue("")) }
    var images by remember { mutableStateOf(listOf<Bitmap>()) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                val images = remember { mutableStateOf(emptyList<Bitmap>()) }

                LaunchedEffect(Unit) {
                    images.value = loadImagesFromDocuments()
                }


                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Список изображений",
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(images.value) { image ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Image(
                                    bitmap = image.asImageBitmap(),
                                    contentDescription = "Загруженное изображение",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Загрузка изображений") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Загруженные изображения доступны в списке изображений.",
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                BasicTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (imageUrl.text.isEmpty()) {
                            Text(text = "Введите ссылку...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )


                Button(onClick = {
                    val url = imageUrl.text
                    if (url.isNotBlank()) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val bitmap = withContext(networkDispatcher) { downloadImage(url) }
                            if (bitmap != null) {
                                images = images + bitmap
                                withContext(diskDispatcher) { saveImage(bitmap) }
                            }
                        }
                    }
                }) {
                    Text("Загрузить изображение")
                }
            }
        }
    }
}

fun downloadImage(url: String): Bitmap? {
    return try {
        val inputStream = URL(url).openStream()
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

fun saveImage(bitmap: Bitmap) {
    val documentsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val file = File(documentsDir, "downloaded_image_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
}

fun loadImagesFromDocuments(): List<Bitmap> {
    val documentsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val imageFiles = documentsDir?.listFiles { file ->
        file.extension.equals("png", ignoreCase = true)
    } ?: emptyArray()

    return imageFiles.mapNotNull { file ->
        BitmapFactory.decodeFile(file.absolutePath)
    }
}
