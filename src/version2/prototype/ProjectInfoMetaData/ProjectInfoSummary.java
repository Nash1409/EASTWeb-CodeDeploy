package version2.prototype.ProjectInfoMetaData;

import jdk.nashorn.internal.ir.annotations.Immutable;
import version2.prototype.ZonalSummary;

/**
 * Represents a summary element from the project meta data. Object is immutable.
 *
 * @author michael.devos
 *
 */
@Immutable public class ProjectInfoSummary {
    public static final String AREA_NAME_FIELD_TAG = "AreaNameField";
    public static final String SHAPE_FILE_TAG = "Shape File Path";
    public static final String AREA_CODE_FIELD_TAG = "AreaCodeField";
    public static final String TEMPORAL_SUMMARY_TAG = "Temporal Summary";
    private final ZonalSummary zonalSummary;
    private final String temporalSummaryCompositionStrategyClassName;
    private final int ID;

    /**
     * Create an instance of ProjectInfoSummary to contain all information from ProjectInfoFile relevant for the Summary framework.
     *
     * @param zonalSummary  - ZonalSummary object representing zonal data from a project metadata xml
     * @param temporalSummaryCompositionStrategyClassName  - class name of the used TemporalSummaryCompositionStrategy in making the filestore parameter
     * @param summaryID
     */
    public ProjectInfoSummary(ZonalSummary zonalSummary, String temporalSummaryCompositionStrategyClassName, int summaryID)
    {
        this.zonalSummary = zonalSummary;
        this.temporalSummaryCompositionStrategyClassName = temporalSummaryCompositionStrategyClassName;
        ID = summaryID;
    }

    /**
     *  Gets the ZonalSummary object from the instance.
     *
     * @return ZonalSummary instance
     */
    public ZonalSummary GetZonalSummary() { return zonalSummary; }

    /**
     * Gets the TemporalSummaryCompositionStrategy class name used to create the TemporalSummaryRasterFileStore object that's returned from GetTemporalFileStore().
     *
     * @return the class name of the TemporalSummaryCompositionStrategy gotten from the project meta data for this summary element
     */
    public String GetTemporalSummaryCompositionStrategyClassName() { return temporalSummaryCompositionStrategyClassName; }

    /**
     * Gets the ID attribute value assigned to this Summary element in the project metadata xml.
     *
     * @return int ID attributed
     */
    public int GetID() { return ID; }

    @Override
    public String toString() {
        if(temporalSummaryCompositionStrategyClassName != null && !temporalSummaryCompositionStrategyClassName.isEmpty()) {
            return AREA_NAME_FIELD_TAG + ": " + zonalSummary.GetAreaNameField() + "; " + SHAPE_FILE_TAG + ": " + zonalSummary.GetShapeFile() + "; " + AREA_CODE_FIELD_TAG + ": " +
                    zonalSummary.GetAreaCodeField() + "; " + TEMPORAL_SUMMARY_TAG + ": " + temporalSummaryCompositionStrategyClassName;
        } else {
            return AREA_NAME_FIELD_TAG + ": " + zonalSummary.GetAreaNameField() + "; " + SHAPE_FILE_TAG + ": " + zonalSummary.GetShapeFile() + "; " + AREA_CODE_FIELD_TAG + ": " +
                    zonalSummary.GetAreaCodeField();
        }
    }
}
