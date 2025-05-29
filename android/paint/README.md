# Paint Module

## Overview
The Paint Module is a reusable Android component that provides image editing and coloring functionality. It allows users to select images from the gallery or camera, apply colors to different areas of the image, erase changes, and share the edited images.

## Architecture
The Paint Module is designed to be independent and reusable, avoiding circular dependencies with the app module. It uses a registry pattern to allow communication between the app module and the paint module without creating direct dependencies.

### Key Components

#### Interfaces
- **PaintButtonClickListener**: Interface for handling button click events from the paint module. This interface should be implemented by the calling activity or fragment.
- **ColorProvider**: Interface for providing and updating colors used in the paint module. This interface is implemented within the paint module (PaintFragment) and does not need to be implemented at the calling site.
- **PaintInterfaceRegistry**: Registry for storing and retrieving interface implementations.

#### UI Components
- **PaintActivity**: Entry point for the paint module.
- **PaintFragment**: Main UI component that provides the painting functionality.
- **RecolourImageView**: Custom view for handling the actual painting operations.

#### Events
- **PaintClickEvent**: Sealed interface representing different button click events.

## Integration Guide

### 1. Add the Module Dependency
Add the paint module to your app's build.gradle file:

```kotlin
dependencies {
    implementation(project(":paint"))
}
```

### 2. Implement the Required Interface
In your activity or fragment, implement the `PaintButtonClickListener` interface:

```kotlin
class MainActivity : AppCompatActivity(), PaintButtonClickListener {
    // Implementation of PaintButtonClickListener
    override fun onPaintButtonClicked(event: PaintClickEvent) {
        when (event) {
            is PaintClickEvent.ColorPalette -> {
                // Handle color palette click
                val colorProvider = PaintInterfaceRegistry.getColorProvider()
                colorProvider?.updateColor(ImageMaskColor(
                    colorName = "New Red",
                    colorCode = "#FF0000",
                    colorValue = Color.RED
                ))
            }
            is PaintClickEvent.ShareClick -> {
                // Handle share click
            }
            else -> {
                // Handle other events
            }
        }
    }
}
```

### 3. Register the Interface
Register your implementation with the `PaintInterfaceRegistry` in your activity's `onCreate` method:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...

    // Register this activity with the PaintInterfaceRegistry
    PaintInterfaceRegistry.registerButtonClickListener(this)

    // Start the PaintActivity
    val intent = Intent(this, PaintActivity::class.java)
    startActivity(intent)
}
```

### 4. Unregister the Interface
Don't forget to unregister your implementation in your activity's `onDestroy` method:

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // Unregister this activity when it's destroyed
    PaintInterfaceRegistry.unregisterButtonClickListener()
}
```

## Features

### Image Selection
Users can select images from the gallery or take a new photo using the camera.

### Painting Tools
- **Paint Roller**: Apply colors to areas of the image.
- **Eraser**: Remove applied colors.
- **Undo**: Revert the last change.

### Color Management
The app module can update colors by getting the `ColorProvider` from the `PaintInterfaceRegistry` and calling its `updateColor` method. The `ColorProvider` interface is implemented within the paint module.

### Sharing
Users can share their edited images.

## Button Click Events
The paint module provides the following button click events through the `PaintClickEvent` sealed interface:

- **ColorPalette**: When the color palette button is clicked.
- **UndoClick**: When the undo button is clicked.
- **EraserClick**: When the eraser button is clicked.
- **ShareClick**: When the share button is clicked.
- **PaintRoll**: When the paint roller button is clicked.
- **ImageRequest**: When the user requests to select an image.
- **NewImageRequest**: When the user requests to select a new image.

## Example Implementation
Here's a complete example of how to implement and use the paint module:

```kotlin
class MainActivity : AppCompatActivity(), PaintButtonClickListener {
    private var activityMainBinding: ActivityMainBinding? = null
    private val binding get() = activityMainBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register this activity with the PaintInterfaceRegistry
        PaintInterfaceRegistry.registerButtonClickListener(this)

        // Start the PaintActivity
        val intent = Intent(this, PaintActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister this activity when it's destroyed
        PaintInterfaceRegistry.unregisterButtonClickListener()
    }

    // Implementation of PaintButtonClickListener
    override fun onPaintButtonClicked(event: PaintClickEvent) {
        val colorProvider = PaintInterfaceRegistry.getColorProvider()
        when (event) {
            is PaintClickEvent.ColorPalette -> {
                // Handle color palette click
                colorProvider?.updateColor(ImageMaskColor(
                    colorName = "New Red",
                    colorCode = "#FF0000",
                    colorValue = Color.RED
                ))
                Toast.makeText(this, "Color palette clicked", Toast.LENGTH_SHORT).show()
            }
            is PaintClickEvent.ShareClick -> {
                Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.d("MainActivity", "Button clicked: $event")
            }
        }
    }
}
```

## Conclusion
The Paint Module provides a flexible and reusable way to add image editing functionality to your Android application. By implementing the required interface (PaintButtonClickListener) and registering it with the `PaintInterfaceRegistry`, you can easily integrate the paint module into your app and customize its behavior to suit your needs. The ColorProvider interface is implemented within the paint module itself and does not need to be implemented at the calling site.
