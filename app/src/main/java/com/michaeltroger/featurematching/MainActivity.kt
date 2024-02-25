package com.michaeltroger.featurematching

import android.Manifest
import android.content.pm.PackageManager
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.features2d.DescriptorMatcher
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.SurfaceView
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.opencv.android.*
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.ORB
import java.io.IOException
import java.util.ArrayList

class MainActivity : ComponentActivity(), CvCameraViewListener2 {
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    /**
     * ORB feature detector * ORB descriptor extractor
     */
    private var de: ORB? = null

    /**
     * the BRUTEFORCE HAMMINGLUT descriptor matcher
     */
    private var dm: DescriptorMatcher? = null

    /**
     * the keypoints of the template image
     */
    private var keyPointsTemplate: MatOfKeyPoint? = null

    /**
     * the keypoints of the camera image
     */
    private var keyPointsCamera: MatOfKeyPoint? = null

    /**
     * the descriptors of the template image
     */
    private var descriptorsTemplate: Mat? = null

    /**
     * the descriptors of the camera image
     */
    private var descriptorsCamera: Mat? = null

    /**
     * the camera frame in gray-scale
     */
    private var mGr: Mat? = null

    /**
     * the camera frame in RGB
     */
    private var mRgb: Mat? = null

    /**
     * the augmented output frame for the camera
     */
    private var output: Mat? = null

    /**
     * the template image used for template matching
     * in gray-scale
     */
    private var templGray: Mat? = null

    /**
     * template image in color
     */
    private var templColor: Mat? = null

    /**
     * the matches between template and target image got from DescriptorMatcher
     */
    private var matches: MatOfDMatch? = null

    /**
     * the corners of the template image
     */
    private var cornersTemplate: Mat? = null

    /**
     * the corners of the recognized object in the camera image
     */
    private var cornersCamera: Mat? = null

    /**
     * the good detected corners recognized in camera image as integers
     */
    private var intCornersCamera: MatOfPoint? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                checkPermissonAndInitialize()
            }
        }

    private fun checkPermissonAndInitialize() {
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun onPermissionGranted() {
        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView!!.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT)
        }
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mOpenCvCameraView!!.setCameraPermissionGranted()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        mOpenCvCameraView =
            findViewById<View>(R.id.tutorial1_activity_java_surface_view) as CameraBridgeViewBase

        checkPermissonAndInitialize()
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    public override fun onResume() {
        super.onResume()
        OpenCVLoader.initLocal()

        Log.i(TAG, "OpenCV loaded successfully")
        mOpenCvCameraView!!.enableView()
        de = ORB.create()
        dm = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT)
        keyPointsTemplate = MatOfKeyPoint()
        keyPointsCamera = MatOfKeyPoint()
        descriptorsCamera = Mat()
        descriptorsTemplate = Mat()
        matches = MatOfDMatch()

        // load the specified image from file system in bgr color
        var bgr: Mat? = null
        try {
            bgr = Utils.loadResource(
                applicationContext,
                TEMPLATE_IMAGE,
                Imgcodecs.IMREAD_COLOR
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // convert the loaded image to gray-scale and color
        templGray = Mat()
        templColor = Mat()
        Imgproc.cvtColor(bgr, templGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(bgr, templColor, Imgproc.COLOR_BGR2RGBA)

        // pre-calculate features and descriptors of template image
        de!!.detect(templGray, keyPointsTemplate)
        de!!.compute(templGray, keyPointsTemplate, descriptorsTemplate)

        // set the corners of the template image
        cornersTemplate = Mat(4, 1, CvType.CV_32FC2)
        cornersTemplate!!.put(0, 0, 0.0, 0.0)
        cornersTemplate!!.put(1, 0, templGray!!.cols().toDouble(), 0.0)
        cornersTemplate!!.put(
            2,
            0,
            templGray!!.cols().toDouble(),
            templGray!!.rows().toDouble()
        )
        cornersTemplate!!.put(3, 0, 0.0, templGray!!.rows().toDouble())

        // init matrices who hold detected corners later
        cornersCamera = Mat(4, 1, CvType.CV_32FC2)
        intCornersCamera = MatOfPoint()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}
    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        mGr = inputFrame.gray()
        mRgb = inputFrame.rgba()
        output = mRgb!!.clone()

        // do the key point matching
        keypointMatching()
        return output!!
    }

    /**
     * responsible for keypoint matching - searches for template images within scene
     */
    private fun keypointMatching() {
        de!!.detect(mRgb, keyPointsCamera)
        if (keyPointsCamera!!.empty()) {
            Log.e(TAG, "no keypoints in camera scene")
            return
        }
        de!!.compute(mRgb, keyPointsCamera, descriptorsCamera)
        if (descriptorsCamera!!.empty()) {
            Log.e(TAG, "no descriptors in camera scene")
            return
        }
        dm!!.match(descriptorsTemplate, descriptorsCamera, matches)
        cornersCamera!!.create(0, 0, cornersCamera!!.type())
        val matchesList = matches!!.toList()
        if (matchesList.size < 4) {
            return
        }
        //float maxDist = 0;
        val minDist = 100f
        val keyPointsTemplateList = keyPointsTemplate!!.toList()
        val keyPointsCameraList = keyPointsCamera!!.toList()
        val goodTemplatePointsList = ArrayList<Point>()
        val goodCameraPointsList = ArrayList<Point>()
        val maxGoodMatchDist = (3 * minDist).toDouble()
        for (match in matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodTemplatePointsList.add(
                    keyPointsTemplateList[match.queryIdx].pt
                )
                goodCameraPointsList.add(
                    keyPointsCameraList[match.trainIdx].pt
                )
            }
        }
        val goodTemplatePoints = MatOfPoint2f()
        goodTemplatePoints.fromList(goodTemplatePointsList)
        val goodCameraPoints = MatOfPoint2f()
        goodCameraPoints.fromList(goodCameraPointsList)
        val homography = Calib3d.findHomography(
            goodTemplatePoints,
            goodCameraPoints,
            Calib3d.RANSAC,
            10.0
        )
        if (homography.empty()) {
            return
        }
        Core.perspectiveTransform(cornersTemplate, cornersCamera, homography)
        cornersCamera!!.convertTo(intCornersCamera, CvType.CV_32S)
        val bb = Imgproc.boundingRect(intCornersCamera)
        if (Imgproc.isContourConvex(intCornersCamera) && bb.width > 100 && bb.width < 1000 && cornersCamera!!.height() >= 4) {
            // draw lines around detected object on top of camera image
            Imgproc.line(
                output,
                Point(cornersCamera!![0, 0]),
                Point(cornersCamera!![1, 0]),
                KEY_COLOR,
                4
            )
            Imgproc.line(
                output,
                Point(cornersCamera!![1, 0]),
                Point(cornersCamera!![2, 0]),
                KEY_COLOR,
                4
            )
            Imgproc.line(
                output,
                Point(cornersCamera!![2, 0]),
                Point(cornersCamera!![3, 0]),
                KEY_COLOR,
                4
            )
            Imgproc.line(
                output,
                Point(cornersCamera!![3, 0]),
                Point(cornersCamera!![0, 0]),
                KEY_COLOR,
                4
            )
        }
    }

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = MainActivity::class.java.simpleName

        /**
         * The template image to use
         */
        private val TEMPLATE_IMAGE = R.drawable.coca_cola

        /**
         * frame size width
         */
        private const val FRAME_SIZE_WIDTH = 640

        /**
         * frame size height
         */
        private const val FRAME_SIZE_HEIGHT = 480

        /**
         * whether or not to use a fixed frame size -> results usually in higher FPS
         * 640 x 480
         */
        private const val FIXED_FRAME_SIZE = true

        /**
         * the color of the key labels
         */
        private val KEY_COLOR = Scalar(0.0, 255.0, 0.0)
    }

    init {
        Log.i(TAG, "Instantiated new " + this.javaClass)
    }
}