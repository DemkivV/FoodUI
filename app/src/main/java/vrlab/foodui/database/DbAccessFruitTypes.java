package vrlab.foodui.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vrlab.foodui.detectableObjects.FruitType;
import vrlab.foodui.information.NutritionalValue;



/**
 * class for administration of a database for fruit types,
 * provides methods for easy access
 *
 */
public class DbAccessFruitTypes extends SQLiteOpenHelper {
    // constant section
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FoodUI.db";
    public static final String TABLE_NAME_FRUIT_TYPES = "fruitTypesTable";

    private static final String TAG = "FoodUI::database";
    private static final String STR_SEPARATOR = "__,__";        // used for conversion of vectors

    // attribute section
    private SQLiteDatabase db;



    public DbAccessFruitTypes(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }


    /**
     * database initialization, when object is created
     *
     * @param db    defines database, which needs to be initialized
     */
    public void onCreate(SQLiteDatabase db) {
        // variable declaration
        String SQL_CREATE_ENTRIES;

        // create table
        try{
            SQL_CREATE_ENTRIES =
                        "CREATE TABLE " + TABLE_NAME_FRUIT_TYPES + " " +
                        "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name VARCHAR(20) NOT NULL, " +
                        "filterValues TEXT NOT NULL, " +
                        "histType INTEGER NOT NULL, " +
                        "histHeight INTEGER NOT NULL, " +
                        "hsvHistogramH TEXT NOT NULL, " +
                        "hsvHistogramS TEXT NOT NULL, " +
                        "shapeType INTEGER, " +
                        "shapeHeight INTEGER, " +
                        "shapeWidth INTEGER, " +
                        "shape BLOB, " +
                        "scalar TEXT NOT NULL," +
                        "nutritionalValue TEXT)";
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


    /**
     * adjustment operations, when database is upgraded
     *
     * @param db
     * @param oldVersion    version number from previous version
     * @param newVersion    version number from new version
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        onCreate(db);
    }


    /**
     * adjustment operations, when database is downgraded
     *
     * @param db
     * @param oldVersion    version number from previous version
     * @param newVersion    version number from new version
     */
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    /**
     * cleanup operations, when database is closed
     *
     */
    public synchronized void close() {
        if(db != null) {
            db.close();
            db = null;
        }

        super.close();
    }



    /**
     * returns all fruit types from the database as a List<FruitType>
     *
     * @return : list with all fruit types saved in the database --> copy afterwards to local list
     */
    public List<FruitType> getAllData(){
        // variable declaration
        List<FruitType> ftList = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME_FRUIT_TYPES, null, null, null, null, null, null);
        int count = cursor.getCount();

        // go through each entry and put information in temporary fruit type
        // when finished with all data puffering, add finished fruit type to list
        cursor.moveToFirst();
        for(int i = 0; i < count; i++) {
            // declare and initialize temporary variables
            FruitType ft = new FruitType();
            String name;
            int[] hsvFilterValues = new int[6];

            int histHeight;
            int histType;
            float[] fHsvHistogramH;
            Mat hsvHistogramH;
            float[] fHsvHistogramS;
            Mat hsvHistogramS;

            int shapeHeight;
            int shapeWidth;
            int shapeType;
            byte[] bShape;

            MatOfPoint shape;

            double[] markerColorValues;
            Scalar markerColor;

            float[] nutritionalValueVector;
            NutritionalValue nutritionalValue = null;


            // get types, heights (and width) of histograms and shape for further processing
            histType = cursor.getInt(3);
            histHeight =  cursor.getInt(4);
            shapeHeight =  cursor.getInt(7);
            shapeWidth =  cursor.getInt(8);
            shapeType =  cursor.getInt(9);

            // prepare mats for histograms and shape
            hsvHistogramH = new Mat(histHeight, 1, histType);
            hsvHistogramS = new Mat(histHeight, 1, histType);
            shape = new MatOfPoint(new Mat(shapeHeight, shapeWidth, shapeType));

            //// get other values of current cursor entry
            // get name
            name = cursor.getString(1);

            // get filter values
            hsvFilterValues = convertStringToIntArray(cursor.getString(2));

            // get histogram information
            fHsvHistogramH = convertStringToFloatArray(cursor.getString(5));
            hsvHistogramH.put(0, 0, fHsvHistogramH);
            fHsvHistogramS = convertStringToFloatArray(cursor.getString(6));
            hsvHistogramS.put(0, 0, fHsvHistogramS);

            // get markerColor information
            markerColorValues = convertStringToDoubleArray(cursor.getString(11));
            markerColor = new Scalar(markerColorValues);

            // get shape information -- since shape could be null -> further safety measures
            try {
                bShape = cursor.getBlob(10);
                shape.put(0, 0, bShape);
            } catch(Exception e) {
                Log.e(TAG, e.getMessage());
            }

            try {
                nutritionalValueVector = convertStringToFloatArray(cursor.getString(12));
                nutritionalValue = new NutritionalValue(nutritionalValueVector[0],
                                        nutritionalValueVector[1], nutritionalValueVector[2],
                                        nutritionalValueVector[3], nutritionalValueVector[4]);
            } catch(Exception e) {
                Log.e(TAG, e.getMessage());
            }

            // apply retrieved data to temporary fruit type
            ft.setName(name);
            ft.setHsvFilterValues(hsvFilterValues);
            ft.setHsvHistogramH(hsvHistogramH);
            ft.setHsvHistogramS(hsvHistogramS);
            ft.setShape(shape);
            ft.setMarkerColor(markerColor);
            ft.setNutritionalValue(nutritionalValue);

            // add prepared fruit type to list
            ftList.add(ft);

            // move on to next item
            cursor.moveToNext();
        }

        return ftList;
    }


    /**
     * adds the fruit type from the transfer variable to the database
     *
     * @param ft : fruit type, which is added to db
     */
    public void add(FruitType ft){
        // general variable declaration
        ContentValues data = new ContentValues();
        byte[] bShape = null;
        int size = ft.getHsvHistogramH().height();
        float fHsvHistogramH[] = new float[size];
        float fHsvHistogramS[] = new float[size];
        float nutritionalValue[] = new float[5];

        // convert histograms in float arrays
        ft.getHsvHistogramH().get(0, 0, fHsvHistogramH);
        ft.getHsvHistogramS().get(0, 0, fHsvHistogramS);

        // fill contentvalues with data from fruit type
        data.put("name", ft.getName());
        data.put("filterValues", Arrays.toString(ft.getHsvFilterValues()));
        data.put("histType", ft.getHsvHistogramH().type());
        data.put("histHeight", ft.getHsvHistogramH().height());
        data.put("hsvHistogramH", Arrays.toString(fHsvHistogramH));
        data.put("hsvHistogramS", Arrays.toString(fHsvHistogramS));
        data.put("scalar", Arrays.toString(ft.getMarkerColor().val));

        // fill contentvalues with data from the shape of the fruit type, if available
        if(ft.getShape() != null) {
            ft.getShape().get(0, 0, bShape);
            data.put("shape", bShape);
            data.put("shapeType", ft.getShape().type());
            data.put("shapeHeight", ft.getShape().height());
            data.put("shapeWidth", ft.getShape().width());
        }

        // get nutritional value if available
        if (ft.getNutritionalValue()!=null) {
            nutritionalValue[0] = ft.getNutritionalValue().getCaloricValuePer100g();
            nutritionalValue[1] = ft.getNutritionalValue().getProteinContentPer100g();
            nutritionalValue[2] = ft.getNutritionalValue().getCarbContentPer100g();
            nutritionalValue[3] = ft.getNutritionalValue().getFatContentPer100g();
            nutritionalValue[4] = ft.getNutritionalValue().getAverageWeightServing();
            data.put("nutritionalValue", Arrays.toString(nutritionalValue));
        }

        // insert prepared data (containing one fruit type) into the database
        db.insertOrThrow(TABLE_NAME_FRUIT_TYPES, null, data);
    }


    /**
     * checks if the defined table is existing
     *
     * @param tableName : table to be checked
     * @return : true if table is existing, e if not
     */
    public boolean isTableExisting(String tableName) {
        boolean isExisting = false;

        // get cursor for defined table
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master " +
                                    "where tbl_name = '"+ tableName +"'", null);

        // check if cursor points to a table
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                isExisting = true;
            }
            cursor.close();
        }

        return isExisting;
    }


    /**
     * clear the whole database (useful for debugging)
     *
     */
    public void clear(String tableName) {
        String SQL_CLEAR_TABLE = "DELETE FROM " + tableName + " WHERE 1=1";

        // try to delete all entries
        try{
            db.execSQL(SQL_CLEAR_TABLE);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }



    ///////////////////////////////////// helper methods ///////////////////////////////////////////



    /**
     * transform String array with separated elements into String with concatenated elements
     *
     * @param array : String array with separated elements
     * @return : String with concatenated elements
     */
    public static String convertStringArrayToString(String[] array){
        String str = "";
        for (int i = 0;i<array.length; i++) {
            str = str+array[i];
            // do not append comma at the end of last element
            if(i<array.length-1){
                str = str+ STR_SEPARATOR;
            }
        }
        return str;
    }


    /**
     * transform String with concatenated elements into String array with separated elements
     *
     * @param string : String with concatenated elements
     * @return : String array with separated elements
     */
    public static String[] convertStringToStringArray(String string){
        String[] arr = string.split(STR_SEPARATOR);
        return arr;
    }


    /**
     * transform String with concatenated elements into int array with separated elements
     *
     * @param string : String with concatenated elements
     * @return : float array with separated elements
     */
    private static int[] convertStringToIntArray(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        int result[] = new int[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(strings[i]);
        }
        return result;
    }


    /**
     * transform String with concatenated elements into float array with separated elements
     *
     * @param string : String with concatenated elements
     * @return : float array with separated elements
     */
    private static float[] convertStringToFloatArray(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        float result[] = new float[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Float.parseFloat(strings[i]);
        }
        return result;
    }


    /**
     * transform String with concatenated elements into double array with separated elements
     *
     * @param string : String with concatenated elements
     * @return : float array with separated elements
     */
    private static double[] convertStringToDoubleArray(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        double result[] = new double[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.parseDouble(strings[i]);
        }
        return result;
    }
}
