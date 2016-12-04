# Augmented Reality Key Sequence for >= Android 4.0 #

<img src="/arkeysequence.jpg" alt="" width="400"/>

### What is this repository for? ###
* an augmented reality key sequence is shown when specific phone buttons (Alcatel 4028) are found within the real time camera picture. A feature matching approach is used, which means that buttons are not recognized separately but with one template image representing several buttons. The interval and the key sequence can be chosen via the GUI. Also playing a sound when a button shall be pressed can be enabled/disabled there.
* the optional debug mode shows the label of the buttons on top of the camera image and draws also a border around the detected area.
* the template image and the hardcoded key database is based on the business phone Alcatel-Lucent phone IP Touch 4028 extended edition  
* Version 1.0

### How do I get set up? ###
* IDE: Android Studio (tested with 2.2.2)
* Android SDK
* Dependencies: OpenCV 3.0.0 library (included)
* Mode (debug/release): Flag in MainActivity
* Configuration key labels: a matrix defined in MainActivity is used to draw the labels for the buttons
* Configuration key location: a matrix defined in MainActivity is responsible for setting the position of each button within the template image
* Template image location: res/drawable (chooseable in MainActivity) | Sound files location: res/raw
* Make sure the app has the required permission on start, as there is no runtime-check yet! (Camera)

### Template image ###
<img src="/app/src/main/res/drawable/phone.jpg" alt="" width="200" />

### Test image ###
Depending on your display settings it's might not possible to detect the keys on your screen. Printing the image should work better if you don't have the Alcatel hardware.

<img src="/testimages/alcatel4028.jpg" alt="" width="600"/>

### Who do I talk to? ###
* Repo owner and developer: android@michaeltroger.com

### Credits
* The feature matching is based on this official OpenCV tutorial http://docs.opencv.org/2.4/doc/tutorials/features2d/feature_homography/feature_homography.html Unlike in this application their version is using OpenCV 2, C++ and is for use with normal images
