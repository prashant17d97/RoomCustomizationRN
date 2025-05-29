package com.whitelabel.android.ui.paint.fragment

import android.graphics.Bitmap

sealed interface PaintClickEvent {
    data object ColorPalette : PaintClickEvent
    data object UndoClick : PaintClickEvent
    data object EraserClick : PaintClickEvent
    data class ShareClick(val bitmap: Bitmap) : PaintClickEvent
    data object PaintRoll : PaintClickEvent
    data object ImageRequest : PaintClickEvent
    data object NewImageRequest : PaintClickEvent

}