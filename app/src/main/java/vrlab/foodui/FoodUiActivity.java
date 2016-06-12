package vrlab.foodui;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import vrlab.foodui.database.DbAccessFruitTypes;
import vrlab.foodui.detectableObjects.Fruit;
import vrlab.foodui.detectableObjects.FruitType;
import vrlab.foodui.information.NutritionalValue;




//////////////////////////////////////////// END IMPORT ////////////////////////////////////////////




public class FoodUiActivity
        extends Activity
        implements CvCameraViewListener2,
        View.OnClickListener,
        View.OnTouchListener {

    // load opencv library
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("foodui");
    }




    //// global constant section
    // constants for development purposes only
    private static final boolean DEBUGGING = true; //deactives the long hsv mode information toast
    private static final boolean TEST_ALL_COMPARISON_METHODS = false;
                                                //activates use of testHistogramComparisonMethods

    // constants for clearer mode operations
    private static final int VIEW_MODE_RGBA = 0;
    private static final int VIEW_MODE_CANNY = 1;
    private static final int VIEW_MODE_FEATURES = 2;
    private static final int VIEW_MODE_HSV_FILTER = 3;

    // constants regarding tags for debugging purposes
    private static final String TAG = "FoodUI::FoodUiActivity";
    private static final String TAG_BUTTON_NORMAL = "modeDetection";
    private static final String TAG_BUTTON_EDGE = "modeEdge";
    private static final String TAG_BUTTON_FEATURES = "modeFeatures";
    private static final String TAG_BUTTON_HSV_FILTER = "modeHsvFilter";
    private static final String HIST_COMP_TESTS = "HistCompTests";
    private static final String DEBUG = "Debug";

    // constants regarding menu operations
    private static final int MAX_DETECTOR_MODE = 3;

    // histogram related settings
    private static final int HISTOGRAM_SIZE = 256;  // size of histogram (value pairs) [max=256]
    private static final double HISTOGRAM_AUTOFIT_THRESHOLD = 0.07; // min value for autofit borders
    private static final int HISTOGRAM_COMPARISON_METHOD = Imgproc.CV_COMP_CORREL;
                                                            // used method for histogram comparison
    // histogram drawing related settings
    final int BUFFER_BETWEEN_HISTOGRAMS = 20;
    final int AMOUNT_HISTOGRAMS = 2 + 1;                 // one more to save space for the buttons

    // defines, which histograms should be compared - choose ONE
    private static final boolean HISTOGRAM_COMPARISON_HUE = false;
    private static final boolean HISTOGRAM_COMPARISON_SATURATION = false;
    private static final boolean HISTOGRAM_COMPARISON_HS = true;

    // threshold for comparison results
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_HUE_MIN = 0.85;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_HUE_MAX = 1;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MIN = 0.75;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MAX = 1;

    // constants for contour relating settings
    private static final boolean isOnlyBiggestContourDesired = true;
    private static final int CONTOUR_THRESHOLD_BOTTOM = 15000;   // threshold for found contours
    private static final int CONTOUR_THRESHOLD_TOP = 300000;     // threshold for found contours
    private static final double APPROX_DISTANCE = 5;     // distance between points of contour line

    // constants relating tracking operations
    private static final int FRUIT_TRACKER_FRAME_RANGE = 10;
                                                    // amount of frames to consider in tracking

    // constants related to the dynamically evenly generated hue scalar
    private static final int AMOUNT_PHASES = 5;
    private static final int STEPS_PER_PHASE = HISTOGRAM_SIZE /AMOUNT_PHASES;

    // constants related to morphing operations
    private static final Size ERODE_SIZE = new Size(3, 3);
    private static final Size DILATE_SIZE = new Size(5, 5);

    // fruit label and information display related settings
    private static final int SPACE_BUFFER_TEXT_FRONT = 5;
    private static final int SPACE_BUFFER_TEXT_END = SPACE_BUFFER_TEXT_FRONT;
    private static final int LABEL_LINE_1_HEIGHT = 30;
    private static final int LABEL_LINE_1_WIDTH = 30;
    private static final int LABEL_LINE_1_THICKNESS = 5;
    private static final int LABEL_LINE_2_THICKNESS = 3;
    private static final int LABEL_LINE_1_DIG_IN_X = 8;  // dig line into object, if no contour is applied (x)
    private static final int LABEL_LINE_1_DIG_IN_Y = 0;  // as above, (y)
    private static final int TEXT_FONT_FACE = 3;
    private static final int TEXT_FONT_SCALE = 1;
    private static final int TEXT_THICKNESS = 2;
    private static final int TEXT_LINE_TYPE = 0;
    private static final int TEXT_MARGIN = 3 + LABEL_LINE_2_THICKNESS;




    //// global variable section
    // opencv specific variables
    private CameraBridgeViewBase mOpenCvCameraView;
    private int mViewMode;


    // various general mats
    private Mat mRgba;
    private Mat mHsvOriginal;
    private Mat mGray;
    private Mat mIntermediateMat;
    private Mat mRgbaMask;

    // variables for hsv mode (including histogram operations)
    private Mat mHsv;
    private Mat mHsvThreshed;
    private List<Mat> hsvHistograms;          // saves histograms for hue and saturation
    private Mat mMat0;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private MatOfFloat mRanges;
    private Scalar mColorsHue[];
    private Scalar mWhite;

    // control variables
    private int detectorChoice = MAX_DETECTOR_MODE;
    private boolean freezeCamera;    // freeze camera, e.g. during input operations

    //// Tracking Targets
    // vector for user defined filter values: hMin, sMin, vMin, hMax, sMax, vMax
    // initialized with values for a good detection of a banana
    private int[] mHsvFilterValues = new int[]{0, 36, 141, 44, 188, 255};

    // objects for fruit and fruit type handling
    private List<FruitType> fruitTypes = new ArrayList<>();
    private List<Fruit> detectedFruits = new ArrayList<>();
    private Fruit detectedFruit;

    // objects for tracking of detected fruits --> using for remembering past states
    private List<LinkedList<Fruit>> fruitTracker = new ArrayList<LinkedList<Fruit>>();
    private DbAccessFruitTypes dbFruitTypes;


    /////////////////////////////////// END INITIALIZATION /////////////////////////////////////////



    /**
     * empty standard constructor
     *
     */
    public FoodUiActivity() {}


    /**
     * executes various initialization operations
     * - initializes camera view from opencv
     * - initializes fruit type list with data from database
     * - initializes user interface
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        //// initialize variables
        // initialize opencv variables
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_main_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        // initialize existing fruitTypes from database
        initializeFromDatabase();

        // build seekbars and disable  them, since hsv mode is not default
        buildRangeSeekBars();

        // build floating button and menu
        buildFab();
    }


    /**
     * cleanup operations, if app is paused
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    /**
     * cleanup operations, if app is resumed
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setOnTouchListener(FoodUiActivity.this);
    }


    /**
     * cleanup operations, if app is resumed
     *
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    /**
     * initializes all mats, scalars and other variables, which are needed for the frame processing
     *
     * @param width     the width of the frames that will be delivered
     * @param height    the height of the frames that will be delivered
     */
    public void onCameraViewStarted(int width, int height) {
        // initialize main mats
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mHsv = new Mat(height, width, CvType.CV_8UC4);
        mHsvOriginal = new Mat(height, width, CvType.CV_8UC4);
        mHsvThreshed = new Mat(height, width, CvType.CV_8UC1);
        mRgbaMask = new Mat(height, width, CvType.CV_8UC3);

        // initialize variables for hsv histograms
        mIntermediateMat = new Mat();
        hsvHistograms = new ArrayList<>(Arrays.asList(
                new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F),
                new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F)));
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mHistSize = new MatOfInt(HISTOGRAM_SIZE);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mWhite = Scalar.all(255);

        // fill hue scalar with as much values as histogram bars (HISTOGRAM_SIZE)
        mColorsHue = createEvenHueScalar(HISTOGRAM_SIZE);

        // initialize other variables
        freezeCamera = false;
    }


    /**
     * cleanup operations, when camera view stops
     *
     */
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();

        mHsv.release();
        mHsvOriginal.release();
        mHsvThreshed.release();
        mRgbaMask.release();
    }


    /**
     * starting point for every frame, arranges steps depending on the active mode
     *
     * @param inputFrame        contains data from the live frame of the camera
     * @return                  returns rgba frame as a result of the frame handling processes
     *                          to display it in the next step
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int VIEW_MODE = mViewMode;

        if (!freezeCamera) {
            switch (VIEW_MODE) {
                case VIEW_MODE_RGBA:
                    // input frame has RBGA format
                    mRgba = inputFrame.rgba();
                    Imgproc.cvtColor(mRgba, mHsvOriginal, Imgproc.COLOR_RGB2HSV_FULL);

                    // execute fruit detection, if at least one fruit type is saved
                    if (fruitTypes.size() > 0)
                        for (FruitType ft : fruitTypes)
                            detectFruitType(ft);

                    // handle detected fruits and clear the list afterwards
                    if (detectedFruits.size() > 0)
                        for (Fruit f : detectedFruits)
                            handleDetectedFruit(f, false);
                    detectedFruits.clear();

                    break;
                case VIEW_MODE_CANNY:
                    // initialize needed mats
                    mGray = inputFrame.gray();
                    mRgba = inputFrame.rgba();
                    Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
                    Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);

                    break;
                case VIEW_MODE_FEATURES:
                    // initialize needed mats
                    mGray = inputFrame.gray();
                    mRgba = inputFrame.rgba();
                    FoodUiJNI.findFeatures(mGray.getNativeObjAddr(),
                            mRgba.getNativeObjAddr(), detectorChoice);

                    break;
                case VIEW_MODE_HSV_FILTER:
                    // initialize needed mats -> backup mats to be able to reset in loops
                    mRgba = inputFrame.rgba();
                    Imgproc.cvtColor(mRgba, mHsvOriginal, Imgproc.COLOR_RGB2HSV_FULL);

                    // apply hsv filter from range seek bar settings as a mask to the screen
                    // also show color histograms of hue and saturation
                    handleHsvMode();

                    // execute fruit detection, if at least one fruit type is saved
                    if (fruitTypes.size() > 0)
                        for (FruitType ft : fruitTypes)
                            detectFruitType(ft);

                    // handle detected fruits and clear the list afterwards
                    if (detectedFruits.size() > 0)
                        for (Fruit f : detectedFruits)
                            handleDetectedFruit(f, true);
                    detectedFruits.clear();

                    break;
            }
        }
        return mRgba;
    }


    /**
     * handle onClick events, such as the manipulation of hsv filter values
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        // variable declaration
        Context context = getApplicationContext();
        String text = "";
        int length = Toast.LENGTH_SHORT;

        // find out, which button was pressed and execute corresponding actions
        if (v.getTag().equals(TAG_BUTTON_HSV_FILTER)) {
            // handle the option 'hsv mode'
            Log.i(TAG, "Activated hsv filter");
            setHsvModeGuiVisibility(true);

            // if it's already activated, ask for input to save current hsv filter values
            if (mViewMode != VIEW_MODE_HSV_FILTER) {
                mViewMode = VIEW_MODE_HSV_FILTER;

                // prepare tutorial text and set toast length to long,
                // since it's fairly a bit of text
                if (!DEBUGGING) {        // skip the introduction, when debugging, since it's long
                    length = Toast.LENGTH_LONG;
                    int lengthHsvIntroduction = 3;
                    text = "HSV filter mode for fruit type storing" +
                            "\nTop left: HSV range filter" +
                            "\nBottom left: Color histograms (hue, saturation)" +
                            "\nPress again the 'hsv filter mode' button to save the fruit!";

                    // show toast multiple times, since TOAST_LONG is still too short
                    for (int i = 0; i < lengthHsvIntroduction; i++) {
                        Toast toast = Toast.makeText(context, text, length);
                        toast.show();
                    }
                }
            } else {
                dialogInputSaveFruitType();
            }
        } else {
            // disable seekbars
            Log.i(TAG, "Activated another mode");
            setHsvModeGuiVisibility(false);

            // reset detectorChoice when another mode is chosen
            // -> initialization with last mode so it starts with first one
            if (!(v.getTag().equals(TAG_BUTTON_FEATURES))) detectorChoice = MAX_DETECTOR_MODE;

            // handle the selection of all modes besides the hsv mode
            if (v.getTag().equals(TAG_BUTTON_NORMAL)) {
                // handle the option 'normal mode'
                Log.i(TAG, "activated rgba mode");
                mViewMode = VIEW_MODE_RGBA;

                // prepare introduction text
                text = "RGB camera mode with active fruit detection.";
            } else if (v.getTag().equals(TAG_BUTTON_EDGE)) {
                // handle the option 'edge mode'
                Log.i(TAG, "activated canny mode");
                mViewMode = VIEW_MODE_CANNY;

                // prepare introduction text
                text = "Explore the edges in your environment!";
            } else if (v.getTag().equals(TAG_BUTTON_FEATURES)) {
                // handle the option 'feature mode'
                Log.i(TAG, "activated feature mode");
                mViewMode = VIEW_MODE_FEATURES;
                detectorChoice = (detectorChoice + 1) % (MAX_DETECTOR_MODE + 1);

                // prepare introduction text and increase length, since it's longer
                text = "Explore features with different detectors!";
                length = Toast.LENGTH_LONG;

                // give feedback to chosen mode
                switch (detectorChoice) {
                    case 0:
                        text += "\nFeature detector: FAST";
                        break;
                    case 1:
                        text += "\nFeature detector: AGAST";
                        break;
                    case 2:
                        text += "\nFeature detector: GFTT";
                        break;
                    case 3:
                        text += "\nFeature detector: SimpleBlobDetector";
                        break;
                    default:
                        text += "\nError in detectorChoice feedback";
                        Log.i(TAG, "Error in detectorChoice feedback");
                        break;
                }
                text += "\nPress again for another detector!";
            } else {
                // handle exceptions
                Log.i(TAG, "error in mode activation");
                mViewMode = VIEW_MODE_RGBA;
            }
            // show prepared toast
            Toast toast = Toast.makeText(context, text, length);
            toast.show();
        }
    }


    /**
     * handle onTouch events (such as displaying nutritional values to a touched fruit)
     *
     * @param arg0      standard transfer variable
     * @param event     standard transfer variable, which contains information about the touch event
     * @return
     */
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        // variable declaration
        double[] coordinates = new double[2];
        Point touchedPoint;

        // define size of mRgba
        double cols = mRgba.cols();// mRgba is your image frame
        double rows = mRgba.rows();

        // determine size of used screen and define offset of mRgba frame
        int width = mOpenCvCameraView.getWidth();
        int height = mOpenCvCameraView.getHeight();
        double scaleFactor = cols / width;
        double xOffset = (width * scaleFactor - cols) / 2;
        double yOffset = (height * scaleFactor - rows) / 2;

        // temporary variable for conversion tasks
        MatOfPoint2f mMOP2f = new MatOfPoint2f();

        // determine coordinates of touched point in mRgba frame and create a Point for the result
        coordinates[0] = (event).getX() * scaleFactor - xOffset;
        coordinates[1] = (event).getY() * scaleFactor - yOffset;
        touchedPoint = new Point(coordinates[0], coordinates[1]);

        //// check depending on detection scale if a touch occured within the contour of a fruit
        // convert contour to needed format
        for(LinkedList<Fruit> currentFruitList : fruitTracker)
        if (currentFruitList.size() > 0) {
            if (currentFruitList.getLast() != null) {
                currentFruitList.getLast().getContour().convertTo(mMOP2f, CvType.CV_32FC2);

                // toggle, that information should be displayed, since a touch
                // occured within the contour
                if (Imgproc.pointPolygonTest(mMOP2f, touchedPoint, false) > 0) {
                    // set, that information is desired, if it's not set - reset, if it's set
                    if (!currentFruitList.getLast().getIsInformationDisplayed())
                        currentFruitList.getLast().setIsInformationDisplayed(true);
                    else
                        currentFruitList.clear();
                }
            }
        }

        return false;       //false: no subsequent events ; true: subsequent events
    }


    ///////////////////////////////// END ACTIVITY CYCLE METHODS ///////////////////////////////////


    /**
     * initialize fruitTypes with saved values from database
     *
     */
    public void initializeFromDatabase() {
        // get database
        dbFruitTypes = new DbAccessFruitTypes(this);

        // get data if table already exists
        if (dbFruitTypes.isTableExisting(dbFruitTypes.TABLE_NAME_FRUIT_TYPES)) {
            // process data
            fruitTypes = dbFruitTypes.getAllData();
        }

        // update fruit tracker
        for(FruitType ft : fruitTypes)
            fruitTracker.add(new LinkedList<Fruit>());
    }


    /**
     * create floating main button and floating menu, which is activated through a click on the
     * main button
     *
     */
    public void buildFab() {
        // create floating button
        final ImageView mainFab = new ImageView(this);
        mainFab.setImageResource(R.drawable.ic_plus);
        final FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(mainFab)
                .setBackgroundDrawable(getResources()
                        .getDrawable(R.drawable.selector_button_gold, getTheme()))
                .build();

        // create menu items
        final ImageView fabModeNormal = new ImageView(this);
        final ImageView fabModeEdge = new ImageView(this);
        final ImageView fabModeFeatures = new ImageView(this);
        final ImageView fabModeHsvFilter = new ImageView(this);
        fabModeNormal.setImageResource(R.drawable.ic_camera);
        fabModeEdge.setImageResource(R.drawable.ic_edge);
        fabModeFeatures.setImageResource(R.drawable.ic_feature);
        fabModeHsvFilter.setImageResource(R.drawable.ic_filter);

        // create menu buttons
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        itemBuilder.setBackgroundDrawable(getResources()
                .getDrawable(R.drawable.selector_button_green, getTheme()));
        SubActionButton buttonFabModeNormal = itemBuilder.setContentView(fabModeNormal).build();
        itemBuilder.setBackgroundDrawable(getResources()
                .getDrawable(R.drawable.selector_button_blue, getTheme()));
        SubActionButton buttonFabModeEdge = itemBuilder.setContentView(fabModeEdge).build();
        itemBuilder.setBackgroundDrawable(getResources()
                .getDrawable(R.drawable.selector_button_red, getTheme()));
        SubActionButton buttonFabModeFeatures = itemBuilder.setContentView(fabModeFeatures).build();
        itemBuilder.setBackgroundDrawable(getResources()
                .getDrawable(R.drawable.selector_button_purple, getTheme()));
        SubActionButton buttonFabModeHsvFilter =
                itemBuilder.setContentView(fabModeHsvFilter).build();

        // set tags
        buttonFabModeNormal.setTag(TAG_BUTTON_NORMAL);
        buttonFabModeEdge.setTag(TAG_BUTTON_EDGE);
        buttonFabModeFeatures.setTag(TAG_BUTTON_FEATURES);
        buttonFabModeHsvFilter.setTag(TAG_BUTTON_HSV_FILTER);
        //  set onClickListener
        buttonFabModeNormal.setOnClickListener(this);
        buttonFabModeEdge.setOnClickListener(this);
        buttonFabModeFeatures.setOnClickListener(this);
        buttonFabModeHsvFilter.setOnClickListener(this);


        // create floating menu
        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonFabModeNormal)
                .addSubActionView(buttonFabModeEdge)
                .addSubActionView(buttonFabModeFeatures)
                .addSubActionView(buttonFabModeHsvFilter)
                .attachTo(actionButton)
                .build();


        // create animation for the main fab, so it becomes an x (for exit) when active
        actionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu menu) {
                // rotate the icon of rightLowerButton 45 degrees clockwise
                mainFab.setRotation(0);
                PropertyValuesHolder propertyValuesHolder =
                        PropertyValuesHolder.ofFloat(View.ROTATION, 45);
                ObjectAnimator animation =
                        ObjectAnimator.ofPropertyValuesHolder(mainFab, propertyValuesHolder);
                animation.start();
            }

            @Override
            public void onMenuClosed(FloatingActionMenu menu) {
                // rotate the icon of rightLowerButton 45 degrees counter-clockwise
                mainFab.setRotation(45);
                PropertyValuesHolder propertyValuesHolder =
                        PropertyValuesHolder.ofFloat(View.ROTATION, 0);
                ObjectAnimator animation =
                        ObjectAnimator.ofPropertyValuesHolder(mainFab, propertyValuesHolder);
                animation.start();
            }
        });
    }


    /**
     * create RangeSeekBars for setting HSV ranges in HSV Filter mode
     *
     */
    public void buildRangeSeekBars() {
        // setup the new range seek bars
        final RangeSeekBar<Integer> rsbHsvFilterH = new RangeSeekBar<>(this);
        final RangeSeekBar<Integer> rsbHsvFilterS = new RangeSeekBar<>(this);
        final RangeSeekBar<Integer> rsbHsvFilterV = new RangeSeekBar<>(this);

        // set the range
        rsbHsvFilterH.setRangeValues(0, 255);
        rsbHsvFilterH.setSelectedMinValue(mHsvFilterValues[0]);
        rsbHsvFilterH.setSelectedMaxValue(mHsvFilterValues[3]);
        rsbHsvFilterS.setRangeValues(0, 255);
        rsbHsvFilterS.setSelectedMinValue(mHsvFilterValues[1]);
        rsbHsvFilterS.setSelectedMaxValue(mHsvFilterValues[4]);
        rsbHsvFilterV.setRangeValues(0, 255);
        rsbHsvFilterV.setSelectedMinValue(mHsvFilterValues[2]);
        rsbHsvFilterV.setSelectedMaxValue(mHsvFilterValues[5]);


        // set onChangeListeners for all three range seek bars
        rsbHsvFilterH.setOnRangeSeekBarChangeListener(
                new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue,
                                                    Integer maxValue) {
                // handle changed range values
                mHsvFilterValues[0] = minValue;
                mHsvFilterValues[3] = maxValue;
            }
        });

        rsbHsvFilterS.setOnRangeSeekBarChangeListener(
                new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue,
                                                    Integer maxValue) {
                // handle changed range values
                mHsvFilterValues[1] = minValue;
                mHsvFilterValues[4] = maxValue;
            }
        });

        rsbHsvFilterV.setOnRangeSeekBarChangeListener(
                new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue,
                                                    Integer maxValue) {
                // handle changed range values
                mHsvFilterValues[2] = minValue;
                mHsvFilterValues[5] = maxValue;
            }

        });

        // add RangeSeekBars to layout
        LinearLayout layout = (LinearLayout) findViewById(R.id.seekbar_placeholder);
        layout.addView(rsbHsvFilterH);
        layout.addView(rsbHsvFilterS);
        layout.addView(rsbHsvFilterV);

        setLinearLayoutVisibility(false, layout);
    }


    /**
     * updates range bar values to the ones defines in the transfer variable
     *
     * @param values        defines values, with which the range bars need to be updated
     */
    public void setRangeBarValues(int[] values) {
        // variable declaration and initialization
        RangeSeekBar<Integer> rsb;
        LinearLayout layout = (LinearLayout) findViewById(R.id.seekbar_placeholder);

        // set values of all rangeseekbars according to given values
        for(int i=0; i < 3; i++) {
            // get a range seek bar
            rsb = (RangeSeekBar<Integer>) layout.getChildAt(i);

            // set the range bar values
            rsb.setSelectedMinValue(values[i]);
            rsb.setSelectedMaxValue(values[i + 3]);

            // update the mHsvFilterValues
            mHsvFilterValues[i] = values[i];
            mHsvFilterValues[i+3] = values[i+3];
        }
    }


    /**
     * function for visibility setting of all seekbars in defined LinearLayout
     *
     * @param visible       determines, if items are made visible or gone (invisible-like)
     * @param layoutParent  determines layout parent item, whose children are made (in-)visible
     */
    public void setLinearLayoutVisibility(boolean visible, LinearLayout layoutParent) {
        int childCount = layoutParent.getChildCount();

        Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),
                android.R.anim.fade_out);
        Animation animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),
                android.R.anim.fade_in);


        if (visible) {
            layoutParent.setAnimation(animFadeIn);
            for (int i = 0; i < childCount; i++) {
                View v = layoutParent.getChildAt(i);
                v.setVisibility(View.VISIBLE);
            }
        } else {
            layoutParent.setAnimation(animFadeOut);
            for (int i = 0; i < childCount; i++) {
                View v = layoutParent.getChildAt(i);
                v.setVisibility(View.GONE);
            }
        }
    }


    /**
     * creates an array of scalars, according to defined size, with an even color range
     * for the visualization of hue vector values
     *
     * @param size      specifies the size of the scalar vector
     */
    public Scalar[] createEvenHueScalar(int size) {
        // variable declaration
        Scalar[] mColorsHue = new Scalar[size];
        double stepSize = 255/(STEPS_PER_PHASE - 1);
        double r = 255, g = 0, b = 0;                   // starting values for the scalar
        double progress = 0;
        double channelValue;


        /*
            * create steady scalar interpolation between
            * How to create a hue scalar:
            * start with 255,0,0 (rgb)
            * phase 1: increase g to max (255)
            * phase 2: decrease r to min (0)
            * phase 3: increase b to max
            * phase 4: increase r to max
            * phase 5: decrease b to min
            * progress: 0...5x255
        */
        for (int i=0; i < size; i++) {

            channelValue = progress % 255;

            if (progress < 255) {
                g = channelValue;
            }
            else if (progress < 255*2 && progress >= 255) {
                if (g < 255) g = 255;
                r -= channelValue;
            }
            else if (progress < 255*3 && progress >= 255*2) {
                if (r > 0) r = 0;
                b = channelValue;
            }
            else if (progress < 255*4 && progress >= 255*3) {
                if (b < 255) b = 255;
                r = channelValue;
            }
            else if (progress < 255*5 && progress >= 255*4) {
                if (r < 255) r = 255;
                b -= channelValue;
                if (i == (size -1)) b = 0;      // safety measures for the last iteration
            }

            // create new scalar with calculated values
            mColorsHue[i] = new Scalar(r, g, b, 255);

            // progress 1 step further
            progress += stepSize;
        }
        return mColorsHue;
    }


    /**
     * handles the hsv mode: applies mask to live screen, depending on the hsv filter values
     * of the range seek bars, also show color histogram of the filtered image
     *
     */
    public void handleHsvMode() {
        // variable declaration
        List<MatOfPoint> contours;
        MatOfPoint biggestContour;

        // copy rgb frame to temporary mat
        mRgba.copyTo(mIntermediateMat);

        // apply hsv filter values to screen
        getHsvFilterMask(mHsvFilterValues);

        // find biggest contour in binary mask from hsv filter mask
        contours = findContoursAboveThreshold(isOnlyBiggestContourDesired);

        // apply biggest contour as a mask to frame, if a contour was found
        if (contours.size() > 0) {
            // get the biggest contour
            biggestContour = contours.get(0);
            Log.e(DEBUG, "Biggest Contour Area: " + Imgproc.contourArea(biggestContour));

            // apply biggest contour as a new mask (with reseted mHsvThreshed) to rgb frame
            Core.setIdentity(mHsvThreshed, new Scalar(0, 0, 0));
            Imgproc.drawContours(mHsvThreshed,
                    new ArrayList<>(Arrays.asList(biggestContour)), 0, mWhite, -1);
            Imgproc.cvtColor(mHsvThreshed, mRgbaMask, Imgproc.COLOR_GRAY2RGBA, 4);
            Core.bitwise_and(mIntermediateMat, mRgbaMask, mIntermediateMat);
            Imgproc.drawContours(mIntermediateMat,
                    new ArrayList<>(Arrays.asList(biggestContour)), 0, mWhite, 1);
        }
        else {
            // apply mask to rgba frame without using any contour information
            Imgproc.cvtColor(mHsvThreshed, mRgbaMask, Imgproc.COLOR_GRAY2RGBA, 4);
            Core.bitwise_and(mIntermediateMat, mRgbaMask, mIntermediateMat);
        }

        // give feedback about histograms of picture in hsv mode (calc them and then draw them)
        drawHistograms(calcHsvHistograms());
        mIntermediateMat.copyTo(mRgba);
    }


    /**
     * executes the whole fruit detection process, shows histograms and mask in hsv mode
     *
     * @param ft         defines fruit type, for which the check occures
     */
    public void detectFruitType(FruitType ft) {
        // variable declaration for temporary saving
        Fruit detectedFruit = null;
        List<MatOfPoint> contours;
        MatOfPoint biggestContour;
        List<Mat> candidateHistograms;
        LinkedList<Fruit> currentFruitList = fruitTracker.get(fruitTypes.indexOf(ft));


        //// handle single detection --> use largest contour of fruit candidates
        // apply hsv filter from currently checked fruit type to screen
        getHsvFilterMask(ft.getHsvFilterValues());

        // find biggest contour in binary mask from hsv filtered frame
        // (uses: mHsvThreshed for contour searching, without modifying it (uses clone))
        contours = findContoursAboveThreshold(isOnlyBiggestContourDesired);

        // reject further actions if there's no contour within the bottom and top threshold
        if (contours.size() > 0) {
            // get the biggest contour
            biggestContour = contours.get(0);

            //// apply the biggest contour as a new mask to reseted mask
            // reset needed mats
            Core.setIdentity(mHsvThreshed, new Scalar(0, 0, 0));
            mRgba.copyTo(mIntermediateMat);

            // draw contour
            Imgproc.drawContours(mHsvThreshed,
                    new ArrayList<>(Arrays.asList(biggestContour)), 0, mWhite, -1);

            // convert to rgba mask and apply mask to rgb frame
            Imgproc.cvtColor(mHsvThreshed, mRgbaMask, Imgproc.COLOR_GRAY2RGBA, 4);
            Core.bitwise_and(mIntermediateMat, mRgbaMask, mIntermediateMat);

            // calculate the histograms from mIntermediateMat
            candidateHistograms = calcHsvHistograms();

            // create new fruit with detected type and accompanying contour and handle it
            if (compareHsHistograms(ft, candidateHistograms)) {
                detectedFruit = new Fruit(ft, biggestContour);

                // information should be displayed, when the detected type appeared in the last
                // few frames (according to FRUIT_TRACKER_FRAME_RANGE)
                if (currentFruitList.size() > 0)
                    for (Fruit f : currentFruitList)
                        if (f != null)
                            if (f.getIsInformationDisplayed() && (f.getType().getName()
                                    .compareToIgnoreCase(ft.getName()) == 0))
                                detectedFruit.setIsInformationDisplayed(true);

                // save found object in tracker to remember the state (fifo list)
                if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                    currentFruitList.add(detectedFruit);
                else {
                    currentFruitList.removeFirst();
                    currentFruitList.add(detectedFruit);
                }

                //save detected fruit
                detectedFruits.add(detectedFruit);

                // clear object of detected fruit, so it's clean for the next frame
                detectedFruit = null;
            } else {
                // put null as an object into the list, so it can act as a counter for frames
                // with abandoned candidates
                if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                    currentFruitList.add(null);
                else {
                    currentFruitList.removeFirst();
                    currentFruitList.add(null);
                }
            }
        } else {
            // put null as an object into the list, so it can act as a counter for frames
            // without detected objects of this fruit type
            if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                currentFruitList.add(null);
            else {
                currentFruitList.removeFirst();
                currentFruitList.add(null);
            }
        }
    }


    /**
     * applies defined filter values to hsv image, copies result to intermediate mat
     * also be done in the background - another result is a binary mask
     * (mask is saved in mHsvThreshed)
     * (modified global variable: mIntermediateMat gets original rgba values)
     * (modified global variable: mHsv gets hsv values from original rgba values)
     * (modified global variable: mHsvThreshed gets mask from hsv image with
     *      applied hsv filter values)
     *
     * @param hsvFilterValues       defines the min/max values for h, s and v filtering
     */
    public void getHsvFilterMask(int[] hsvFilterValues) {
        // create elements for morphing operations
        Mat mErodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, ERODE_SIZE);
        Mat mDilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, DILATE_SIZE);

        // save mRgba value (live screen) temporary in mIntermediateMat, to use the latter here
        mHsvOriginal.copyTo(mHsv);

        // apply hsv filter values to hsv frame
        Core.inRange(mHsv,
                new Scalar(hsvFilterValues[0], hsvFilterValues[1], hsvFilterValues[2]),
                new Scalar(hsvFilterValues[3], hsvFilterValues[4], hsvFilterValues[5]),
                mHsvThreshed);

        //// erode, dilate and blur frame to get rid of interference
        // erode and dilate in each two iterations
        Imgproc.erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        Imgproc.erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        Imgproc.dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
        Imgproc.dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
    }


    /**
     * calculates hsv histograms and feedback graph and puts the feedback graph of the color
     * histograms (hue, saturation) into a copy of mRgba (mIntermediateMat), so it can also
     * be done in the background without affecting the live frame
     * (needs rgba copy in mIntermediateMat, which is converted to mHsv)
     *
     */
    public List<Mat> calcHsvHistograms() {
        // declaration and initialization of variables
        float highestValue;
        float mBuff[] = new float[HISTOGRAM_SIZE];
        Mat hist = new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F);
        List<Mat> resultHistograms = new ArrayList<>();

        // get hsv mat from mat buffer (intermediate mat --> frame with applied mask)
        Imgproc.cvtColor(mIntermediateMat, mHsv, Imgproc.COLOR_RGB2HSV_FULL);

        //// calculate and display histogram for hue
        Imgproc.calcHist(Arrays.asList(mHsv), mChannels[0], mMat0, hist, mHistSize, mRanges);

        //// normalize, so the values are [0..1] and reset the first value, when it's at maximum
        //// (this happens, when the mask is applied, since a lot of area is black)
        Core.normalize(hist, hsvHistograms.get(0), 1, 0, Core.NORM_INF);
        hsvHistograms.get(0).get(0, 0, mBuff);

        // find out highest value
        highestValue = 0;
        for (float d : mBuff)
            if (d > highestValue) highestValue = d;

        // reset first value to zero, if it's the highest value, or trim it to zero
        if (mBuff[0] == highestValue) {
            // reset the freak value (comes from the mask probably)
            mBuff[0] = 0;

            // update histogram and normalize it again
            hsvHistograms.get(0).put(0, 0, mBuff);
            Core.normalize(hsvHistograms.get(0), hsvHistograms.get(0), 1, 0, Core.NORM_INF);
        }

        //// calculate and display histogram for saturation
        Imgproc.calcHist(Arrays.asList(mHsv), mChannels[1], mMat0, hist, mHistSize, mRanges);

        // normalize, so the values are [0..1] and reset the first value, when it's at maximum
        // (this happens, when the mask is applied, since a lot of area is black)
        Core.normalize(hist, hsvHistograms.get(1), 1, 0, Core.NORM_INF);
        hsvHistograms.get(1).get(0, 0, mBuff);

        // find out highest value
        highestValue = 0;
        for (float d : mBuff)
            if (d > highestValue) highestValue = d;

        // reset first value to zero, if it's the highest value, or trim it to zero
        if (mBuff[0] == highestValue) {
            mBuff[0] = 0;       //reset

            // update histogram and normalize it again
            hsvHistograms.get(1).put(0, 0, mBuff);
            Core.normalize(hsvHistograms.get(1), hsvHistograms.get(1), 1, 0, Core.NORM_INF);
        }

        // clone calculated histograms and return them as a result
        for (Mat m : hsvHistograms)
            resultHistograms.add(m.clone());

        return resultHistograms;
    }


    /**
     * draws the two first histograms from a given list
     *
     * @param histograms    defines list containing histograms
     */
    public void drawHistograms(List<Mat> histograms) {
        // constant declaration
        final double BAR_HEIGHT = mRgba.size().height / 16 * 7;

        // declaration and initialization of variables
        int thickness = (mRgba.width() - BUFFER_BETWEEN_HISTOGRAMS * (AMOUNT_HISTOGRAMS + 1)) /
                (HISTOGRAM_SIZE * AMOUNT_HISTOGRAMS);
        if (thickness > 7) thickness = 7;               // limit thickness
        float mBuff[] = new float[HISTOGRAM_SIZE];
        Point mP1 = new Point();
        Point mP2 = new Point();
        Mat hist = new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F);

        // normalize to the desired height of the color histogram bars
        Core.normalize(hsvHistograms.get(0), hist, BAR_HEIGHT, 0, Core.NORM_INF);
        hist.get(0, 0, mBuff);
        for (int h = 0; h < HISTOGRAM_SIZE; h++) {
            mP1.x = mP2.x = BUFFER_BETWEEN_HISTOGRAMS + h * thickness;
            mP1.y = mRgba.height() - 1;
            mP2.y = mP1.y - (int) mBuff[h];
            Imgproc.line(mIntermediateMat, mP1, mP2, mColorsHue[h], thickness);
        }

        // normalize to the desired height of the color histogram bars
        Core.normalize(histograms.get(1), hist, BAR_HEIGHT, 0, Core.NORM_INF);
        hist.get(0, 0, mBuff);
        for (int h = 0; h < HISTOGRAM_SIZE; h++) {
            mP1.x = mP2.x = 2 * BUFFER_BETWEEN_HISTOGRAMS + (HISTOGRAM_SIZE + h) * thickness;
            mP1.y = mRgba.height() - 1;
            mP2.y = mP1.y - (int) mBuff[h];
            Imgproc.line(mIntermediateMat, mP1, mP2, mWhite, thickness);
        }
    }


    /**
     * applies HISTOGRAM_AUTOFIT_THRESHOLD to given histogram
     *
     * @param histograms    histograms, to which autofit should be applied
     * @return              returns a vector of new filter values
     */
    public int[] autofitHistograms(List<Mat> histograms) {
        // declaration and initialization of variables
        float mBuff[] = new float[HISTOGRAM_SIZE];
        int[] hsvFilterValuesAutofit =
                new int[]{0,0,mHsvFilterValues[2],255,255,mHsvFilterValues[5]};
        int i = 0,j = 0, k = 0;

        // handle all available histograms
        for (Mat hist : histograms) {
            // get data from histogram
            hist.get(0, 0, mBuff);

            // apply left border --> set all histogram values to zero,
            // if they're beneath the threshold
            for (i = 0; i < mBuff.length && mBuff[i] < HISTOGRAM_AUTOFIT_THRESHOLD; i++)
                if (mBuff[i] < HISTOGRAM_AUTOFIT_THRESHOLD)
                    mBuff[i] = 0;

            // apply right border --> set all histogram values to zero,
            // if they're beneath the threshold
            for (j = mBuff.length-1; j > 0 && mBuff[j] < HISTOGRAM_AUTOFIT_THRESHOLD; j--)
                if (mBuff[j] < HISTOGRAM_AUTOFIT_THRESHOLD)
                    mBuff[j] = 0;

            // save data to histogram
            hist.put(0, 0, mBuff);

            // save settings, if the histogram consists of more than just zeros
            if (j > 0) {
                hsvFilterValuesAutofit[k] = i;
                hsvFilterValuesAutofit[k + 3] = j;
            }
            else if (j == 0) {
                hsvFilterValuesAutofit[k] = i;
                hsvFilterValuesAutofit[k + 3] = 255;
            }

            k++;
        }

        return hsvFilterValuesAutofit;
    }


    /**
     * find closest match to picture --> hs color histogram comparison with specified method
     *
     * @return      returns the detected FruitType, which is most likely the captured fruit
     */
    public boolean compareHsHistograms(FruitType ft, List<Mat> candidateHistograms) {
        double highestComparisonValueH = 0;
        double highestComparisonValueS = 0;
        boolean isFruitType = false;

        // find closest match to picture --> hs color histogram comparison with chi square method
        // histogram comparison for the currently checked fruit type
        double comparisonValueH = 0;
        double comparisonValueS = 0;

        // check if all histograms are available
        if (ft.getHsvHistogramH() != null && ft.getHsvHistogramS() != null
                && candidateHistograms.get(0) != null && candidateHistograms.get(1) != null) {

            // <debugSection>
            // compare histograms with all available methods and print results in the log
            if (TEST_ALL_COMPARISON_METHODS) {
                String results;
                results = testHistogramComparisonMethods(ft, candidateHistograms);
                Log.e(HIST_COMP_TESTS, results);
            }
            // </debugSection>

            // compare histograms with intersect method
            comparisonValueH = Imgproc.compareHist(ft.getHsvHistogramH(),
                    candidateHistograms.get(0), HISTOGRAM_COMPARISON_METHOD);
            comparisonValueS = Imgproc.compareHist(ft.getHsvHistogramS(),
                    candidateHistograms.get(1), HISTOGRAM_COMPARISON_METHOD);

            if (comparisonValueH > highestComparisonValueH) {
                highestComparisonValueH = comparisonValueH;
            }
            if (comparisonValueS > highestComparisonValueS) {
                highestComparisonValueS = comparisonValueS;
            }

            Log.e(HIST_COMP_TESTS, ft.getName() + " - H: " + comparisonValueH +
                                    " ; S:" + comparisonValueS);
        }

        // apply threshold, depending on the defined scope
        if (HISTOGRAM_COMPARISON_HUE) {
            if (highestComparisonValueH >= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MIN
                    && highestComparisonValueH <= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MAX) {
                isFruitType = true;
            }
        }
        else if (HISTOGRAM_COMPARISON_SATURATION) {
            if (highestComparisonValueS >= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MIN
                    && highestComparisonValueS <= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MAX) {
                isFruitType = true;
            }
        }
        else if (HISTOGRAM_COMPARISON_HS) {
            if (highestComparisonValueH >= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MIN
                    && highestComparisonValueH <= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MAX
                    && highestComparisonValueS >= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MIN
                    && highestComparisonValueS <= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MAX) {
                isFruitType = true;
            }
        }

        return isFruitType;
    }


    /**
     * finds biggest contour in mHsvThreshed (binary mask from hsv threshold filtering) above
     * the defined threshold, or even all contours above this threshold,
     * depending on the parameter isOnlyBiggestDesired
     *
     * @param isOnlyBiggestDesired      defines, if the task is to find the biggest contour or all
     *                                  above the threshold
     * @return                          list of contours with biggest contour only are all above
     *                                  the threshold
     */
    public List<MatOfPoint> findContoursAboveThreshold(boolean isOnlyBiggestDesired) {
        //// prepare variables for getting the contours
        //// out of the binary image (filter threshold result)
        // general variables
        List<MatOfPoint> results = new ArrayList<>();
        Mat temp = new Mat();       // needed, since original mat is modified

        // contour specific variables
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint biggestContour = new MatOfPoint();

        // temporary variable for conversion tasks
        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
        MatOfPoint2f mMOP2f2 = new MatOfPoint2f();

        // apply hsv filter values from each registered fruit type and clone the resulting
        // binary mask into temporary Mat for further processing
        mHsvThreshed.copyTo(temp);

        // find all contours in clone of binary mask
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // security measures for the case of no found contours
        if (contours.size() > 0) {
            // walk through all contours
            for (int i = 0; i < contours.size(); i++) {
                // operations, if only the biggest contour is desired
                if (isOnlyBiggestDesired) {
                    // save index, if the contour is the biggest yet
                    if (i == 0)
                        biggestContour = contours.get(i);
                    else if (Imgproc.contourArea(contours.get(i))
                                > Imgproc.contourArea(biggestContour)
                            && Imgproc.contourArea(contours.get(i)) > CONTOUR_THRESHOLD_BOTTOM
                            && Imgproc.contourArea(contours.get(i)) < CONTOUR_THRESHOLD_TOP)
                        biggestContour = contours.get(i);
                }
                // operations, if multiple contours are desired
                else {
                    if (Imgproc.contourArea(contours.get(i)) > CONTOUR_THRESHOLD_BOTTOM
                            && Imgproc.contourArea(contours.get(i)) < CONTOUR_THRESHOLD_TOP)
                        results.add(contours.get(i));
                }
            }

            //// handle found contours
            if (isOnlyBiggestDesired) {
                // if there was found one above the threshold size: smooth
                // and save the found contour
                if (Imgproc.contourArea(biggestContour) > CONTOUR_THRESHOLD_BOTTOM
                        && Imgproc.contourArea(biggestContour) < CONTOUR_THRESHOLD_TOP) {
                    // convert mats of contour (forth and back) and smooth contour
                    biggestContour.convertTo(mMOP2f1, CvType.CV_32FC2);
                    Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, APPROX_DISTANCE, true);
                    mMOP2f2.convertTo(biggestContour, CvType.CV_32S);
                    results.add(biggestContour);
                }
            }
            else {
                if (results.size()!=0) {
                    //smooth each found contours, if any are found
                    for (MatOfPoint mop : results) {
                        mop.convertTo(mMOP2f1, CvType.CV_32FC2);
                        Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, APPROX_DISTANCE, true);
                        mMOP2f2.convertTo(mop, CvType.CV_32S);
                    }
                }
                else {
                    // no contour found
                    results = null;
                }
            }
        }

        return results;
    }


    /**
     * handle the detected fruit: draw contour, line and label
     *
     * @param f                     fruit, which should be handled
     * @param isContourDesired      determines, if contour should be drawn
     */
    public void handleDetectedFruit(Fruit f, boolean isContourDesired) {
        // variable declaration
        Size textSize;
        int labelLine2Width;

        Point start;
        Point p1;
        Point p2;
        Point locationName;
        Point locationFurtherInformation;

        List<String> furtherInformation = new ArrayList<>();
        String format = "%3.1f";  // width = 3 and 2 digits after the dot
        NutritionalValue nValue;

        // safety measurement
        if (f != null) {
            // variable initialization
            textSize = Imgproc.getTextSize(f.getType().getName(), TEXT_FONT_FACE, TEXT_FONT_SCALE,
                    TEXT_THICKNESS, null);
            labelLine2Width = (int) (textSize.width
                    + SPACE_BUFFER_TEXT_FRONT + SPACE_BUFFER_TEXT_END);
            start =  f.getLocationLabel().clone();

            // draw contour if wished with transfer variable
            if (isContourDesired) {
                List<MatOfPoint> contours = new ArrayList<>(Arrays.asList(f.getContour()));
                Imgproc.drawContours(mRgba, contours, 0, f.getType().getMarkerColor(),
                        2, 8, new Mat(), 0, new Point());
            }
            else {
                // dig line of label into object (otherwise it could float)
                start.x -= LABEL_LINE_1_DIG_IN_X;
                start.y -= LABEL_LINE_1_DIG_IN_Y;
            }

            // calculate points for the label lines and the text
            p1 = new Point(start.x + LABEL_LINE_1_WIDTH, start.y - LABEL_LINE_1_HEIGHT);
            p2 = new Point(p1.x + labelLine2Width, p1.y);
            locationName = new Point(p1.x + SPACE_BUFFER_TEXT_FRONT, p1.y - TEXT_MARGIN);

            // draw lines of label
            Imgproc.line(mRgba, start, p1, f.getType().getMarkerColor(),
                            LABEL_LINE_1_THICKNESS);
            Imgproc.line(mRgba, p1, p2, f.getType().getMarkerColor(), LABEL_LINE_2_THICKNESS);

            // draw text of label with name of detected fruit type
            Imgproc.putText(mRgba, f.getType().getName(), locationName,
                    TEXT_FONT_FACE, TEXT_FONT_SCALE, f.getType().getMarkerColor(),
                    TEXT_THICKNESS, TEXT_LINE_TYPE, false);

            // display further information, if it's desired (state of fruit)
            if (f.getIsInformationDisplayed()) {
                // prepare string for further information
                nValue = f.getType().getNutritionalValue();

                // security measures
                if (nValue != null) {
                    furtherInformation.add("" + (int) (nValue.getCaloricValuePer100g() *
                                    nValue.getAverageWeightServing()/100) + " kcal");
                    furtherInformation.add("Protein: " + String.format(Locale.ENGLISH, format,
                            (nValue.getProteinContentPer100g() *
                                    nValue.getAverageWeightServing()/100)) + "g");
                    furtherInformation.add("Carbs: " + String.format(Locale.ENGLISH, format,
                            (nValue.getCarbContentPer100g() *
                                    nValue.getAverageWeightServing()/100)) + "g");
                    furtherInformation.add("Fat: " + String.format(Locale.ENGLISH, format,
                            (nValue.getFatContentPer100g() *
                                    nValue.getAverageWeightServing()/100)) + "g");
                    furtherInformation.add("Weight: "
                                            + (int) nValue.getAverageWeightServing() + "g");
                }

                // prepare location
                locationFurtherInformation = new Point(p1.x, p1.y + TEXT_MARGIN + textSize.height);

                // check if any text was prepared yet before drawing it
                if (!furtherInformation.isEmpty()) {
                    for(int i=0; i < furtherInformation.size(); i++) {
                        Imgproc.putText(mRgba, furtherInformation.get(i),
                                new Point(locationFurtherInformation.x,
                                            locationFurtherInformation.y
                                                    + i*(textSize.height + TEXT_MARGIN)),
                                TEXT_FONT_FACE, TEXT_FONT_SCALE, f.getType().getMarkerColor(),
                                TEXT_THICKNESS, TEXT_LINE_TYPE, false);
                    }
                }
            }
        }
    }


    /**
     * method for testing all histogram comparison methods from opencv with given fruit type on
     * live frames
     *
     * @param ft                    FruitType, which histogram is compared with the live screen
     * @return comparisonResults    String with all formatted results, can be used for the log
     */
    public String testHistogramComparisonMethods(FruitType ft, List<Mat> candidateHistograms) {
        // variable declaration for string and format of results
        String comparisonResults;
        String format = "%7.2f";  // width = 7 and 2 digits after the dot

        // compare h and s histogram and add results to the corresponding string
        comparisonResults = "Fruit type: " + ft.getName() + "\n";
        comparisonResults += "Method: CV_COMP_CHISQR        ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_CHISQR)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_CHISQR)) + "\n";
        comparisonResults += "Method: CV_COMP_CHISQR_ALT    ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_CHISQR_ALT)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_CHISQR_ALT)) + "\n";
        comparisonResults += "Method: CV_COMP_INTERSECT     ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_INTERSECT)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_INTERSECT)) + "\n";
        comparisonResults += "Method: CV_COMP_BHATTACHARYYA ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_BHATTACHARYYA)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_BHATTACHARYYA)) + "\n";
        comparisonResults += "Method: CV_COMP_CORREL        ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_CORREL)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_CORREL)) + "\n";
        comparisonResults += "Method: CV_COMP_HELLINGER     ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_HELLINGER)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_HELLINGER)) + "\n";
        comparisonResults += "Method: CV_COMP_KL_DIV        ---   Values: H: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramH(),
                        candidateHistograms.get(0), Imgproc.CV_COMP_KL_DIV)) + ", S: " +
                String.format(Locale.ENGLISH, format, Imgproc.compareHist(ft.getHsvHistogramS(),
                        candidateHistograms.get(1), Imgproc.CV_COMP_KL_DIV)) + "\n";

        return comparisonResults;
    }


    /**
     * Creates a dialog box and asks for a name of the currently filtered object
     * afterwards saves the object with calculated histograms as a fruit type, locally and
     * also as an update in the database
     *
     */
    public void dialogInputSaveFruitType() {
        // initialization of dialog
        String strTitle = "Name of currently filtered fruit:";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(strTitle);

        // use an EditText view to get user input.
        final EditText input = new EditText(this);
        builder.setView(input);

        // define actions for left button of dialog
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // variable declaration
                String name = input.getText().toString();

                // save fruit type
                saveFruitType(name);

                // abolish frame freeze, which was initiated for inputs
                freezeCamera = false;
                return;
            }
        });

        // define actions for right button of dialog
        builder.setNegativeButton("Clear Database", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // clear locally saved fruit types and also the ones in the database
                fruitTypes.clear();
                dbFruitTypes.clear(dbFruitTypes.TABLE_NAME_FRUIT_TYPES);

                String text = "Registered fruit types deleted.";
                Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                toast.show();

                // abolish frame freeze, which was initiated for inputs
                freezeCamera = false;
            }
        });

        // resets frozen screen, if user cancels with a touch anywhere outside the dialog box
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                freezeCamera = false;
            }
        });

        // freeze camera, so the current picture is saved instead of the live picture
        // unfreeze, when a button is pressed (unfreeze action declared in button declarations)
        freezeCamera = true;

        // now show this dialog
        builder.show();
    }


    /**
     * saves fruit type with given name
     * gives feedback, if name was empty and nothing was saved
     *
     * @param name      defines name of the to be saved fruit type
     */
    public void saveFruitType(String name) {
        // variable declaration
        FruitType ft;

        if (!name.isEmpty()) {
            // clone hsv histograms since otherwise just references are copied
            List<Mat> clonedHistograms = new ArrayList<>();
            //List<Mat> clonedHistograms = new ArrayList<>(hsvHistograms);

            // clone histograms
            for (Mat m : hsvHistograms)
                clonedHistograms.add(m.clone());

            // apply autofit to the histograms and apply new histogram borders to
            // hsv filter settings
            //mHsvFilterValues = autofitHistograms(clonedHistograms);
            setRangeBarValues(autofitHistograms(clonedHistograms));

            // create fruit type with current data and name
            ft = new FruitType(name, mHsvFilterValues.clone(), clonedHistograms.get(0),
                    clonedHistograms.get(1), null, null, getNutritionalValue(name));

            // register the new fruit type to the local fruit type list and also save it in the
            // database, and in the tracker (every fruit type id of the tracker is thus
            // corresponding with the id in the fruitTypes List)
            fruitTypes.add(ft);
            dbFruitTypes.add(ft);
            fruitTracker.add(new LinkedList<Fruit>());
        } else {
            String text = "No name was entered, thus nothing could be saved.\n" +
                            "Please enter a name the next time.";
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    /**
     * makes all declared gui elements of the hsv mode visible or invisible, as defined
     *
     * @param isVisible     distinguishes if goal of actions is visibility or invisibility
     */
    public void setHsvModeGuiVisibility(boolean isVisible) {
        // handle seekbars
        LinearLayout layoutSeekbars = (LinearLayout) findViewById(R.id.seekbar_placeholder);
        setLinearLayoutVisibility(isVisible, layoutSeekbars);

        //// handle other objects
        // ... (not needed for the moment)
    }


    /**
     * delivers the nutritional value from a food with the specified name
     * the result is depending on the name of the object and is hard-coded for the demonstration
     * purposes - a crawler would be a better solution
     *
     * @param name      specifies name of the food
     */
    public NutritionalValue getNutritionalValue(String name) {
        // variable declaration
        NutritionalValue result = null;
        float caloricValuePer100g;
        float proteinContentPer100g;
        float carbContentPer100g;
        float fatContentPer100g;
        float averageWeightServing;

        if (name != null) {
            if (!name.isEmpty()) {
                if (name.contains("Banana") || name.contains("banana")) {
                    // get nutritional values for a banana
                    caloricValuePer100g = 89;
                    proteinContentPer100g = (float) 1.1;
                    carbContentPer100g = (float) 22.8;
                    fatContentPer100g = (float) 0.3;
                    averageWeightServing = 118;
                    result = new NutritionalValue(caloricValuePer100g, proteinContentPer100g,
                                    carbContentPer100g, fatContentPer100g, averageWeightServing);

                } else if (name.contains("Lemon") || name.contains("lemon")) {
                    // get nutritional values for a lemon
                    caloricValuePer100g = 29;
                    proteinContentPer100g = (float) 1.1;
                    carbContentPer100g = (float) 9.3;
                    fatContentPer100g = (float)0.3;
                    averageWeightServing = (float) 58;
                    result = new NutritionalValue(caloricValuePer100g, proteinContentPer100g,
                            carbContentPer100g, fatContentPer100g, averageWeightServing);

                } else if (name.contains("Apple") || name.contains("apple")) {
                    // get nutritional values for a apple
                    caloricValuePer100g = 52;
                    proteinContentPer100g = (float) 0.3;
                    carbContentPer100g = (float) 13.8;
                    fatContentPer100g = (float) 0.2;
                    averageWeightServing = 182;
                    result = new NutritionalValue(caloricValuePer100g, proteinContentPer100g,
                            carbContentPer100g, fatContentPer100g, averageWeightServing);

                } else if (name.contains("Mandarin") || name.contains("mandarin")) {
                    // get nutritional values for a mandarin
                    caloricValuePer100g = 53;
                    proteinContentPer100g = (float) 0.8;
                    carbContentPer100g = (float) 13.3;
                    fatContentPer100g = (float) 0.3;
                    averageWeightServing = 76;
                    result = new NutritionalValue(caloricValuePer100g, proteinContentPer100g,
                            carbContentPer100g, fatContentPer100g, averageWeightServing);

                } else if (name.contains("Orange") || name.contains("orange")) {
                    // get nutritional values for a mandarin
                    caloricValuePer100g = 49;
                    proteinContentPer100g = (float) 0.9;
                    carbContentPer100g = (float) 12.5;
                    fatContentPer100g = (float) 0.2;
                    averageWeightServing = 140;
                    result = new NutritionalValue(caloricValuePer100g, proteinContentPer100g,
                            carbContentPer100g, fatContentPer100g, averageWeightServing);

                }else {
                    // no values available, return null
                }
            }
        }
        return result;
    }
}
