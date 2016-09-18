# Augmented Reality Key Sequence for >= Android 4.0 #

### What is this repository for? ###
* an augmented reality key sequence is shown when specific phone buttons are found within real time camera picture. A feature matching approach is used, which means that buttons are not recognized separately but with one template image including several buttons. The interval and the key sequence can be chosen via the GUI. Also playing a sound when a button shall be pressed can be enabled/disabled there.
* the optional debug mode shows the label of the buttons on top of the camera image and draws also a border around the detected area.
* Version 1.0

### How do I get set up? ###
* IDE: Android Studio 1.5.1
* Android SDK
* Dependencies: OpenCV 3.0.0
* Mode (debug/release): Flag in MainActivity
* Configuration key labels: a matrix defined in MainActivity is used to draw the labels for the buttons
* Configuration key location: a matrix defined in MainActivity is responsible for setting the position of each button within the template image
* Template image location: res/drawable | Sound files location: res/raw

### Who do I talk to? ###
* Repo owner and developer: michael.troger@student.pxl.be
