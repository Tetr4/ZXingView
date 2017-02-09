ZXing Fragment for Android
=============

This repository contains code for an Android [fragment](https://developer.android.com/guide/components/fragments.html) that displays the camera's viewfinder and allows scanning barcodes via the [ZXing](https://github.com/zxing/zxing) image processing library.  

The fragment uses [AsyncTasks](https://developer.android.com/reference/android/os/AsyncTask.html) for smooth scanning, without interrupting the UI-Thread. Changing the display orientation during scanning is also supported.

Have a look at [MainActivity](app/src/main/java/de/klimek/zxingfragment/MainActivity.java) for a sample implementation.
