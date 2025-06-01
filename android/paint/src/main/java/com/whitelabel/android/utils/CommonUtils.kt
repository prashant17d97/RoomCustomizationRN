package com.whitelabel.android.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.whitelabel.android.R
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.databinding.DialogLayoutBinding
import com.whitelabel.android.databinding.DlgTakePhotoBinding
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object CommonUtils {
    private var files: ArrayList<String>? = null
    private const val showLog = false
    private val TIMESTAMP_FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val TAG_COLOR_FILTER = Color.BLACK
    private val COLOR_MATRIX_NEGATIVE = floatArrayOf(
        -1.0f,
        0.0f,
        0.0f,
        0.0f,
        255.0f,
        0.0f,
        -1.0f,
        0.0f,
        0.0f,
        255.0f,
        0.0f,
        0.0f,
        -1.0f,
        0.0f,
        255.0f,
        0.0f,
        0.0f,
        0.0f,
        1.0f,
        0.0f
    )

    fun getCorrectlyOrientedBitmap(inputStream: InputStream): Bitmap {
        val exif = ExifInterface(inputStream)
        inputStream.reset() // Reset stream if reused below

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val bitmap = BitmapFactory.decodeStream(inputStream)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }

    fun getARGB(colorProperty: ColorProperty?): Int {
        if (colorProperty == null) {
            return 0
        }
        return colorProperty.colorValue
    }


    fun isColorLight(colorProperty: ColorProperty?): Boolean {
        if (colorProperty == null) {
            return false
        }
        return isColorLight(colorProperty.colorValue)
    }

    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // Standard luminance formula based on human perception
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance > 186  // You can tweak this threshold if needed
    }


    fun setImageViewColor(imageView: ImageView, isSelected: Boolean) {
        var drawable = imageView.drawable
        if (drawable == null) {
            drawable = imageView.background
        }
        if (drawable == null) {
            return
        }
        if (isSelected) {
            val i = TAG_COLOR_FILTER
            if (imageView.getTag(i) == null) {
                val colorMatrixColorFilter = ColorMatrixColorFilter(
                    COLOR_MATRIX_NEGATIVE
                )
                val mutate = drawable.constantState!!.newDrawable().mutate()
                mutate.colorFilter = colorMatrixColorFilter
                imageView.setImageDrawable(mutate)
                imageView.setTag(i, true)
                return
            }
            return
        }
        drawable.clearColorFilter()
        imageView.setTag(TAG_COLOR_FILTER, null)
    }

    @Throws(IOException::class)
    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        try {
            val bArr = ByteArray(1024)
            while (true) {
                val read = inputStream.read(bArr)
                if (read != -1) {
                    outputStream.write(bArr, 0, read)
                } else {
                    inputStream.close()
                    outputStream.flush()
                    outputStream.close()
                    return
                }
            }
        } catch (e: IOException) {
            throw IOException("Error occured while copying file.")
        }
    }

    @JvmOverloads
    fun createAlertDialog(
        context: Context,
        title: String?,
        description: String?,
        positiveButtonText: String?,
        negativeButtonText: String?,
        onClickListener: DialogInterface.OnClickListener?,
        onClickListener2: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val binding = DialogLayoutBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        // Set title if present
        if (!title.isNullOrEmpty()) {
            binding.dialogTitle.visibility = View.VISIBLE
            binding.dialogTitle.text = title
        }

        // Set message
        binding.dialogText.text = description

        // Determine buttons
        if (positiveButtonText == null && negativeButtonText == null) {
            // Only OK button
            binding.dialogPositiveBtnLayout.visibility = View.VISIBLE
            binding.dialogPositiveBtn.text = context.getString(R.string.button_ok)
            binding.dialogPositiveBtn.setOnClickListener {
                onClickListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                dialog.dismiss()
            }
        } else {
            // Positive Button
            if (!positiveButtonText.isNullOrEmpty()) {
                binding.dialogPositiveBtnLayout.visibility = View.VISIBLE
                binding.dialogPositiveBtn.text = positiveButtonText
                binding.dialogPositiveBtn.setOnClickListener {
                    onClickListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                    dialog.dismiss()
                }
            }
            // Negative Button
            if (!negativeButtonText.isNullOrEmpty()) {
                binding.dialogNegativeBtnLayout.visibility = View.VISIBLE
                binding.dialogNegativeBtn.text = negativeButtonText
                binding.dialogNegativeBtn.setOnClickListener {
                    onClickListener2?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }


    @JvmOverloads
    fun createTakePhotoDialog(
        context: Context,
        onTakePhotoClickListener: DialogInterface.OnClickListener?,
        onChooseFromAlbumClickListener: DialogInterface.OnClickListener? = null
    ): AlertDialog {
        val binding = DlgTakePhotoBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context, R.style.FullScreenDialog)
            .setView(binding.root)
            .create()
        val params: ViewGroup.LayoutParams = dialog.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.setAttributes(params as WindowManager.LayoutParams?)

        binding.layoutTakePhoto.setOnClickListener {
            onTakePhotoClickListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
        }

        binding.layoutChooseFromAlbum.setOnClickListener {
            onChooseFromAlbumClickListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
        }

        binding.closeButton.setOnClickListener {
            dialog.cancel()
        }

        return dialog
    }


    private fun deleteCache(context: Context) {
        try {
            deleteDir(context.cacheDir)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDir(file: File): Boolean {
        if (file.isDirectory) {
            for (str in file.list()!!) {
                if (!deleteDir(File(file, str))) {
                    return false
                }
            }
            return file.delete()
        } else if (!file.isFile) {
            return false
        } else {
            return file.delete()
        }
    }


}
