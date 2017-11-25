package version2.prototype.summary.temporal;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;

import version2.prototype.Config;
import version2.prototype.Process;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;

/**
 * Merging strategy template to be used in temporal summary for creating classes to define how to create composite data files.
 *
 * @author michael.devos
 *
 */
public interface MergeStrategy {
    /**
     * Merge method. Returns metadata about created merged raster file.
     *
     * @param configInstance
     * @param con
     * @param process
     * @param projectInfo
     * @param pluginName  - current plugin's name
     * @param indexNm  - current index name
     * @param firstDate  - date of first data file in composite
     * @param rasterFiles  - list of files to create composite from
     * @param outputFilePath  - string path for the merged file
     * @return metadata about single merged file created
     * @throws Exception
     */
    DataFileMetaData Merge(Config configInstance, Connection con, Process process, ProjectInfoFile projectInfo, String pluginName, String indexNm, LocalDate firstDate, File[] rasterFiles,
            String outputFilePath) throws Exception;
}
