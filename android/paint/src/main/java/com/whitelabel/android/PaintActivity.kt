package com.whitelabel.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.activity.OnBackPressedCallback
import com.whitelabel.android.databinding.ActivityPaintBinding
import com.whitelabel.android.utils.ActivityLoader.COLOR_HEX
import com.whitelabel.android.utils.ActivityLoader.COLOR_NAME
import com.whitelabel.android.utils.ActivityLoader.FANDECK_ID
import com.whitelabel.android.utils.ActivityLoader.FANDECK_NAME
import com.whitelabel.android.utils.ActivityLoader.IMAGE_URI

class PaintActivity : AppCompatActivity() {
    private var activityPaintBinding: ActivityPaintBinding? = null
    private val binding get() = activityPaintBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityPaintBinding = ActivityPaintBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onIntentReceived()
    onBackPressedHandle()
    }

    private fun onBackPressedHandle() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
               finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        onIntentReceived()
    }



    private fun onIntentReceived() {

        val imageUri = intent.getStringExtra(IMAGE_URI)
        val color = intent.getStringExtra(COLOR_HEX)
        val fandeckId = intent.getIntExtra(FANDECK_ID, -1)
        val fandeckName = intent.getStringExtra(FANDECK_NAME) ?: ""
        val colorName = intent.getStringExtra(COLOR_NAME) ?: "Default Color"

        Log.d(
            "PaintActivity",
            "onIntentReceived: imageUri: $imageUri, color: $color, fandeckId: $fandeckId, fandeckName: $fandeckName, colorName: $colorName"
        )

        if (imageUri != null && color != null) {
            val bundle = Bundle().apply {
                putString(IMAGE_URI, imageUri)
                putString(COLOR_HEX, color)
                putInt(FANDECK_ID, fandeckId)
                putString(FANDECK_NAME, fandeckName)
                putString(COLOR_NAME, colorName)
            }

            val navHostFragment =
                activityPaintBinding?.navHostFragment?.id?.let {
                    supportFragmentManager.findFragmentById(
                        it
                    )
                } as NavHostFragment
            val navController = navHostFragment.navController

            navController.navigate(R.id.paintFragment, bundle) // Replace with your fragment ID
        } else {
            throw IllegalArgumentException("Intent extras are null")
        }

    }
}