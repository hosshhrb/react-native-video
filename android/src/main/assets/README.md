# Local Ad Video Replacement

This directory is where you should place the video file that will be used to replace IMA SDK ads.

## How to use this feature

1. Place your local ad video file named `local_ad.mp4` in this directory
2. The video will automatically be used instead of any ad served by the IMA SDK
3. All ad events will still be triggered as normal, allowing the rest of the app to function without changes

## Customizing ad behavior

If you need to use a different file name or path, you can modify the `localAdPath` variable in the `LocalAdReplacingMediaSourceFactory` class in `ReactExoplayerView.java`.

```java
private final String localAdPath = "file:///android_asset/local_ad.mp4"; // Path to your local ad file
```

You can also add multiple ad videos and randomly select between them by modifying the implementation. 