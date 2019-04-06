ZXing View for Android [![](https://jitpack.io/v/Tetr4/ZXingView.svg)](https://jitpack.io/#Tetr4/ZXingView)
=============

This repository contains code for an Android view that displays the camera's viewfinder and allows scanning barcodes via the [ZXing](https://github.com/zxing/zxing) image processing library.  

The fragment uses [AsyncTasks](https://developer.android.com/reference/android/os/AsyncTask.html) for smooth scanning, without interrupting the UI-Thread. Changing the display orientation during scanning is also supported.

## Download

1. Add [JitPack](https://jitpack.io/) to your top-level `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

2. Add the library as a dependency to your apps `build.gradle`:

```gradle
dependencies {
    compile 'com.github.Tetr4:ZXingView:v1.1.0'
}
```

## Usage

1. Use the ScannerView in you layout:

```xml
<de.klimek.scanner.ScannerView
    android:id="@+id/scanner"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:reticle_fraction="0.5"
    app:reticle_color="@android:color/holo_green_light"
    app:allow_frontcamera="true"
    app:use_flash="true"
    app:decode_interval="500" />
```

2. Add a callback and start/stop the scanner in your activity:

```java
ScannerView scanner = (ScannerView) findViewById(R.id.scanner);
scanner.setOnDecodedCallback(new OnDecodedCallback() {
    @Override
    public void onDecoded(String decodedData) {
        Toast.makeText(MainActivity.this, decodedData, Toast.LENGTH_SHORT).show();
    }
});
scanner.startScanning();
scanner.stopScanning();
```

Have a look at the [sample app](sample/src/main/java/de/klimek/scanner/sample/MainActivity.java) for a reference on how to handle runtime permissions for the camera.
