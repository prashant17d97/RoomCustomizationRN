# Room Customization RN

A React Native application that allows users to customize room images by applying colors to different areas using an advanced paint module. This project combines the power of React Native with a native Android paint module for a seamless room customization experience.

## Features

- Image Selection: Choose images from gallery or capture using camera
- Color Customization: Apply colors to different areas of the room
- Paint Tools: Includes paint roller and eraser functionality
- Color Palette: 16 pre-defined colors to choose from
- Share Feature: Share your customized room designs
- Cross-Platform: Built with React Native for iOS and Android support

## Project Structure

The project consists of two main parts:

1. React Native Application (Main App)
2. Native Paint Module (Android)

### React Native Components

- `MainScreen.tsx`: The main interface component that handles:
  - Image selection
  - Color palette management
  - Camera permissions
  - Integration with native paint module

### Paint Module Integration

The paint module is integrated as a native module and provides advanced painting capabilities:

#### Android Paint Module

The paint module is implemented as a standalone Android library that provides:

- `PaintActivity`: Main entry point for the paint functionality
- `PaintFragment`: Handles the painting interface and operations
- `RecolourImageView`: Custom view for painting operations

#### Module Communication

Communication between React Native and the Paint Module is handled through:

1. `PaintModule.kt`: Bridge between React Native and native Android code
2. `PaintInterfaceRegistry`: Manages interface implementations for communication
3. Event System: Uses `DeviceEventEmitter` for native to React Native communication

## Setup and Installation

1. Clone the repository
2. Install dependencies:
   ```bash
   npm install
   # or
   yarn install
   ```
3. Install pod dependencies (iOS):
   ```bash
   cd ios && pod install
   ```

## Usage

1. Launch the application
2. Select an image from gallery or capture using camera
3. Choose a color from the color palette
4. Use the paint tools to customize the room
5. Save or share your customized design

## Paint Module Features

The paint module provides several key features:

- Color Application: Paint different areas of the image
- Eraser Tool: Remove applied colors
- Undo Functionality: Revert recent changes
- Share Capability: Share edited images
- Color Management: Update and manage color palettes

## Technical Implementation

### Native Module Integration

The paint module is integrated into React Native using Native Modules:

```typescript
import { NativeModules } from 'react-native';
const { PaintModule } = NativeModules;
```

### Color Management

Colors are managed using the `ImageMaskColor` model:
```typescript
interface ImageMaskColor {
  colorName: string;
  colorCode: string;  // Hex color value
  colorValue: number; // Native color value
}
```

## Dependencies

- React Native
- react-native-image-picker
- Native Android Paint Module
