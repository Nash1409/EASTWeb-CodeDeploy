package version2.prototype;

import java.io.Serializable;

import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Represents metadata for a single zonal summary. Paring of shape file and zone field. Object is immutable.
 *
 * @author michael.devos
 *
 */
@Immutable public class ZonalSummary implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String shapeFile;
    private final String areaIDField;
    private final String areaNameField;

    /**
     * Creates a ZonalSummary object pairing of shape file and zone field.
     *
     * @param shapeFile  - shape file to be used in this zonal summary
     * @param areaIDField
     * @param areaNameField
     */
    public ZonalSummary(String shapeFile, String areaIDField, String areaNameField) {
        this.shapeFile = shapeFile;
        this.areaIDField = areaIDField;
        this.areaNameField = areaNameField;
    }

    /**
     * Gets the paired shape file.
     *
     * @return shape file path
     */
    public String GetShapeFile() {
        return shapeFile;
    }

    /**
     * @deprecated
     * Gets the paired area ID field.
     *
     * @return area ID
     */
    @Deprecated
    public String GetAreaValueField() {
        return areaIDField;
    }

    /**
     * Gets the paired area code field.
     * @return area code field
     */
    public String GetAreaCodeField() {
        return areaIDField;
    }

    /**
     * Gets the area name field.
     *
     * @return zone name
     */
    public String GetAreaNameField() {
        return areaNameField;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "[shapefile=%s, areaIDField=%s, areaNameField=%s]",
                shapeFile, areaIDField, areaNameField
                );
    }
}
