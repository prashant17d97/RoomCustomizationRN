package com.whitelabel.android.ui.paint.fragment

import android.graphics.Bitmap

sealed interface BottomSheetClickEvent {
    data class SaveToProjectClick(val bitmap: Bitmap) : BottomSheetClickEvent
    data class SendToColorConsultationClick(val bitmap: Bitmap) : BottomSheetClickEvent
    data class SaveColorClick(val hexColorString: String) : BottomSheetClickEvent
}