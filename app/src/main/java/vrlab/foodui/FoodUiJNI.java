package vrlab.foodui;

public class FoodUiJNI {

    /**
     * function for detection and drawing of features, uses different detectors, depending
     * on the choice of the user (for demo/exploration purposes)
     */
    public native static void findFeatures(long matAddrGr, long matAddrRgba, int detectorChoice);


    /**
     * test function for finding and drawing contours in random colors
     */
    public native static void threshCallback(long matAddrGr, long matAddrRgba);


    /**
     * test function for evaluation of efficiency bonus in morph operations
     * (gives no significant benefit in this short form)
     */
    public native static void morphOperations(long matAddrRgba);


    /**
     * test function for conversion of rgba mat into hsv format
     */
    public native static void hsvMode(long matAddrGr, long matAddrRgba, int hsvFilterValues[]);
}
