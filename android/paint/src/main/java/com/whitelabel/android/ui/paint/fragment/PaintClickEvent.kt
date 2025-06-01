package com.whitelabel.android.ui.paint.fragment

import android.graphics.Bitmap

sealed interface PaintClickEvent {
    data class ShareClick(val bitmap: Bitmap) : PaintClickEvent
    data object PaintBrush : PaintClickEvent
    data object GalleryRequest : PaintClickEvent
    data object ColorPicker : PaintClickEvent
    data object None : PaintClickEvent
}

