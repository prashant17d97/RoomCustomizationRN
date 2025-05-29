package com.whitelabel.android.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.whitelabel.android.data.model.ImageMaskColor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
                context,
                "Out Of Memory",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun getARGB(imageMaskColor: ImageMaskColor?): Int {
        if (imageMaskColor == null) {
            return 0
        }
        return imageMaskColor.colorValue?:Color.RED
    }


    fun String.hexToColor(): Int {
        val normalized = if (startsWith("#")) this else "#$this"
        return try {
            normalized.toColorInt()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid color hex: $this")
        }
    }
}