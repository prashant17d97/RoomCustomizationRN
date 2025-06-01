package com.whitelabel.android.utils

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.whitelabel.android.data.model.ColorProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

object CoroutineUtils {

    fun getAllColors(context: Context, list: MutableList<ColorProperty>) {
//        getAllColors(context, list, null)
    }

    suspend fun <T> backgroundScope(
        block: suspend CoroutineScope.() -> T
    ): T {
        return withContext(Dispatchers.IO, block)
    }

    suspend fun <T> mainScope(
        block: suspend CoroutineScope.() -> T
    ): T {
        return withContext(Dispatchers.Main, block)
    }

    suspend fun <T> defaultScope(
        block: suspend CoroutineScope.() -> T
    ): T {
        return withContext(Dispatchers.Main, block)
    }

    fun ViewModel.launchViewModelScope(
        context: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(context, block = block)
    }

    fun Fragment.launchLifeCycleScope(
        context: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        lifecycleScope.launch(context, block = block)
    }

    fun LifecycleOwner.launchLifeCycleScope(
        context: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        lifecycleScope.launch(context, block = block)
    }
}
