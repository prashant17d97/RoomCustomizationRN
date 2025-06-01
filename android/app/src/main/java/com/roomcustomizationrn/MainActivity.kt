package com.roomcustomizationrn

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.whitelabel.android.data.model.ImageMaskColor
import com.whitelabel.android.interfaces.ColorProvider
import com.whitelabel.android.interfaces.ImageProvider
import com.whitelabel.android.interfaces.PaintButtonClickListener
import com.whitelabel.android.interfaces.PaintInterfaceRegistry
import com.whitelabel.android.ui.paint.fragment.BottomSheetClickEvent
import com.whitelabel.android.utils.ActivityLoader.loadPaintFragment


class MainActivity : ReactActivity(), PaintButtonClickListener {

  /**
   * Returns the ColorProvider instance.
   * This method is used by the PaintModule to access the ColorProvider.
   *
   * @return The ColorProvider instance.
   */
  fun getColorProvider(): ColorProvider = PaintInterfaceRegistry.getColorProvider()

  /**
   * Returns the ImageProvider instance.
   * This method is used by the PaintModule to access the ImageProvider.
   *
   * @return The ImageProvider instance.
   */
  fun getImageProvider(): ImageProvider = PaintInterfaceRegistry.getImageProvider()

  override fun getMainComponentName(): String = "RoomCustomizationRN"

  private var _fragmentContainerId: Int = 0
  val fragmentContainerId: Int
    get() = _fragmentContainerId


  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      _fragmentContainerId = loadPaintFragment()
  }

  override fun onStart() {
            super.onStart()
            // Register this activity with the PaintInterfaceRegistry
            PaintInterfaceRegistry.registerButtonClickListener(this)
        }

  override fun onDestroy() {
              super.onDestroy()
              // Unregister this activity when it's destroyed
              PaintInterfaceRegistry.unregisterButtonClickListener()
          }

          // Implementation of PaintButtonClickListener
  override fun onPaintButtonClicked(event: BottomSheetClickEvent) {
                // Forward the event to the PaintModule to send to React Native
                PaintModule.getInstance()?.handlePaintClickEvent(event)
            }

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

}
