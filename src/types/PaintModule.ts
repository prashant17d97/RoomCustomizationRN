/**
 * Type definitions for the PaintModule native module
 */

/**
 * Interface representing a color in the paint module
 */
export interface ImageMaskColor {
  colorName: string;
  colorCode: string;
  colorValue: number;
  fandeckId: number;
  fandeckName: string;
}

/**
 * Base interface for paint button click events
 */
export interface PaintButtonClickEvent {
  type: string;
}

/**
 * Event emitted when the color palette button is clicked
 */
export interface ColorPaletteEvent extends PaintButtonClickEvent {
  type: 'colorPalette';
}

/**
 * Event emitted when the undo button is clicked
 */
export interface UndoClickEvent extends PaintButtonClickEvent {
  type: 'undoClick';
}

/**
 * Event emitted when the eraser button is clicked
 */
export interface EraserClickEvent extends PaintButtonClickEvent {
  type: 'eraserClick';
}

/**
 * Event emitted when the share button is clicked
 */
export interface ShareClickEvent extends PaintButtonClickEvent {
  type: 'shareClick';
  image: string; // Base64 encoded image
}

/**
 * Event emitted when the paint roll button is clicked
 */
export interface PaintRollEvent extends PaintButtonClickEvent {
  type: 'paintRoll';
}

/**
 * Event emitted when an image is requested
 */
export interface ImageRequestEvent extends PaintButtonClickEvent {
  type: 'imageRequest';
}

/**
 * Event emitted when a new image is requested
 */
export interface NewImageRequestEvent extends PaintButtonClickEvent {
  type: 'newImageRequest';
}

/**
 * Union type of all possible paint button click events
 */
export type PaintEvent =
  | ColorPaletteEvent
  | UndoClickEvent
  | EraserClickEvent
  | ShareClickEvent
  | PaintRollEvent
  | ImageRequestEvent
  | NewImageRequestEvent;

interface PaintModuleInterface {
  /**
   * Launches the PaintActivity with the specified color and image URI.
   *
   * @param color The hex string representing the color to use (e.g., "#FFFFFF").
   * @param imageUri The URI of the image to be used in the activity.
   * @param fandeckId Optional ID representing the fandeck. Defaults to -1 if not provided.
   * @param fandeckName Optional name of the fandeck. Defaults to an empty string if not provided.
   * @param colorName Optional name of the color. Defaults to "Default Color" if not provided.
   */
  startPaintActivity(
    color: string,
    imageUri: string,
    fandeckId?: number,
    fandeckName?: string,
    colorName?: string
  ): void;

  /**
   * Gets the current color from the ColorProvider.
   *
   * @return An object containing the color information.
   */
  getCurrentColor(): ImageMaskColor;

  /**
   * Updates the current color in the ColorProvider.
   *
   * @param color An object containing the color information.
   */
  updateColor(color: ImageMaskColor): void;

  /**
   * Loads an image from the provided URI using the ImageProvider.
   *
   * @param imageUri The URI of the image to be loaded.
   */
  loadImageUri(imageUri: string): void;

  /**
   * Loads an image from a base64 encoded string using the ImageProvider.
   *
   * @param base64Image The base64 encoded string representation of the image.
   */
  loadImageBase64(base64Image: string): void;
}

declare module 'react-native' {
  interface NativeModulesStatic {
    PaintModule: PaintModuleInterface;
  }
}
