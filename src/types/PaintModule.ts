/**
 * Type definitions for the PaintModule native module
 */

/**
 * Interface representing a color in the paint module
 */
export interface ColorProperty {
  colorName: string;
  colorCode: string;
  colorValue: number;
  id: number;
  roomTypeId?: number;
  colorCatalogue?: string;
  r?: number;
  g?: number;
  b?: number;
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
export interface SaveColorClick extends PaintButtonClickEvent {
  type: 'saveColorClick';
  color: string;
}

/**
 * Event emitted when the undo button is clicked
 */
export interface SendToColorConsultationClick extends PaintButtonClickEvent {
  type: 'sendToColorConsultationClick';
  image: string; // Base64 encoded image
}

/**
 * Event emitted when the eraser button is clicked
 */
export interface SaveToProjectClick extends PaintButtonClickEvent {
  type: 'saveToProjectClick';
  image: string; // Base64 encoded image
}

/**
 * Union type of all possible paint button click events
 */
export type PaintEvent =
  | SaveToProjectClick
  | SendToColorConsultationClick
  | SaveColorClick

interface PaintModuleInterface {
  /**
   * Launches the PaintFragment with the specified color and image URI.
   *
   * @param color The hex string representing the color to use (e.g., "#FFFFFF").
   * @param imageUri The URI of the image to be used in the activity.
   * @param id Optional ID representing the color. Defaults to -1 if not provided.
   * @param colorCatalogue Optional name of the color catalogue. Defaults to an empty string if not provided.
   * @param colorName Optional name of the color. Defaults to "Default Color" if not provided.
   * @param colorOptionsListGson Optional JSON string representing a list of color options.
   */
  showPaintFragment(
    color: string,
    imageUri: string,
    id?: number,
    colorCatalogue?: string,
    colorName?: string,
    colorOptionsListGson?: string,
  ):void;

  /**
   * Gets the current color from the ColorProvider.
   *
   * @return An object containing the color information.
   */
  getCurrentColor(): ColorProperty;

  /**
   * Updates the current color in the ColorProvider.
   *
   * @param color An object containing the color information.
   */
  updateColor(color: ColorProperty): void;

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
