import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  Dimensions,
  SafeAreaView,
  ImageSourcePropType,
  Platform,
  PermissionsAndroid,
  Alert,
  DeviceEventEmitter,
  NativeModules,
} from 'react-native';
import {
  launchCamera,
  launchImageLibrary,
  MediaType,
} from 'react-native-image-picker';
import '../types/PaintModule'; // Import the PaintModule type definitions
import {
  PaintEvent,
  ColorProperty,
} from '../types/PaintModule';

const {width} = Dimensions.get('window');
const {PaintModule} = NativeModules;

// Define a set of 16 colors for the color grid
const COLORS = [
  '#FF0000', // Red
  '#00FF00', // Green
  '#0000FF', // Blue
  '#FFFF00', // Yellow
  '#FF00FF', // Magenta
  '#00FFFF', // Cyan
  '#FFA500', // Orange
  '#800080', // Purple
  '#008000', // Dark Green
  '#000080', // Navy
  '#800000', // Maroon
  '#808000', // Olive
  '#008080', // Teal
  '#FFC0CB', // Pink
  '#A52A2A', // Brown
  '#808080', // Gray
];

const MainScreen = () => {
  const [selectedImage, setSelectedImage] =
    useState<ImageSourcePropType | null>(null);
  const [activeColor, setActiveColor] = useState(COLORS[4]);
  const [hasCameraPermission, setHasCameraPermission] = useState(false);
  const [sharedImage] = useState<string | null>(null);
  const [currentNativeColor, setCurrentNativeColor] =
    useState<ColorProperty | null>(null);

  // Function to get the current color from the native module
  const getCurrentNativeColor = () => {
    try {
      const color = PaintModule.getCurrentColor();
      setCurrentNativeColor(color);
      console.log('Current native color:', color);
      return color;
    } catch (error) {
      console.error('Error getting current color:', error);
      return null;
    }
  };

  // Function to update the color in the native module
  const updateNativeColor = (color: string) => {
    try {
      // Convert hex color to RGB
      const r = parseInt(color.substring(1, 3), 16);
      const g = parseInt(color.substring(3, 5), 16);
      const b = parseInt(color.substring(5, 7), 16);
      // eslint-disable-next-line no-bitwise
      const colorValue = (0xff << 24) | (r << 16) | (g << 8) | b;

      setActiveColor(color);
    } catch (error) {
      console.error('Error updating color:', error);
    }
  };

  // Function to load an image from URI in the native module
  const loadImageToNative = useCallback(
    (uri: string) => {
      try {
        // Create parameters with descriptive names matching the updated interface
        const color = `${activeColor}`;
        const id = -1;
        const colorCatalogue = '';
        const colorName = `${activeColor}`;
        const colorOptionsListGson = '';

        PaintModule.showPaintFragment(
          color,
          uri,
          id,
          colorCatalogue,
          colorName,
          colorOptionsListGson
        );
        console.log('Loaded image to native module:', uri);
      } catch (error) {
        console.error('Error loading image to native module:', error);
      }
    },
    [activeColor],
  );

  // We'll check for permissions only when the camera button is pressed
  // This ensures the component is fully attached to an Activity

  const requestCameraPermission = useCallback(async () => {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
          {
            title: 'Camera Permission',
            message: 'App needs camera permission to take pictures',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          },
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          setHasCameraPermission(true);
          return true;
        } else {
          console.log('Camera permission denied');
          Alert.alert(
            'Permission Denied',
            'Camera permission is required to use this feature',
          );
          return false;
        }
      } catch (err) {
        console.warn(err);
        return false;
      }
    } else {
      // For iOS, permissions are handled by the image picker
      setHasCameraPermission(true);
      return true;
    }
  }, []);

  const handleCameraPress = useCallback(async () => {
    // Check if we already have permission, if not request it
    const hasPermission =
      hasCameraPermission || (await requestCameraPermission());

    if (!hasPermission) {
      console.log('Camera permission not granted');
      return;
    }

    const options = {
      mediaType: 'photo' as MediaType,
      includeBase64: false,
      maxHeight: 2000,
      maxWidth: 2000,
    };

    try {
      const response = await launchCamera(options);
      if (response.didCancel) {
        console.log('User cancelled camera');
      } else if (response.errorCode) {
        console.log('Camera Error: ', response.errorMessage);
        Alert.alert(
          'Camera Error',
          response.errorMessage || 'Unknown error occurred',
        );
      } else if (
        response.assets &&
        response.assets.length > 0 &&
        response.assets[0].uri
      ) {
        const source = {uri: response.assets[0].uri};
        setSelectedImage(source);
        // Load the image to the native module
        loadImageToNative(response.assets[0].uri);
        console.log('Image captured successfully');
      } else {
        console.log('No image data found');
        Alert.alert('Error', 'No image data found');
      }
    } catch (error) {
      console.error('Error launching camera:', error);
      Alert.alert('Error', 'Failed to launch camera. Please try again.');
    }
  }, [
    hasCameraPermission,
    requestCameraPermission,
    setSelectedImage,
    loadImageToNative,
  ]);

  const handleGalleryPress = useCallback(async () => {
    const options = {
      mediaType: 'photo' as MediaType,
      includeBase64: false,
      maxHeight: 2000,
      maxWidth: 2000,
    };

    try {
      const response = await launchImageLibrary(options);
      if (response.didCancel) {
        console.log('User cancelled image picker');
      } else if (response.errorCode) {
        console.log('ImagePicker Error: ', response.errorMessage);
      } else if (
        response.assets &&
        response.assets.length > 0 &&
        response.assets[0].uri
      ) {
        const source = {uri: response.assets[0].uri};
        setSelectedImage(source);
        // Load the image to the native module
        loadImageToNative(response.assets[0].uri);
      } else {
        console.log('No image data found');
      }
    } catch (error) {
      console.error('Error launching image library:', error);
      Alert.alert('Error', 'Failed to launch image library. Please try again.');
    }
  }, [setSelectedImage, loadImageToNative]);

  const handleStartPaintActivity = () => {
    if (
      selectedImage &&
      typeof selectedImage !== 'number' &&
      'uri' in selectedImage &&
      selectedImage.uri
    ) {
      console.log(
        'Starting PaintActivity with color:',
        activeColor,
        'and image:',
        selectedImage.uri,
      );
      try {
        // Get the current native color if available
        const nativeColor = currentNativeColor || getCurrentNativeColor();

        // Use the native color's properties if available, otherwise use defaults
        const id = nativeColor?.id || -1;
        const colorCatalogue = nativeColor?.colorCatalogue || '';
        const colorName = nativeColor?.colorName || nativeColor?.colorCode || 'Selected Color';

        PaintModule.showPaintFragment(
          activeColor,
          selectedImage.uri,
          id,
          colorCatalogue,
          colorName,
          ''
        );
      } catch (error) {
        console.error('Error starting PaintActivity:', error);
        Alert.alert(
          'Error',
          'Failed to start paint activity. Please try again.',
        );
      }
    } else {
      Alert.alert('Error', 'Please select an image first.');
    }
  };

  const renderColorGrid = () => {
    return (
      <View style={styles.colorGridContainer}>
        {COLORS.map((color, index) => (
          <TouchableOpacity
            key={index}
            style={[
              styles.colorBox,
              {backgroundColor: color},
              activeColor === color && styles.activeColorBox,
            ]}
            onPress={() => {
              console.log('Color selected:', color);
              setActiveColor(color);
              // Update the color in the native module
              updateNativeColor(color);
            }}
          />
        ))}
      </View>
    );
  };

  // Get the current color when the component mounts
  useEffect(() => {
    try {
      const color = getCurrentNativeColor();
      if (color && color.colorCode) {
        // If the color has a valid hex code, update the active color
        setActiveColor(color.colorCode);
      }
    } catch (error) {
      console.error('Error getting initial color:', error);
    }
  }, []);

  // Set up event listeners for paint button clicks
  useEffect(() => {
    // Handler for paint button click events
    const handlePaintButtonClick = (event: PaintEvent) => {
      console.log('Paint button clicked:', event.type);

      switch (event.type) {
        case 'saveColorClick':
          console.log('Color palette button clicked');
          Alert.alert('Color Palette', 'Color palette button was clicked');
          break;
        case 'sendToColorConsultationClick':
          console.log('Send to color consultation button clicked');
          Alert.alert(
            'Send to Color Consultation',
            'Send to color consultation button was clicked',
          );
          break;
        case 'saveToProjectClick':
          console.log('Save to project button clicked');
          Alert.alert('Save to Project', 'Save to project button was clicked');
          break;

        default:
          console.warn('Unknown paint button click event type:', event);
      }
    };

    // Add event listener
    const subscription = DeviceEventEmitter.addListener(
      'paintButtonClicked',
      handlePaintButtonClick,
    );

    // Clean up the event listener on unmounting
    return () => {
      subscription.remove();
    };
  }, [handleCameraPress, handleGalleryPress]);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={[styles.button, styles.cameraButton]}
          onPress={handleCameraPress}>
          <Text style={styles.buttonText}>Camera</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.button, styles.galleryButton]}
          onPress={handleGalleryPress}>
          <Text style={styles.buttonText}>Gallery</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.imageContainer}>
        {selectedImage ? (
          <Image source={selectedImage} style={styles.image} />
        ) : (
          <Text style={styles.placeholderText}>
            No image selected. Please use Camera or Gallery buttons.
          </Text>
        )}
      </View>

      {selectedImage && (
        <TouchableOpacity
          style={[styles.paintButton, {backgroundColor: activeColor}]}
          onPress={handleStartPaintActivity}>
          <Text style={styles.buttonText}>Start Paint Activity</Text>
        </TouchableOpacity>
      )}

      {sharedImage && (
        <View style={styles.sharedImageContainer}>
          <Text style={styles.sectionTitle}>
            Shared Image from Paint Activity:
          </Text>
          <Image
            source={{uri: `data:image/png;base64,${sharedImage}`}}
            style={styles.sharedImage}
          />
        </View>
      )}

      {currentNativeColor && (
        <View style={styles.nativeColorContainer}>
          <Text style={styles.sectionTitle}>Current Native Color:</Text>
          <View style={styles.nativeColorInfo}>
            <View
              style={[
                styles.nativeColorSwatch,
                {
                  backgroundColor:
                    currentNativeColor.colorCode ||
                    `#${currentNativeColor.colorValue.toString(16)}`,
                },
              ]}
            />
            <View style={styles.nativeColorDetails}>
              <Text style={styles.nativeColorText}>
                Name: {currentNativeColor.colorName || 'Unnamed'}
              </Text>
              <Text style={styles.nativeColorText}>
                Code:{' '}
                {currentNativeColor.colorCode ||
                  `#${currentNativeColor.colorValue.toString(16)}`}
              </Text>
              {currentNativeColor.colorCatalogue && (
                <Text style={styles.nativeColorText}>
                  Fandeck: {currentNativeColor.colorCatalogue}
                </Text>
              )}
            </View>
          </View>
          <TouchableOpacity
            style={[styles.button, styles.refreshButton]}
            onPress={getCurrentNativeColor}>
            <Text style={styles.buttonText}>Refresh Native Color</Text>
          </TouchableOpacity>
        </View>
      )}

      {renderColorGrid()}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  buttonContainer: {
    flexDirection: 'row',
    padding: 16,
  },
  button: {
    flex: 1,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 8,
    marginHorizontal: 4,
  },
  cameraButton: {
    backgroundColor: '#4285F4',
  },
  galleryButton: {
    backgroundColor: '#34A853',
  },
  paintButton: {
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: '#EA4335',
    marginHorizontal: 16,
    marginVertical: 8,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  imageContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  image: {
    width: '100%',
    height: '100%',
    resizeMode: 'contain',
    borderRadius: 8,
  },
  placeholderText: {
    fontSize: 16,
    color: '#757575',
    textAlign: 'center',
  },
  colorGridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    padding: 16,
    backgroundColor: '#fff',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: -2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 5,
  },
  colorBox: {
    width: (width - 32 - 24) / 4, // 4 columns with padding
    height: (width - 32 - 24) / 4,
    margin: 3,
    borderRadius: 8,
  },
  activeColorBox: {
    borderWidth: 3,
    borderColor: '#000',
  },
  sharedImageContainer: {
    marginHorizontal: 16,
    marginVertical: 16,
    padding: 8,
    backgroundColor: '#fff',
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
    color: '#333',
  },
  sharedImage: {
    width: '100%',
    height: 200,
    resizeMode: 'contain',
    borderRadius: 4,
  },
  nativeColorContainer: {
    marginHorizontal: 16,
    marginVertical: 16,
    padding: 8,
    backgroundColor: '#fff',
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  nativeColorInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 8,
  },
  nativeColorSwatch: {
    width: 50,
    height: 50,
    borderRadius: 4,
    marginRight: 12,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  nativeColorDetails: {
    flex: 1,
  },
  nativeColorText: {
    fontSize: 14,
    color: '#333',
    marginBottom: 4,
  },
  refreshButton: {
    backgroundColor: '#4285F4',
    marginTop: 8,
  },
});

export default MainScreen;
