/**
 *
 */
package version2.prototype.summary.zonal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Transformer;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Layer;
import org.gdal.osr.SpatialReference;

import version2.prototype.util.GdalUtils;
import version2.prototype.util.IndicesFileMetaData;

/**
 * @author michael.devos
 *
 */
public class LayerFileData {
    public final String areaCodeField;
    public final String areaNameField;
    public SummariesCollection summariesCollection;

    private Map<Integer, Boolean> zoneReceivedValidData;
    private Map<Integer, String> areas; // <AreaCode, AreaName>
    private Map<Integer, Double> countMap;

    private Integer noDataValue;

    public LayerFileData(IndicesFileMetaData inputFile, String shapeFilePath, Layer layer, String areaCodeField, String areaNameField, SummariesCollection summariesCollection, Dataset raster,
            Integer noDataValue) throws IllegalArgumentException, UnsupportedOperationException, IOException
    {
        this.areaCodeField = areaCodeField;
        this.areaNameField = areaNameField;
        this.summariesCollection = summariesCollection;
        zoneReceivedValidData = new HashMap<Integer, Boolean>();
        this.noDataValue = noDataValue;
        Dataset zoneRaster = null;

        // Validate inputs
        if (!isSameProjection(raster, layer)) {
            throw new IOException("\"" + inputFile.dataFilePath + "\" isn't in same projection as \"" + shapeFilePath + "\"");
        }

        if (!isLayerSubsetOfRaster(layer, raster)) {
            throw new IOException("\"" + shapeFilePath + "\" isn't a subset of \"" + inputFile.dataFilePath + "\".");
        }

        // Create the zone raster
        zoneRaster = rasterize(layer, raster.GetGeoTransform());

        assert(raster.GetRasterXSize() == zoneRaster.GetRasterXSize());
        assert(raster.GetRasterYSize() == zoneRaster.GetRasterYSize());

        // Calculate statistics
        try {
            countMap = calculateStatistics(raster, zoneRaster);
        } catch(IllegalArgumentException | UnsupportedOperationException | IOException e) {
            if(zoneRaster != null) {
                zoneRaster.delete();
            }
            throw e;
        }

        // Compile list of area codes and area names
        areas = new HashMap<Integer, String>();
        layer.ResetReading(); GdalUtils.errorCheck();
        Feature feature = layer.GetNextFeature(); GdalUtils.errorCheck();
        int areaCode;
        String areaName;
        while (feature != null) {
            areaCode = feature.GetFieldAsInteger(areaCodeField); GdalUtils.errorCheck();
            areaName = feature.GetFieldAsString(areaNameField); GdalUtils.errorCheck();
            areas.put(areaCode, areaName);
            feature = layer.GetNextFeature(); GdalUtils.errorCheck();
        }
    }

    public Map<Integer, Boolean> getZoneReceivedValidData()
    {
        Map<Integer, Boolean> zones = new HashMap<Integer, Boolean>();
        for(Integer key : zoneReceivedValidData.keySet())
        {
            zones.put(key, zoneReceivedValidData.get(key));
        }
        return zones;
    }

    public Map<Integer, String> getAreas()
    {
        Map<Integer, String> copy = new HashMap<Integer, String>();
        for(Integer key : areas.keySet())
        {
            copy.put(key, areas.get(key));
        }
        return copy;
    }

    public Map<Integer, Double> getCountMap()
    {
        Map<Integer, Double> copy = new HashMap<Integer, Double>();
        for(Integer key : countMap.keySet())
        {
            copy.put(key, countMap.get(key));
        }
        return copy;
    }

    /**
     * Checks whether the given raster and layer share the same projection.
     *
     * @param raster  - input raster file
     * @param layer  -  input shape file
     * @return true if projections are the same
     * @throws IOException
     * @throws UnsupportedOperationException
     * @throws IllegalArgumentException
     */
    private boolean isSameProjection(Dataset raster, Layer layer) throws IllegalArgumentException, UnsupportedOperationException, IOException {
        SpatialReference rasterRef = new SpatialReference(raster.GetProjection()); GdalUtils.errorCheck();
        boolean same = layer.GetSpatialRef().IsSame(rasterRef) != 0; GdalUtils.errorCheck();
        return same;
    }

    private boolean isLayerSubsetOfRaster(Layer layer, Dataset raster)
            throws IllegalArgumentException, UnsupportedOperationException, IOException {
        double[] extent = layer.GetExtent(true); GdalUtils.errorCheck();

        Vector<String> options = new Vector<String>();
        options.add("SRC_DS=" + layer.GetSpatialRef().ExportToWkt()); GdalUtils.errorCheck();

        Transformer transformer = new Transformer(null, raster, options); GdalUtils.errorCheck();

        double[] min = new double[] {Math.min(extent[0], extent[1]), Math.min(extent[2], extent[3]), 0};
        double[] max = new double[] {Math.max(extent[0], extent[1]), Math.max(extent[2], extent[3]), 0};

        transformer.TransformPoint(0, min); GdalUtils.errorCheck();
        transformer.TransformPoint(0, max); GdalUtils.errorCheck();

        int layerMinX = (int) Math.round(Math.min(min[0], max[0]));
        int layerMaxX = (int) Math.round(Math.max(min[0], max[0]));
        int layerMinY = (int) Math.round(Math.min(min[1], max[1]));
        int layerMaxY = (int) Math.round(Math.max(min[1], max[1]));

        int rasterMinX = 0;
        int rasterMaxX = raster.GetRasterXSize(); GdalUtils.errorCheck();
        int rasterMinY = 0;
        int rasterMaxY = raster.GetRasterYSize(); GdalUtils.errorCheck();

        if (layerMinX < rasterMinX) {
            return false;
        } else if (layerMaxX > rasterMaxX) {
            return false;
        } else if (layerMinY < rasterMinY) {
            return false;
        } else if (layerMaxY > rasterMaxY) {
            return false;
        }

        return true;
    }

    /**
     * Rasterizes data creating Dataset object. Uses GDal library to do calculation.
     *
     * @param layer  - shape file
     * @param transform  - raster data in single double array
     * @return all data after rasterization
     * @throws IOException
     * @throws UnsupportedOperationException
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private Dataset rasterize(Layer layer, double[] transform) throws IllegalArgumentException, UnsupportedOperationException, IOException {

        // Create the raster to burn values into
        double[] layerExtent = layer.GetExtent(); GdalUtils.errorCheck();

        Dataset zoneRaster = gdal.GetDriverByName("MEM").Create(
                "",
                (int) Math.ceil((layerExtent[1]-layerExtent[0]) / Math.abs(transform[1])),
                (int) Math.ceil((layerExtent[3]-layerExtent[2]) / Math.abs(transform[5])),
                1,
                gdalconstConstants.GDT_UInt32
                );
        GdalUtils.errorCheck();

        zoneRaster.SetProjection(layer.GetSpatialRef().ExportToWkt()); GdalUtils.errorCheck();
        zoneRaster.SetGeoTransform(new double[] {
                layerExtent[0], transform[1], 0,
                layerExtent[2] + zoneRaster.GetRasterYSize()*Math.abs(transform[5]), 0, transform[5]
        });
        GdalUtils.errorCheck();

        // Burn the values
        Vector<String> options = new Vector<String>();
        options.add("ATTRIBUTE=" + areaCodeField);

        gdal.RasterizeLayer(zoneRaster, new int[] {1}, layer, new double[] {}, options); GdalUtils.errorCheck();

        return zoneRaster;
    }


    private Map<Integer, Double> calculateStatistics(Dataset rasterDS, Dataset featureDS) throws IllegalArgumentException, UnsupportedOperationException, IOException {
        // Calculate zonal statistics
        Band zoneBand = featureDS.GetRasterBand(1); GdalUtils.errorCheck();
        Band rasterBand = rasterDS.GetRasterBand(1); GdalUtils.errorCheck();

        final int WIDTH = zoneBand.GetXSize(); GdalUtils.errorCheck();
        final int HEIGHT = zoneBand.GetYSize(); GdalUtils.errorCheck();

        int[] zoneArray = new int[WIDTH];
        double[] rasterArray = new double[WIDTH];

        Double[] noData = new Double[1];
        rasterBand.GetNoDataValue(noData);
        final ArrayList<Double> NO_DATA = new ArrayList<Double>();
        //        NO_DATA.add(new Double(GdalUtils.NO_DATA));
        NO_DATA.add(new Double(noDataValue));

        for (int y=0; y<HEIGHT; y++) {
            zoneBand.ReadRaster(0, y, WIDTH, 1, zoneArray); GdalUtils.errorCheck();
            rasterBand.ReadRaster(0, y, WIDTH, 1, rasterArray); GdalUtils.errorCheck();

            for (int i=0; i<WIDTH; i++) {
                int zone = zoneArray[i];
                Double value = rasterArray[i];
                if (!NO_DATA.contains(value)) { // Neither are no data values
                    summariesCollection.add(zone, value);
                    zoneReceivedValidData.put(zone, true);
                }
                else if(zoneReceivedValidData.get(zone) == null) {
                    zoneReceivedValidData.put(zone, false);
                }
            }
        }

        ArrayList<SummaryNameResultPair> results = summariesCollection.getResults();
        Map<Integer, Double> countMap = new HashMap<Integer, Double>(1);
        for(SummaryNameResultPair pair : results){
            if(pair.getSimpleName().equalsIgnoreCase("count")) {
                countMap = pair.getResult();
            }
        }

        return countMap;
    }
}
