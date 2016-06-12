package vrlab.foodui.information;

/**
 * the class nutritional value specifies typical values regarding the nutritional value of a food
 * all weight information is in grams [g] and the caloric value information is in kilocalories
 * [kcal]
 */
public class NutritionalValue {
    // attribute section
    private float caloricValuePer100g;     // specifies fat content in [g] per 100g of the food
    private float proteinContentPer100g;   // specifies protein content in [g] per 100g of the food
    private float carbContentPer100g;      // specifies carb content in [g] per 100g of the food
    private float fatContentPer100g;       // specifies fat content in [g] per 100g of the food
    private float averageWeightServing;    // specifies the average weight of one piece or serving size



    /**
     * empty standard constructor
     *
     */
    public NutritionalValue(){}


    /**
     * standard constructor with transfer variables for all attributes
     *
     * @param caloricValuePer100g       specifies fat content in [g] per 100g of the food
     * @param proteinContentPer100g     specifies protein content in [g] per 100g of the food
     * @param carbContentPer100g        specifies carb content in [g] per 100g of the food
     * @param fatContentPer100g         specifies fat content in [g] per 100g of the food
     * @param averageWeightServing      specifies the average weight of one piece or serving size
     *                                  (medium size)
     */
    public NutritionalValue(float caloricValuePer100g, float proteinContentPer100g,
                            float carbContentPer100g, float fatContentPer100g,
                            float averageWeightServing) {
        this.caloricValuePer100g = caloricValuePer100g;
        this.proteinContentPer100g = proteinContentPer100g;
        this.carbContentPer100g = carbContentPer100g;
        this.fatContentPer100g = fatContentPer100g;
        this.averageWeightServing = averageWeightServing;
    }


    public float getCaloricValuePer100g() {
        return caloricValuePer100g;
    }

    public void setCaloricValuePer100g(float caloricValuePer100g) {
        this.caloricValuePer100g = caloricValuePer100g;
    }


    public float getProteinContentPer100g() {
        return proteinContentPer100g;
    }

    public void setProteinContentPer100g(float proteinContentPer100g) {
        this.proteinContentPer100g = proteinContentPer100g;
    }


    public float getCarbContentPer100g() {
        return carbContentPer100g;
    }

    public void setCarbContentPer100g(float carbContentPer100g) {
        this.carbContentPer100g = carbContentPer100g;
    }


    public float getFatContentPer100g() {
        return fatContentPer100g;
    }

    public void setFatContentPer100g(float fatContentPer100g) {
        this.fatContentPer100g = fatContentPer100g;
    }


    public float getAverageWeightServing() {
        return averageWeightServing;
    }

    public void setAverageWeightServing(float averageWeightServing) {
        this.averageWeightServing = averageWeightServing;
    }
}
