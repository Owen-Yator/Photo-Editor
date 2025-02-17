package com.example.myphotoeditapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myphotoeditapp.ui.theme.MyPhotoEditAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoEditingApp()
        }
    }
}

@Composable
fun PhotoEditingApp(){

    var imageUri by remember{ mutableStateOf<Uri?>(null) }
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var exposure by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var activeFilter by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri: Uri? ->
        imageUri = uri
    }
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val bitmap = remember(imageUri){
            imageUri?.let { loadBitmapFromUri(it, context) }

        }

        if (bitmap == null) {
            Image(
                painterResource(id = R.drawable.baseline_image_24),
                contentDescription = "Default Image",
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(400.dp)

            )
        }else{
            val editedBitmap = remember(bitmap, brightness, contrast, exposure, saturation) {
                applyFilters(bitmap, brightness, contrast, exposure, saturation)
            }
            Image(
                bitmap = editedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {launcher.launch("image/*")}) {
            Text("Select Image")
        }
        Spacer(modifier = Modifier.height(10.dp))

        if(bitmap != null){
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {activeFilter = "Brightness"}) { Text("Brightness")}
                Button(onClick = {activeFilter = "Contrast"}) { Text("Contrast")}
                Button(onClick = {activeFilter = "Exposure"}) { Text("Exposure")}
                Button(onClick = {activeFilter = "Saturation"}) { Text("Saturation")}
                }
            Spacer(modifier = Modifier.height(16.dp))

            activeFilter?.let { filter->
                FilterControl(name = filter, value = when (filter){
                    "Brightness" -> brightness
                    "Contrast" -> contrast
                    "Exposure" -> exposure
                    "Saturation" -> saturation
                    else -> 0f
                },
                    min = if (filter == "Contrast" || filter == "Saturation") 0f else -100f,
                    max = if (filter == "Contrast" || filter == "Saturation") 2f else 100f
                    ){value ->
                    when(filter){
                        "Brightness" -> brightness = value
                        "Contrast" -> contrast = value
                        "Exposure" -> exposure = value
                        "Saturation" -> saturation = value
                    }

                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = {
                bitmap?.let {
                    val editedBitmap = applyFilters(it, brightness, contrast, exposure, saturation)
                    saveImageToGallery(context, editedBitmap)
                }
            }) {
                Text(text = "Saved To Gallery")
            }
            }
        }
    }

fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap{
    val options = BitmapFactory.Options().apply {
        inSampleSize = 2
    }
    val inputStream = context.contentResolver.openInputStream(uri)
    return BitmapFactory.decodeStream(inputStream,null,options)?:
    throw IllegalArgumentException("Failed To Load Bitmap")
}
@Composable
fun FilterControl(name:String, value:Float, min:Float, max: Float, onValueChange: (Float) -> Unit){
    Column (horizontalAlignment = Alignment.CenterHorizontally){
        Text(text = "$name: ${"%.2f".format(value)}", fontSize = 16.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()


        )
    }
}
fun applyFilters(bitmap: Bitmap, brightness:Float,contrast:Float, exposure:Float, saturation:Float,): Bitmap{
    val width = bitmap.width
    val height = bitmap.height
    val bmp = Bitmap.createBitmap(width,height,bitmap.config ?: Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bmp)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    colorMatrix.setSaturation(saturation)

    val brightnessMatrix = ColorMatrix(
        floatArrayOf(
            1f,0f,0f,0f, brightness,
            0f,1f,0f,0f, brightness,
            0f,0f,1f,0f, brightness,
            0f,0f,0f,1f,0f, brightness
        )

    )
    colorMatrix.postConcat(brightnessMatrix)

    val scale = contrast
    val translate = (-0.5f*scale+0.5f)*255
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            scale,0f,0f,0f,translate,
            0f,scale,0f,0f,translate,
            0f,0f,scale,0f,translate,
            0f,0f,0f,1f,0f
        )
    )
    colorMatrix.postConcat(contrastMatrix)

    val exposureMatrix = ColorMatrix(
        floatArrayOf(
            1f,0f,0f,0f,exposure,
            0f,1f,0f,0f,exposure,
            0f,0f,1f,0f,exposure,
            0f,0f,0f,1f,0f
        )
    )
    colorMatrix.postConcat(exposureMatrix)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap,0f,0f,paint)

    return bmp
}
fun saveImageToGallery(context: Context,bitmap: Bitmap){
    val filename = "EditedImage_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME,filename)
        put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        put(MediaStore.Images.Media.IS_PENDING,1)

    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING,0)
        Toast.makeText(context,"Image Saved To Gallery",Toast.LENGTH_SHORT).show()

    }
}



fun Slider(value: Float) {

}
