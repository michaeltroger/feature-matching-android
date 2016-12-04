package com.michaeltroger.arKeySequence;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * detection of keys with template image
 */
public class MainActivity extends Activity implements CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    /**
     * class name for debugging with logcat
     */
    private static final String TAG = MainActivity.class.getSimpleName();
    /**
     * if enabled key labels and borders of object are drawn on top of camera image
     */
    private static final boolean DEBUG_MODE = true;
    /**
     * The template image to use
     */
    private static final int TEMPLATE_IMAGE = R.drawable.phone;
    /**
     * the database of the keys in correct order
     */
    private static final char[] KEY_DATABASE = {
            '1','2','3',
            '4','5','6',
            '7','8','9',
            '*','0','#'
    };
    /**
     * frame size width
     */
    private static final int FRAME_SIZE_WIDTH = 640;
    /**
     * frame size height
     */
    private static final int FRAME_SIZE_HEIGHT = 480;
    /**
     * whether or not to use a fixed frame size -> results usually in higher FPS
     * 640 x 480
     */
    private static final boolean FIXED_FRAME_SIZE = true;

    /**
     * for detecting finishing other activity (settings)
     */
    private static final int RESULT_SETTINGS = 1;
    /**
     * the key sequence to dial
     */
    private String keyOrder;
    /**
     * after how many milliseconds the next key should be pressed
     */
    private int keyDisplayDuration;
    /**
     * the current index within the keyOrder sequence
     */
    private int keyIndex = 0;
    /**
     * whether or not the keys have been detected
     */
    private boolean detectedKeys = false;
    /**
     * flag which shows if changing key (waiting for some seconds) is in work
     */
    private boolean timerRunning = false;

    /**
     * The ORB feature detector
     */
    private FeatureDetector fd;
    /**
     * the ORB descriptor extractor
     */
    private DescriptorExtractor de;
    /**
     * the BRUTEFORCE HAMMINGLUT descriptor matcher
     */
    private DescriptorMatcher dm;
    /**
     * the keypoints of the template image
     */
    private MatOfKeyPoint keyPointsTemplate;
    /**
     * the keypoints of the camera image
     */
    private MatOfKeyPoint keyPointsCamera;
    /**
     * the descriptors of the template image
     */
    private Mat descriptorsTemplate;
    /**
     * the descriptors of the camera image
     */
    private Mat descriptorsCamera;

    /**
     * the camera frame in gray-scale
     */
    private Mat mGr;
    /**
     * the camera frame in RGB
     */
    private Mat mRgb;
    /**
     * the augmented output frame for the camera
     */
    private Mat output;

    /**
     * the template image used for template matching
     * in gray-scale
     */
    private Mat templGray;
    /**
     * template image in color
     */
    private Mat templColor;
    /**
     * the matches between template and target image got from DescriptorMatcher
     */
    private MatOfDMatch matches;

    /**
     * the corners of the template image
     */
    private Mat cornersTemplate;
    /**
     * the corners of the recognized object in the camera image
     */
    private Mat cornersCamera;
    /**
     * possible corners detected in camera image
     */
    private Mat candidateCornersCamera;
    /**
     * the good detected corners recognized in camera image as integers
     */
    private MatOfPoint intCornersCamera;

    /**
     * the sound of a bear
     */
    private MediaPlayer bearSound;
    /**
     * the coordinates of the keys within template image
     */
    private Mat keys;
    /**
     * the calculated transformed coordinates of the keys within the camera image
     */
    private Mat transformedKeys;

    /**
     * the color of the key labels
     */
    private static final Scalar KEY_COLOR = new Scalar(0, 255, 0);

    /**
     * color of the marking label
     */
    private static final Scalar MARKED_KEY_COLOR = new Scalar(255, 0, 0);
    /**
     * the font face used for labels
     */
    private static final int FONT_FACE = Core.FONT_HERSHEY_SIMPLEX;
    /**
     * the scale/size of text used for labels
     */
    private static final double SCALE = 1;//0.4;
    /**
     * the thickness of text used for labels
     */
    private static final int THICKNESS = 3;//1;
    /**
     * whether or not to play sound
     */
    private boolean playSound;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    fd = FeatureDetector.create(FeatureDetector.ORB);
                    de = DescriptorExtractor.create(DescriptorExtractor.ORB);
                    dm = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

                    keyPointsTemplate = new MatOfKeyPoint();
                    keyPointsCamera = new MatOfKeyPoint();
                    descriptorsCamera = new Mat();
                    descriptorsTemplate = new Mat();
                    matches = new MatOfDMatch();

                    // load the specified image from file system in bgr color
                    Mat bgr = null;
                    try {
                        bgr = Utils.loadResource(getApplicationContext(), TEMPLATE_IMAGE, Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // convert the loaded image to gray-scale and color
                    templGray = new Mat();
                    templColor = new Mat();
                    Imgproc.cvtColor(bgr, templGray, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.cvtColor(bgr, templColor, Imgproc.COLOR_BGR2RGBA);

                    // pre-calculate features and descriptors of template image
                    fd.detect(templGray, keyPointsTemplate);
                    de.compute(templGray, keyPointsTemplate, descriptorsTemplate);

                    // set the corners of the template image
                    cornersTemplate = new Mat(4, 1, CvType.CV_32FC2);
                    cornersTemplate.put(0, 0, 0, 0);
                    cornersTemplate.put(1, 0, templGray.cols(), 0);
                    cornersTemplate.put(2, 0, templGray.cols(), templGray.rows());
                    cornersTemplate.put(3, 0, 0, templGray.rows());

                    // init matrices who hold detected corners later
                    cornersCamera = new Mat(4, 1, CvType.CV_32FC2);
                    candidateCornersCamera  = new Mat(4, 1, CvType.CV_32FC2);
                    intCornersCamera = new MatOfPoint();

                    // the coordinates of the keys within the image
                    keys = new Mat(KEY_DATABASE.length, 1, CvType.CV_32FC2);
                    keys.put(0,0, 70, 80);
                    keys.put(1,0, 260, 80);
                    keys.put(2,0, 470, 80);

                    keys.put(3,0, 70, 200);
                    keys.put(4,0, 260, 200);
                    keys.put(5,0, 470, 200);

                    keys.put(6,0, 70, 350);
                    keys.put(7,0, 260, 350);
                    keys.put(8,0, 470, 350);

                    keys.put(9,0, 70, 500);
                    keys.put(10,0, 260, 500);
                    keys.put(11,0, 470, 500);

                    transformedKeys = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT);
        }
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        bearSound = MediaPlayer.create(this, R.raw.bear);
        loadSettings();
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.preferences:
            {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, RESULT_SETTINGS);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * loads user settings which are customizable via gui
     */
    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        keyOrder = prefs.getString("pref_key_key_sequence", "41538");
        keyDisplayDuration = Integer.parseInt(prefs.getString("pref_key_duration", "3000"));
        playSound = prefs.getBoolean("pref_key_sound", true);
        keyIndex = 0;
        Log.d(TAG, "key sequence: " + keyOrder);
        Log.d(TAG, "key duration: " + keyDisplayDuration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                loadSettings();
                break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mGr = inputFrame.gray();
        mRgb = inputFrame.rgba();
        output = mRgb.clone();

        // do the key point matching
        keypointMatching();

        // display keys and the to do key sequence
        displayKeys();

        return output;
    }

    /**
     * responsible for keypoint matching - searches for template images within scene
     */
    private void keypointMatching() {
        fd.detect(mRgb, keyPointsCamera);

        if (!keyPointsCamera.empty()) {
            de.compute(mRgb, keyPointsCamera, descriptorsCamera);
            if (!descriptorsCamera.empty()) {
                dm.match(descriptorsTemplate, descriptorsCamera, matches);

                cornersCamera.create(0, 0, cornersCamera.type());
                List<DMatch> matchesList = matches.toList();
                if (matchesList.size() >= 4) {
                    //float maxDist = 0;
                    float minDist = 100;

                    List<KeyPoint> keyPointsTemplateList  = keyPointsTemplate.toList();
                    List<KeyPoint> keyPointsCameraList  = keyPointsCamera.toList();
                    ArrayList<Point> goodTemplatePointsList = new ArrayList<>();
                    ArrayList<Point> goodCameraPointsList = new ArrayList<>();

                    double maxGoodMatchDist = 3 * minDist;

                    for (DMatch match : matchesList) {
                        if (match.distance < maxGoodMatchDist) {
                            goodTemplatePointsList.add(
                                    keyPointsTemplateList.get( match.queryIdx ).pt
                            );
                            goodCameraPointsList.add(
                                    keyPointsCameraList.get( match.trainIdx ).pt
                            );
                        }

                    }

                    MatOfPoint2f goodTemplatePoints = new MatOfPoint2f();
                    goodTemplatePoints.fromList(goodTemplatePointsList);
                    MatOfPoint2f goodCameraPoints = new MatOfPoint2f();
                    goodCameraPoints.fromList(goodCameraPointsList);

                    Mat homography = Calib3d.findHomography(goodTemplatePoints, goodCameraPoints, Calib3d.RANSAC, 10);

                    if (!homography.empty()) {
                        Core.perspectiveTransform(cornersTemplate, cornersCamera, homography);
                        Core.perspectiveTransform(keys, transformedKeys, homography);

                        cornersCamera.convertTo(intCornersCamera, CvType.CV_32S);
                        Rect bb = Imgproc.boundingRect(intCornersCamera);
                        if (Imgproc.isContourConvex(intCornersCamera) && bb.width > 100 && bb.width < 1000) {
                            if (cornersCamera.height() >= 4) {
                                // draw lines around detected object on top of camera image
                                if (DEBUG_MODE) {
                                    Imgproc.line(output,
                                            new Point(cornersCamera.get(0, 0)),
                                            new Point(cornersCamera.get(1, 0)),
                                            KEY_COLOR,
                                            4);
                                    Imgproc.line(output,
                                            new Point(cornersCamera.get(1, 0)),
                                            new Point(cornersCamera.get(2, 0)),
                                            KEY_COLOR,
                                            4);
                                    Imgproc.line(output,
                                            new Point(cornersCamera.get(2, 0)),
                                            new Point(cornersCamera.get(3, 0)),
                                            KEY_COLOR,
                                            4);
                                    Imgproc.line(output,
                                            new Point(cornersCamera.get(3, 0)),
                                            new Point(cornersCamera.get(0, 0)),
                                            KEY_COLOR,
                                            4);
                                }
                            }
                            detectedKeys = transformedKeys.height() >= 12;
                        }
                    }
                }

            } else {
                Log.e(TAG, "no descriptors in camera scene");
            }
        } else {
            Log.e(TAG, "no keypoints in camera scene");
        }
    }

    /**
     * display labels of keys augmented and display key sequence
     */
    private void displayKeys() {
       if (detectedKeys) {
           if (DEBUG_MODE) {
               for (int i = 0; i < KEY_DATABASE.length; i++) {
                   Imgproc.putText(
                           output,
                           String.valueOf(KEY_DATABASE[i]),
                           new Point(transformedKeys.get(i, 0)),
                           FONT_FACE,
                           SCALE,
                           KEY_COLOR,
                           THICKNESS
                   );
               }
           }
           if (!timerRunning) {
               this.runOnUiThread(new Runnable() {
                   public void run() {
                       timerRunning = true;
                       Handler handler = new Handler();
                       handler.postDelayed(new Runnable() {
                           public void run() {
                               //Log.d(TAG, "timer finished");
                                if (keyIndex < keyOrder.length() -1) {
                                    keyIndex++;
                                } else {
                                    keyIndex = 0;
                                }
                                timerRunning = false;

                                if (playSound) {
                                    bearSound.start();
                                }
                           }
                       }, keyDisplayDuration);
                   }
               });
           }

           if (keyIndex < keyOrder.length()) {
               char numberAsChar = keyOrder.charAt(keyIndex);

               int indexOfKey = -1;
               for (int i = 0; i < KEY_DATABASE.length; i++) {
                   if (KEY_DATABASE[i] == numberAsChar) {
                       //Log.d(TAG, "found index of key:"+i);
                       indexOfKey = i;
                       break;
                   }
               }

               if (indexOfKey != -1) {
                   //int number = (int) numberAsChar - 48;
                   //Log.d(TAG, "current number:"+number);
                   Imgproc.putText(
                           output,
                           "X",
                           new Point(transformedKeys.get(indexOfKey, 0)),
                           FONT_FACE,
                           SCALE,
                           MARKED_KEY_COLOR,
                           THICKNESS*2
                   );
               }
           }
       }
       detectedKeys = false;
    }

}