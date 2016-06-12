#include "vrlab_foodui_FoodUiJNI.h"
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_findFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,
                                                jint detectorChoice);
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_threshCallback(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_hsvMode(JNIEnv *env, jobject obj, jlong addrGray, jlong addrRgba,
                                                jintArray hsvFilterValues);


    // global constant declaration
    const int THRESH = 100;

    // global variable declaration
    RNG rng(12345);



    /**
     * function for detection and drawing of features, uses different detectors, depending
     * on the choice of the user (for demo/exploration purposes)
     */
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_findFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,
                                                jint detectorChoice)
    {
        // variable declaration
        Mat& mGr  = *(Mat*)addrGray;
        Mat& mRgb = *(Mat*)addrRgba;
        vector<KeyPoint> v;
        Ptr<FeatureDetector> detector;

        // create detector
        switch(detectorChoice) {
            case 0:
                // FAST detector
                detector = FastFeatureDetector::create(50);
                break;
            case 1:
                // AGAST detector
                detector = AgastFeatureDetector::create(50);
                break;
            case 2:
                // GFTT detector, pretty slow
                detector = GFTTDetector::create(50);
                break;
            default:
                // simple blob detector, pretty slow
                detector = SimpleBlobDetector::create();
                break;
        }

        // proceed the detection
        detector->detect(mGr, v);

        // draw circles around the keypoints
        for( unsigned int i = 0; i < v.size(); i++ )
        {
            const KeyPoint& kp = v[i];
            circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
        }
    }


    /**
     * test function for finding and drawing contours in random colors
     */
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_threshCallback(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
    {
        // variable declaration
        Mat& mGr  = *(Mat*)addrGray;
        Mat& mRgb = *(Mat*)addrRgba;
        Mat mCannyOutput;
        vector<vector<Point> > contours;
        vector<Vec4i> hierarchy;

        // Detect edges using canny and find contours
        Canny( mGr, mCannyOutput, THRESH, THRESH*2, 3 );
        findContours( mCannyOutput, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE,
                      Point(0, 0) );

        // Draw contours
        for( int i = 0; i< contours.size(); i++ )
        {
            Scalar color = Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );
            drawContours( mRgb, contours, i, color, 2, 8, hierarchy, 0, Point() );
        }
    }


    /**
     * test function for evaluation of efficiency bonus in morph operations
     * (gives no significant benefit in this short form)
     */
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_morphOperations(JNIEnv*, jobject, jlong matAddrHsvThreshed)
    {
        // variable declaration
        Mat& mHsvThreshed = *(Mat*)matAddrHsvThreshed;

        // create elements for morphing operations
        Mat mErodeElement = getStructuringElement(MORPH_RECT, Size(3, 3));
        Mat mDilateElement = getStructuringElement(MORPH_RECT, Size(8, 8));

        // erode and dilate in each two iterations
        erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
        dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
    }


    /**
     * test function for conversion of rgba mat into hsv format
     */
    JNIEXPORT void JNICALL
    Java_vrlab_foodui_FoodUiJNI_hsvMode(JNIEnv *env, jobject, jlong addrGray, jlong addrRgba,
                                        jintArray hsvFilterValues)
    {
        // variable declaration
        Mat& mGr  = *(Mat*)addrGray;
        Mat& mRgb = *(Mat*)addrRgba;
        Mat mHsv;
        Mat mHsvThreshed;
        jint *mHsvFilterValues = env->GetIntArrayElements(hsvFilterValues, 0);

        // convert rgb frame into hsv format
        cvtColor(mRgb, mHsv, COLOR_RGB2HSV);


        /* the next block somehow does not work - however, the second block with fixed values was
         * tested and no significant improvement in computation time could be identified
         * in contrast to the java implementation
        */
        // filter
        inRange(mHsv,
                Scalar(mHsvFilterValues[0], mHsvFilterValues[1], mHsvFilterValues[2]),
                Scalar(mHsvFilterValues[3], mHsvFilterValues[4], mHsvFilterValues[5]),
                mHsvThreshed);

    }
}
