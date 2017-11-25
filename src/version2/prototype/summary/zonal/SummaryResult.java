/**
 *
 */
package version2.prototype.summary.zonal;

import java.util.Map;

/**
 * @author michael.devos
 *
 */
public class SummaryResult {
    /**
     *
     */
    public final int projectSummaryID;
    /**
     *
     */
    public final String areaName;
    /**
     *
     */
    public final int areaCode;
    /**
     *
     */
    public final int dateGroupID;
    /**
     *
     */
    public final int indexID;
    /**
     *
     */
    public final String filePath;
    /**
     *
     */
    public final Map<String, Double> summaryResults;

    /**
     * @param projectSummaryID
     * @param areaName
     * @param areaCode
     * @param dateGroupID
     * @param indexID
     * @param filePath
     * @param summaryResults
     */
    public SummaryResult(int projectSummaryID, String areaName, int areaCode, int dateGroupID, int indexID, String filePath, Map<String, Double> summaryResults)
    {
        this.projectSummaryID = projectSummaryID;
        this.areaName = areaName;
        this.areaCode = areaCode;
        this.dateGroupID = dateGroupID;
        this.indexID = indexID;
        this.filePath = filePath;
        this.summaryResults = summaryResults;
    }
}
