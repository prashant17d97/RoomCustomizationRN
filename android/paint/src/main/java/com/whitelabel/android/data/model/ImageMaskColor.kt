package com.whitelabel.android.data.model

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class ImageMaskColor(
    val colorName: String = "",
    val colorCode: String = "",
    val colorValue: Int = Color.DKGRAY,
    val fandeckId: Int = -1,
    val fandeckName: String = "",
)