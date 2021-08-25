# Augmented Reality Template Matching for >= Android 4.0
## Approach: Feature Matching - Brute-Force Matching with ORB Descriptors
Attention: This app was created in 2016. I was a beginner to Android development and Computer Vision back then. So don't expect a perfect code please. In 2021 I updated the project to build with the latest Android Studio (2020.3.1), updated most dependencies and converted it to Kotlin, while the business logic remained unchanged.

<img src="/tbd.jpg" alt="" width="800"/>

### What is this repository for? ###
* Uses the camera image to search for a specified template image within it via a feature matching approach using the OpenCV library. The detected object is marked with lines within the scene. This can be used to e.g. find a logo.

### How do I get set up? ###
* IDE: Android Studio (tested with 2020.3.1)
* Android SDK
* Dependencies: OpenCV 3 library (included) [License](/opencv-3-4-15/LICENSE)
* Template image location: res/drawable (chooseable in MainActivity)

### Template image ###
Used default template image:

<img src="/app/src/main/res/drawable/coca_cola.bmp" alt="" width="200" />

Copyright of the logo: The Coca-Cola Company

### Who do I talk to? ###
* Repo owner and developer: android@michaeltroger.com

### Credits
* The feature matching is based on this official OpenCV tutorial http://docs.opencv.org/2.4/doc/tutorials/features2d/feature_homography/feature_homography.html Unlike in this application their version is using OpenCV 2, C++ and is for use with regular static images
