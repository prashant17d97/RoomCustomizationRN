package com.whitelabel.android.utils

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.whitelabel.android.data.model.ColorProperty
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    fun Context.hasCameraPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun Context.createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "UGP_" + timeStamp + "_"
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            externalCacheDir      /* directory */
        )
        return image
    }

    @Throws(IOException::class)
    fun copy(file: File, file2: File) {
        if (!file.exists()) {
            return
        }
        val fileInputStream = FileInputStream(file)
        try {
            val fileOutputStream = FileOutputStream(file2)
            val bArr = ByteArray(1024)
            while (true) {
                val read = fileInputStream.read(bArr)
                if (read > 0) {
                    fileOutputStream.write(bArr, 0, read)
                } else {
                    fileOutputStream.close()
                    fileInputStream.close()
                    return
                }
            }
        } catch (th: Throwable) {
            try {
                fileInputStream.close()
            } catch (th2: Throwable) {
                th.addSuppressed(th2)
            }
            throw th
        }
    }

    @Throws(IOException::class)
    fun copy(str: String, str2: String) {
        copy(File(str), File(str2))
    }

    fun delete(str: String) {
        val file = File(str)
        if (file.exists()) {
            file.delete()
        }
    }

    fun showOutOfMemoryToast(context: Context?) {
        if (context == null) {
            return
        }
        (context as Activity).runOnUiThread {
            Toast.makeText(
                context, "Out Of Memory", Toast.LENGTH_LONG
            ).show()
        }
    }

    fun getARGB(colorProperty: ColorProperty?): Int {
        if (colorProperty == null) {
            return 0
        }
        return colorProperty.colorValue
    }


    fun String.hexToColor(): Int {
        val normalized = if (startsWith("#")) this else "#$this"
        return try {
            normalized.toColorInt()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid color hex: $this")
        }
    }

    fun saveToGallery(bitmap: Bitmap, context: Context): Boolean {
        val displayName = "photo_${System.currentTimeMillis()}.jpg"
        return saveBitmapToGallery(
            context = context, // Assuming 'this' is an Activity or Context
            bitmap = bitmap,
            format = Bitmap.CompressFormat.JPEG,
            mimeType = "image/jpeg",
            displayName = displayName,
        ) != null
    }


    /**
     * Saves a Bitmap object to the device's image gallery.
     *
     * @param context The application context.
     * @param bitmap The Bitmap to save.
     * @param format The compression format for the image (e.g., Bitmap.CompressFormat.JPEG, Bitmap.CompressFormat.PNG).
     * @param mimeType The MIME type of the image (e.g., "image/jpeg", "image/png").
     * @param displayName The desired display name for the image file (e.g., "MyImage.jpg").
     * @param relativePath The relative path for saving the image. For Android Q and above,
     * this is relative to the primary external storage volume (e.g., Environment.DIRECTORY_PICTURES).
     * For older versions, this parameter is used to create a directory if it doesn't exist.
     * @param quality The compression quality (0-100) for JPEG format. Ignored for PNG.
     * @return The Uri of the saved image, or null if saving failed.
     */
    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        displayName: String,
        relativePath: String = Environment.DIRECTORY_PICTURES, // Default to Pictures directory
        quality: Int = 100 // Default quality for JPEG
    ): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            // For Android Q (API 29) and above, use RELATIVE_PATH and IS_PENDING
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending while writing
            }
        }

        // Determine the correct URI for inserting the image
        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var imageUri: Uri? = null
        var outputStream: OutputStream? = null

        try {
            imageUri = contentResolver.insert(collectionUri, contentValues)
            if (imageUri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }

            outputStream = contentResolver.openOutputStream(imageUri)
            if (outputStream == null) {
                throw IOException("Failed to get output stream for MediaStore URI.")
            }

            if (!bitmap.compress(format, quality, outputStream)) {
                throw IOException("Failed to save bitmap.")
            }

            // For Android Q and above, mark as no longer pending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }

            // Show a toast message indicating success
            Toast.makeText(context, "Image saved to gallery: $displayName", Toast.LENGTH_LONG)
                .show()
            return imageUri

        } catch (e: IOException) {
            // If an error occurs, and we have a URI, delete the incomplete entry
            imageUri?.let { orphanUri ->
                contentResolver.delete(orphanUri, null, null)
            }
            e.printStackTrace()
            return null
        } finally {
            try {
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun ActivityResultLauncher<Intent>.shareBitmapFromFragment(
        bitmap: Bitmap,
        context: Context,
        filename: String = "shared_image.png",
        markForDeletion: (File) -> Unit
    ) {

        try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "Shared Image", uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }


            markForDeletion(file)
            launch(Intent.createChooser(shareIntent, "Share Image"))

        } catch (e: Exception) {
            Log.e("ShareBitmap", "Error sharing bitmap: ${e.message}", e)
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }

    fun Any.toJson(): String {
        return Gson().toJson(this)
    }


    fun String?.toColorProperty(): ColorProperty {
        return try {
            Gson().fromJson(this, ColorProperty::class.java)
        } catch (exception: Exception) {
            Log.e("Utils", "toColorProperty: $exception")
            ColorProperty()
        }
    }

    fun String?.convertJsonToColorList(): List<ColorProperty> {
        val type = object : TypeToken<List<ColorProperty>>() {}.type
        return try {
            Gson().fromJson(this, type)
        } catch (exception: Exception) {
            Log.e("Utils", "convertJsonToColorList: $exception")
            emptyList()
        }
    }


    fun Int.toTextColor(): Int {
        val r = (this shr 16) and 0xFF
        val g = (this shr 8) and 0xFF
        val b = this and 0xFF

        // Calculate luminance (perceived brightness)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)

        // Return black or white based on luminance
        return if (luminance > 186) Color.BLACK else Color.WHITE
    }

}