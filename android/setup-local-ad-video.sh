#!/bin/bash

# Setup script for downloading a sample video for ad replacement
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
RESOURCES_DIR="$SCRIPT_DIR/src/main/res/raw"
SAMPLE_VIDEO_URL="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
LOCAL_AD_FILENAME="local_ad.mp4"

echo "Setting up local ad video for react-native-video..."

# Create resources directory if it doesn't exist
mkdir -p "$RESOURCES_DIR"

# Check if video already exists
if [ -f "$RESOURCES_DIR/$LOCAL_AD_FILENAME" ]; then
    echo "Local ad video already exists at $RESOURCES_DIR/$LOCAL_AD_FILENAME"
    echo "To replace it, delete the file and run this script again."
    exit 0
fi

# Download the sample video
echo "Downloading sample video from $SAMPLE_VIDEO_URL..."
if command -v curl &> /dev/null; then
    curl -L "$SAMPLE_VIDEO_URL" -o "$RESOURCES_DIR/$LOCAL_AD_FILENAME"
elif command -v wget &> /dev/null; then
    wget "$SAMPLE_VIDEO_URL" -O "$RESOURCES_DIR/$LOCAL_AD_FILENAME"
else
    echo "Error: Neither curl nor wget is installed. Please install one of them and try again."
    exit 1
fi

# Check if download was successful
if [ $? -eq 0 ] && [ -f "$RESOURCES_DIR/$LOCAL_AD_FILENAME" ]; then
    echo "Sample video downloaded successfully to $RESOURCES_DIR/$LOCAL_AD_FILENAME"
    echo "You can now build your project with ad replacement functionality."
    # Make the script executable
    chmod +x "$SCRIPT_DIR/setup-local-ad-video.sh"
else
    echo "Error: Failed to download the sample video."
    echo "Please download a video manually and place it at $RESOURCES_DIR/$LOCAL_AD_FILENAME"
    exit 1
fi 