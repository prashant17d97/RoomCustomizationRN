package com.whitelabel.android.interfaces

/**
 * Registry for paint module interfaces.
 * This class allows the app module to register implementations of the interfaces
 * without creating a circular dependency.
 */
object PaintInterfaceRegistry {
    private var buttonClickListener: PaintButtonClickListener? = null
    private var colorProvider: ColorProvider? = null
    private var imageProvider: ImageProvider? = null

    /**
     * Gets the registered PaintButtonClickListener.
     * @return The registered listener, or null if none is registered.
     */
    fun getButtonClickListener(): PaintButtonClickListener? = buttonClickListener

    /**
     * Gets the registered ColorProvider.
     * @return The registered provider, or null if none is registered.
     */
    fun getColorProvider(): ColorProvider =
        colorProvider ?: throw IllegalStateException("No ColorProvider registered")

    /**
     * Retrieves the currently registered ImageProvider instance.
     *
     * @return The registered ImageProvider instance, or null if none is registered.
     */
    fun getImageProvider(): ImageProvider =
        imageProvider ?: throw IllegalStateException("No ImageProvider registered")

    /**
     * Registers an ImageProvider instance. The registered provider is used to handle
     * image loading functionality in the paint module. The provided implementation enables
     * the paint module to load images from various sources, such as a Bitmap or a URI.
     *
     * @param provider The ImageProvider instance to register. This implementation
     *        will be used to load images required by the paint module.
     */
    fun registerImageProvider(provider: ImageProvider) {
        imageProvider = provider
    }

    /**
     * Registers a PaintButtonClickListener.
     * @param listener The listener to register.
     */
    fun registerButtonClickListener(listener: PaintButtonClickListener) {
        buttonClickListener = listener
    }

    /**
     * Registers a ColorProvider.
     * @param provider The provider to register.
     */
    fun registerColorProvider(provider: ColorProvider) {
        colorProvider = provider
    }

    /**
     * Unregisters the PaintButtonClickListener.
     */
    fun unregisterButtonClickListener() {
        buttonClickListener = null
    }

    /**
     * Unregisters the ColorProvider.
     */
    fun unregisterColorProvider() {
        colorProvider = null
    }

    /**
     * Unregisters the currently registered ImageProvider instance.
     * This method removes the registered ImageProvider, ensuring that no image
     * loading functionality is linked to the paint module. After invoking this
     * method, any attempts to retrieve an ImageProvider will result in a null value.
     */
    fun unregisterImageProvider() {
        imageProvider = null
    }
}