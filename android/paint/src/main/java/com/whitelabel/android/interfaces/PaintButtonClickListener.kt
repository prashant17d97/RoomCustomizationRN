package com.whitelabel.android.interfaces

import com.whitelabel.android.ui.paint.fragment.BottomSheetClickEvent

/**
 * Interface for listening to button clicks in the paint module.
 * This interface should be implemented by the calling activity or fragment
 * to handle button click events from the paint module.
 */
interface PaintButtonClickListener {
    /**
     * Called when a button is clicked in the paint module.
     * @param event The type of button click event.
     */
    fun onPaintButtonClicked(event: BottomSheetClickEvent)
}