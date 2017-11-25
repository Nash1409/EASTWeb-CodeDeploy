package version2.prototype.util;

import java.util.ArrayList;

import version2.prototype.util.GdalUtils;

import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.ogr;
import org.gdal.ogr.Layer;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.DataSource;


public class ReadShapefile {

    private DataSource shapefile;
    private ArrayList<String []> featureNameList = new ArrayList<String[]>();
    private ArrayList<String []> numericFeatureList = new ArrayList<String[]>();
    private ArrayList<String []> stringFeatureList = new ArrayList<String[]>();

    /* filename:  the input shapefile name
     * throws exception ShapefileException if a shapefile does not exist or cannot be open
     * precondition: none
     * postcondition: a datasource object is created to for the input shapefile
     */

    public ReadShapefile(String filename) throws ShapefileException{
        if (filename != null){
            GdalUtils.register();
            synchronized (GdalUtils.lockObject) {
                shapefile = ogr.Open(filename);
                if (shapefile == null){
                    throw new ShapefileException("The shape file " + filename + " cannot be properly opened.");
                }
            }
        }
        else{
            throw new ShapefileException("The shape file " + filename + " does not exist");
        }

        ProcessFeatures();
    }

    /* precondition: none
     * postcondition: get the feature(field) names in each layer
     *              store all of them in each featureNameList element
     *              Store the numeric ones in each numericFeatureList element
     */
    private void ProcessFeatures()
    {
        synchronized (GdalUtils.lockObject) {
            // System.out.println("ShapeFile count: " + shapefile.GetLayerCount());
            for (int iLayer=0; iLayer<shapefile.GetLayerCount(); iLayer++) {
                // System.out.println("Test_Layer: " + iLayer);
                Layer layer = shapefile.GetLayer(iLayer);
                FeatureDefn layerDefn = layer.GetLayerDefn();

                int count = layerDefn.GetFieldCount();

                String [] featureNames = new String[count];
                ArrayList <String> numericFeatures =  new ArrayList<String>();
                ArrayList <String> stringFeatures =  new ArrayList<String>();

                for (int iFeature=0; iFeature<count; iFeature++) {
                    FieldDefn fieldDefn = layerDefn.GetFieldDefn(iFeature);
                    String type = fieldDefn.GetFieldTypeName(fieldDefn.GetFieldType()).toLowerCase();
                    //System.out.println(type);
                    featureNames[iFeature] = layerDefn.GetFieldDefn(iFeature).GetName();
                    if (type.equals("real") || type.equals("integer"))
                    {
                        numericFeatures.add(featureNames[iFeature]);
                    }
                    if (type.equals("string"))
                    {
                        stringFeatures.add(featureNames[iFeature]);
                    }

                }

                featureNameList.add(featureNames);

                numericFeatureList.add(numericFeatures.toArray(new String[numericFeatures.size()]));

                stringFeatureList.add(stringFeatures.toArray(new String[stringFeatures.size()]));
            }
        }
    }

    /* precondition: none
     * postcondition: ALL the feature(field) names in each layer are stored in each ArrayList element
     */

    public ArrayList<String []> getFeatureList()
    {
        return featureNameList;
    }

    /* precondition: none
     * postcondition: the features with numeric values in each layer are stored in each ArrayList element
     */
    public ArrayList<String []> getNumericFeatureList()
    {
        return numericFeatureList;
    }

    /* precondition: none
     * postcondition: the features with string values in each layer are stored in each ArrayList element
     */
    public ArrayList<String []> getNameFeatureList()
    {
        return stringFeatureList;
    }


}