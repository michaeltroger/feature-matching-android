# Augmented Reality Template Matching using OpenCV 4 for Android
## Approach: Feature Matching - Brute-Force Matching with ORB Descriptors
[![Android CI](https://github.com/michaeltroger/feature-matching-android/actions/workflows/android.yml/badge.svg)](https://github.com/michaeltroger/feature-matching-android/actions/workflows/android.yml)

Attention: This app was created in 2016. I was a beginner to Android development and Computer Vision back then. So don't expect a perfect code please. Over the years I updated the dependencies and converted it to Kotlin, while the business logic remained unchanged.

Note: Originally I targeted min SDK 15 (Android 4), more architectures ("mips", "mips64", "armeabi") and OpenCV 3 with this project. Nowadays the repo uses newer versions. If you need to support older devices, then you can look back in the repo's Git history (app version 1.1 / Git tag 2)

<img src="/screenshots/demo.gif" alt="" width="800"/>
Copyright of the logo: The Coca-Cola Company

### What is this repository for? ###
* Uses the camera image to search for a specified template image within it via a feature matching approach using the OpenCV library. The detected object is marked with lines within the scene. This can be used to e.g. find a logo.
* More computer vision projects at https://michaeltroger.com/computervision/

### How do I get set up? ###
* IDE: Android Studio  (tested with 2023.3.1)
* Android SDK
* Template image location: res/drawable (chooseable in MainActivity)

### Template image ###
Used default template image:

<img src="/app/src/main/res/drawable/coca_cola.bmp" alt="" width="200" />

Copyright of the logo: The Coca-Cola Company

### Author ###
[Michael Troger](https://michaeltroger.com)

### Credits
* The feature matching is based on this official OpenCV tutorial http://docs.opencv.org/2.4/doc/tutorials/features2d/feature_homography/feature_homography.html Unlike in this application their version is using OpenCV 2, C++ and is for use with regular static images
