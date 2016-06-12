package vrlab.foodui.detectableObjects;


import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


/**
 * class for located fruits, consists of a FruitType, the contour and relevant positions such
 * as the mass center or the label location (defined as the outermost point on the right)
 *
 */
public class Fruit {
    // attribute section
    FruitType type;                 // specifies, to which fruit type this fruit belongs
    MatOfPoint contour;             // contains coordinates of the contour
    Point locationMassCenter;       // specifies mass center of the object
    Point locationLabel;            // specifies location of starting point for labeling actions
    boolean isInformationDesired = false;
                                    // specifies, if further information displaying is currently
                                     // desired (useful for stateful information displaying)



    /**
     * empty standard constructor
     *
     */
    public Fruit() {}


    /**
     * initialization constructor
     *
     * @param type          defines the type of the detected fruit
     * @param contour       describes the contour of the detected fruit within the frame
     */
    public Fruit(FruitType type, MatOfPoint contour) {
        // variable declaration for point calculations
        Moments m;
        double x, y;

        // get transfer variables
        this.type = type;
        this.contour = contour;

        //// calculate mass center and outermost
        // get and save the mass center of the detected object and put into fruit type
        m = Imgproc.moments(contour, false);
        x = m.get_m10() / m.get_m00();
        y = m.get_m01() / m.get_m00();
        this.locationMassCenter = new Point(x, y);

        // get the beginning point of the labeling --> outermost (right) point on mass center level
        // browse through points of contour to get it
        for(int i = 0; i < contour.size().height; i++) {
            if (x < contour.get(i, 0)[0]) {
                x = contour.get(i, 0)[0];
                y = contour.get(i, 0)[1];
            }
        }

        this.locationLabel = new Point(x, y);
    }


    public FruitType getType() {
        return type;
    }

    public void setType(FruitType type) {
        this.type = type;
    }


    public MatOfPoint getContour() {
        return contour;
    }

    public void setContour(MatOfPoint contour) {
        this.contour = contour;
    }


    public Point getLocationMassCenter() {
        return locationMassCenter;
    }

    public void setLocationMassCenter(Point p) {
        this.locationMassCenter = p;
    }


    public Point getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(Point p) {
        this.locationLabel = p;
    }


    public boolean getIsInformationDisplayed() {
        return isInformationDesired;
    }

    public void setIsInformationDisplayed(boolean isInformationDisplayed) {
        this.isInformationDesired = isInformationDisplayed;
    }
}
