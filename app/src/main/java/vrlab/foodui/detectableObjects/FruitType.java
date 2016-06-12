package vrlab.foodui.detectableObjects;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Moments;

import vrlab.foodui.information.NutritionalValue;

/**
 * TODO: - think about using Moments for the shape, since it could create a normalized
 * TODO: (cont.) representation of the shape
 */


/**
 * class specifies a category of fruit, e.g. apple, banana, orange, and also
 * the data belonging to this category - the class contains the name of the fruit type and
 * also information, which is generated through a training
 *
 */
public class FruitType {
    // attribute section
    private String name;
    private int[] hsvFilterValues;
    private Mat hsvHistogramH;
    private Mat hsvHistogramS;
    private MatOfPoint shape;
    private Scalar markerColor;
    private NutritionalValue nutritionalValue;


    /**
     * empty standard constructor
     *
     */
    public FruitType(){}


    /**
     * constructor for direct initialization
     *
     * @param name              name of the fruit
     * @param colorFilter       color filter setting values
     * @param hsvHistogramH     color histogram for hue of filtered image area
     * @param hsvHistogramS     color histogram for saturation of filtered image area
     * @param shape             shape of fruit type (contour of detected area)
     * @param markerColor       specifies the representative color of the fruit type
     */
    public FruitType(String name, int[] colorFilter, Mat hsvHistogramH, Mat hsvHistogramS,
                     MatOfPoint shape, Scalar markerColor) {
        this.name = name;
        this.hsvFilterValues = colorFilter;
        this.hsvHistogramH = hsvHistogramH;
        this.hsvHistogramS = hsvHistogramS;
        this.shape = shape;

        // get a default color for markercolor depending on the name (if it's null)
        if (markerColor == null) {
            if (name.contains("Banana") || name.contains("banana") ||
                    name.contains("Lemon") || name.contains("lemon")) {
                markerColor = new Scalar(255, 255, 0);      // yellow
            } else if (name.contains("Apple") || name.contains("apple")) {
                markerColor = new Scalar(255, 0, 0);      // red
            } else if (name.contains("Mandarin") || name.contains("mandarin") ||
                    name.contains("Orange") || name.contains("orange")) {
                markerColor = new Scalar(255, 165, 0);      // orange
            } else if (name.contains("Lime") || name.contains("lime")) {
                markerColor = new Scalar(0, 255, 0);      // green
            } else if (name.contains("Blueberry") || name.contains("blueberry")) {
                markerColor = new Scalar(0, 0, 255);      // blue
            } else {
                markerColor = new Scalar(0, 255, 0);      // green as default
            }
        }

        /*
            scalar (color) notes:
                255 255 255 -> white
                0 255 255 -> cyan
                255 0 255 -> magenta
                255 255 0 ->
                0 0 255 ->
                255 0 0 ->
                0 255 0 ->
        */

        this.markerColor = markerColor;
    }


    /**
     * constructor for direct initialization (including nutritional value)
     *
     * @param name                  name of the fruit
     * @param colorFilter           color filter setting values
     * @param hsvHistogramH         color histogram for hue of filtered image area
     * @param hsvHistogramS         color histogram for saturation of filtered image area
     * @param shape                 shape of fruit type (contour of detected area)
     * @param markerColor           specifies the representative color of the fruit type
     * @param nutritionalValue      specifies the corresponding nutritional values
     */
    public FruitType(String name, int[] colorFilter, Mat hsvHistogramH, Mat hsvHistogramS,
                     MatOfPoint shape, Scalar markerColor, NutritionalValue nutritionalValue) {
        // call hierarchically paramount constructor
        this(name, colorFilter, hsvHistogramH, hsvHistogramS, shape, markerColor);

        this.nutritionalValue = nutritionalValue;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int[] getHsvFilterValues() {
        return hsvFilterValues;
    }

    public void setHsvFilterValues(int[] hsvFilterValues) {
        this.hsvFilterValues = hsvFilterValues;
    }


    public MatOfPoint getShape() {
        return shape;
    }

    public void setShape(MatOfPoint shape) {
        this.shape = shape;
    }


    public Scalar getMarkerColor() {
        return markerColor;
    }

    public void setMarkerColor(Scalar markerColor) {
        this.markerColor = markerColor;
    }


    public Mat getHsvHistogramH() {
        return hsvHistogramH;
    }

    public void setHsvHistogramH(Mat hsvHistogramH) {
        this.hsvHistogramH = hsvHistogramH;
    }


    public Mat getHsvHistogramS() {
        return hsvHistogramS;
    }

    public void setHsvHistogramS(Mat hsvHistogramS) {
        this.hsvHistogramS = hsvHistogramS;
    }


    public NutritionalValue getNutritionalValue() {
        return nutritionalValue;
    }

    public void setNutritionalValue(NutritionalValue nutritionalValue) {
        this.nutritionalValue = nutritionalValue;
    }
}
