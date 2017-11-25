package version2.prototype.indices.NldasForcing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import com.amazonaws.samples.DeployCode.S3;
import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.util.GdalUtils;
import version2.prototype.util.Schemas;

public class NldasForcingExtraIndices {

    private static MonthDay fdate;
    private static MonthDay hdate;

    public static void main(String[] args) throws ClassNotFoundException, SQLException, FileNotFoundException, InterruptedException {
        if(args.length < 7) {
            System.out.print("ExtraIndices");
            for(String arg : args) {
                System.out.print(" " + arg);
            }
            System.out.println();
            System.out.println("usage: ExtraIndices.bat databaseName user password schema projectName start outDayCount");
            return;
        }

        final String databaseName = args[0];
        final String user = args[1];
        final String password = args[2];
        final String schema = args[3];
        final String projectName = args[4];
        final String start = args[5];
        final Integer outDayCount = Integer.parseInt(args[6]);

        final LocalDate freezing = LocalDate.of(1970, 10, 5);
        final LocalDate heating = LocalDate.of(1970, 4, 12);


        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/" + databaseName;
        Connection con = DriverManager.getConnection(url, user, password);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(start, dtf);

        final String rootDir = "C:\\eastweb\\Projects";
        getCumulative(con, rootDir, "EASTWeb", schema, projectName, startDate, outDayCount, freezing,  heating);
        con.close();
    }

    public static void getCumulative(Connection con, final String rootDir, final String globalSchema, final String projectSchema, final String projectName,
            final LocalDate startDate, final Integer outDayCount, final LocalDate freezing, final LocalDate heating) throws NumberFormatException, ClassNotFoundException, SQLException, FileNotFoundException {

        final Integer inDayCount = outDayCount;
        final String projectRoot;
        fdate = MonthDay.of(freezing.getMonth(), freezing.getDayOfMonth());
        hdate = MonthDay.of(heating.getMonth(), heating.getDayOfMonth());
        String path;
        if(rootDir.endsWith("\\") || rootDir.endsWith("/")) {
            projectRoot = rootDir + projectName + "\\";
        } else {
            projectRoot = rootDir + "\\" + projectName + "\\";
        }
        String[] plugins = new File(projectRoot).list(new FilenameFilter(){  //plugins folder
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        S3.GetIndicesFile(projectRoot);
        System.out.println("Creating Cumulative Indices...");

        Connection con1 = RDS.getCentralDBconnection();
        Statement stmt1 = con1.createStatement();

        Statement stmt = con.createStatement();
        NldasForcingExtraIndices instance = new NldasForcingExtraIndices();
        IndicesDates indexDates = instance.new IndicesDates();


        /*System.out.println(validDays);*/

        for (String plugin : plugins)
        {
            String outputRoot = projectRoot + plugin + "\\Indices\\Output\\";

            String[] years = new File(outputRoot).list(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });

            for(String year : years)
            {
                String yearRoot = outputRoot + year + "\\";
                String[] days = new File(yearRoot).list(new FilenameFilter(){
                    @Override
                    public boolean accept(File dir, String name) {
                        return new File(dir, name).isDirectory();
                    }
                });

                for(String day : days)
                {
                    File d = new File(yearRoot + day + "\\");
                    for(File file : d.listFiles())
                    {
                        if(file.getName().contains("NldasForcingOverwinteringIndex")) {
                            //download file from S3
                            indexDates.addDate(plugin, file.getName().substring(0, file.getName().lastIndexOf(".")), Integer.parseInt(year), Integer.parseInt(day));
                        }
                        else if(file.getName().contains("NldasForcingLymeDiseaseIndex"))
                        {
                            indexDates.addDate(plugin, file.getName().substring(0, file.getName().lastIndexOf(".")), Integer.parseInt(year), Integer.parseInt(day));
                        }
                        else if(file.getName().contains("NldasForcingWNVAmplificationIndex"))
                        {
                            indexDates.addDate(plugin, file.getName().substring(0, file.getName().lastIndexOf(".")), Integer.parseInt(year), Integer.parseInt(day));
                        }
                        else if(file.getName().contains("NldasForcingFreezingDegreeDays"))
                        {
                            indexDates.addDate(plugin, file.getName().substring(0, file.getName().lastIndexOf(".")), Integer.parseInt(year), Integer.parseInt(day));
                        }
                        else if(file.getName().contains("NldasForcingFreezingDegreeDays"))
                        {
                            indexDates.addDate(plugin, file.getName().substring(0, file.getName().lastIndexOf(".")), Integer.parseInt(year), Integer.parseInt(day));
                        }
                    }

                }
            }

        }


        stmt = con.createStatement();
        ResultSet rs = null;

        ArrayList<Integer> dateGroupIDs;
        for(IndicesDate aDate : indexDates.getDates())
        {
            System.out.println("Handling " + aDate.plugin + "." + aDate.index + "." + aDate.year + "." + aDate.day);
            dateGroupIDs = new ArrayList<Integer>();

            // Get IndexID
            Integer indexID = Schemas.getIndexID(globalSchema, aDate.index, stmt1);
            if(indexID == null || indexID == -1)
            {
                System.err.println("ERROR: Failed to get IndexID of '" + aDate.index + "'.");
                if(rs != null) {
                    rs.close();
                }
                stmt.close();
                con.close();
                stmt1.close();
                con1.close();
                return;
            }

            // Get DateGroupIDs
            StringBuilder query = new StringBuilder("select * from \"EASTWeb\".\"DateGroup\" where \"Year\"=" + aDate.year + " and (\"DayOfYear\"=" + aDate.day);
            if(outDayCount > 1)
            {
                for(int i=inDayCount; i < outDayCount && (aDate.day+i < 367); i += inDayCount) {
                    query.append(" or \"DayOfYear\"=" + (aDate.day+i));
                }
            }
            query.append(") ");
            rs = stmt1.executeQuery(query.toString());
            if(rs != null)
            {
                while(rs.next())
                {
                    dateGroupIDs.add(rs.getInt("DateGroupID"));
                }
                rs.close();
            }

            // If no DateGroupIDs retrieved then assume no problems to correct for this date and skip to next
            if(dateGroupIDs.size() == 0) {
                continue;
            }



            // Make sure there are no rows for the current dates in the ZonalStat table
            query = new StringBuilder("delete from " + projectSchema + ".\"ZonalStat\" where \"IndexID\"=" + indexID
                    + " and (\"DateGroupID\"=" + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++)
            {
                query.append(" or \"DateGroupID\"="+dateGroupIDs.get(i));
            }
            query.append(")");
            stmt.executeUpdate(query.toString());

            // Get current values in IndicesCache table
            query = new StringBuilder("select \"Retrieved\", \"Processed\" from \"" + projectSchema + "\".\"IndicesCache\" "
                    + "where (\"DateGroupID\"=" + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++)
            {
                query.append(" or \"DateGroupID\"="+dateGroupIDs.get(i));
            }
            query.append(") and \"IndexID\"=" + indexID);
            rs = stmt.executeQuery(query.toString());

            // Correct IndicesCache table
            query = new StringBuilder("update \"" + projectSchema + "\".\"IndicesCache\" set \"Retrieved\"=false, \"Processed\"=false where \"IndexID\"=" + indexID + " and (\"DateGroupID\"=" + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++) {
                query.append(" or \"DateGroupID\"=" + dateGroupIDs.get(i));
            }
            query.append(")");
            stmt.execute(query.toString());

            GetCumulativeDays(aDate.plugin, aDate.index, projectRoot, aDate.year, aDate.day);

            // Progress message
            StringBuilder msg = new StringBuilder("Handled " + aDate.plugin + "." + aDate.index + "." + aDate.year + "." + aDate.day + " with DateGroupIDs: " + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++) {
                msg.append(", " + dateGroupIDs.get(i));
            }
            System.out.println(msg.toString());
        }
        stmt.close();
        stmt1.close();
        con1.close();

        System.out.println("Finished creating cumulative..");
    }

    public class IndicesDates
    {
        private ArrayList<IndicesDate> dates;

        public IndicesDates()
        {
            dates = new ArrayList<IndicesDate>();
        }

        public void addDate(String plugin, String index, Integer year, Integer day)
        {
            dates.add(new IndicesDate(plugin, index, year, day));
        }

        public ArrayList<IndicesDate> getDates() { return (ArrayList<IndicesDate>) dates.clone(); }
    }

    public class IndicesDate
    {
        public final int year;
        public final int day;
        public final String index;
        public final String plugin;

        public IndicesDate(String plugin, String index, int year, int day)
        {
            this.plugin = plugin;
            this.index = index;
            this.year = year;
            this.day = day;
        }
    }

    private static void GetCumulativeDays(String plugin, String prefix, String projectRoot, int year, int day)
    {

        String inputFolder = projectRoot + plugin + "\\Indices\\Output\\" + year + "\\" + String.format("%03d", day);
        File input = new File (inputFolder + "\\" + prefix + ".tif");
        Integer noDataValue = 9999;

        System.out.println(prefix);

        GdalUtils.register();
        synchronized (GdalUtils.lockObject) {

            if(!(new File(inputFolder).exists())){
                return;
            }


            Dataset inputDS = gdal.Open(input.getPath());

            int rasterX = inputDS.GetRasterXSize();
            int rasterY = inputDS.GetRasterYSize();

            double [] outputArray = new double[rasterX * rasterY];
            inputDS.GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, outputArray);

            int length = outputArray.length;
            MonthDay start = null;

            if(prefix.equals("NldasForcingOverwinteringIndex"))
            {
                start = MonthDay.of(7, 1);
            }
            else if(prefix.equals("NldasForcingLymeDiseaseIndex"))
            {
                start = MonthDay.of(1, 1);
            }
            else if(prefix.equals("NldasForcingWNVAmplificationIndex"))
            {
                start = MonthDay.of(1, 1);
            }
            else if(prefix.equals("NldasForcingFreezingDegreeDays"))
            {
                start = fdate;
            }
            else if(prefix.equals("NldasForcingFreezingDegreeDays"))
            {
                start = hdate;
            }

            double[] cumulative = GetPreviousValues(prefix, start, inputFolder);
            double degree = 0.0;

            for(int i = 0; i < length; i++)
            {
                double previousVal = 0.0;
                if(cumulative != null) {
                    previousVal = cumulative[i];
                }
                else if(cumulative == null && outputArray[i] == 9999.0)
                {
                    previousVal = outputArray[i];
                }

                // Fill value is 9999.0
                if(outputArray[i] != 9999.0 && outputArray[i] < degree) {
                    outputArray[i] = previousVal + (degree - outputArray[i]);
                } else {
                    outputArray[i] = previousVal;
                }
            }

            Dataset outputDS = gdal.GetDriverByName("GTiff").Create(
                    input.getPath(),
                    rasterX, rasterY,
                    1,
                    gdalconstConstants.GDT_Float32
                    );

            outputDS.SetGeoTransform(inputDS.GetGeoTransform());
            outputDS.SetProjection(inputDS.GetProjection());
            outputDS.SetMetadata(inputDS.GetMetadata_Dict());
            outputDS.GetRasterBand(1).WriteRaster(0, 0, rasterX, rasterY, outputArray);
            outputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
            outputDS.GetRasterBand(1).ComputeStatistics(true);

            outputDS.delete();
            inputDS.delete();
        }
    }

    private static double[] GetPreviousValues(String prefix, MonthDay start, String inputFolder)
    {
        double[] cumulative = null;
        File input = new File(inputFolder);
        File yesterdayFile = null;

        try
        {
            int year = Integer.parseInt(input.getParentFile().getName());
            DataDate startDate = new DataDate(start.getDayOfMonth(), start.getMonthValue(), year);

            // if we're currently processing the start date,
            // we don't want to continue with the previous run of heating degree days
            if(!(input.getName().equalsIgnoreCase(String.format("%03d", startDate.getDayOfYear()))))
            {
                //Get the day before's values
                File yesterdayFolder = null;
                int currentDayOfYear = Integer.parseInt(input.getName());
                LocalDate leap = LocalDate.ofYearDay(year-1, 1);

                if(currentDayOfYear > 1) {
                    yesterdayFolder = new File(input.getParentFile().getAbsolutePath() + File.separator
                            + String.format("%03d", (currentDayOfYear-1)));
                }
                else {
                    if(leap.isLeapYear())
                    {
                        yesterdayFolder = new File(input.getParentFile().getParentFile().getAbsolutePath() + File.separator
                                + String.format("%04d", (year-1)) + File.separator + "366");
                    }
                    else
                    {
                        // Previous day would be last year and day of year = 365
                        yesterdayFolder = new File(input.getParentFile().getParentFile().getAbsolutePath() + File.separator
                                + String.format("%04d", (year-1)) + File.separator + "365");
                    }
                }

                if(yesterdayFolder != null && yesterdayFolder.exists())
                {
                    for(File file : yesterdayFolder.listFiles())
                    {
                        if(file.getName().contains(prefix)) {
                            yesterdayFile = file;
                            break;
                        }
                    }
                }
            }
        }
        catch(NumberFormatException e)
        {
            ErrorLog.add(Config.getInstance(), "NldasForcingComposite.GetPreviousValues error.", e);
        }

        if(yesterdayFile != null) {
            Dataset ds = gdal.Open(yesterdayFile.getPath());
            int rasterX = ds.GetRasterXSize();
            int rasterY = ds.GetRasterYSize();

            cumulative = new double[rasterX * rasterY];
            ds.GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, cumulative);
        }

        return cumulative;
    }

    public class Task implements Runnable
    {
        private final IndicesDates indexDates;
        private final String schema;
        private final String projectName;
        private final String databaseName;
        private final String user;
        private final String password;
        private final Integer outDayCount;

        public Task(String schema, String projectName, String databaseName, String user, String password, Integer outDayCount, IndicesDates indexDates)
        {
            this.schema = schema;
            this.projectName = projectName;
            this.databaseName = databaseName;
            this.user = user;
            this.password = password;
            this.outDayCount = outDayCount;
            this.indexDates = indexDates;
        }

        @Override
        public void run()
        {
            try{
                Class.forName("org.postgresql.Driver");
                String url = "jdbc:postgresql://localhost:5432/" + databaseName;
                Connection con = DriverManager.getConnection(url, user, password);
                Statement stmt = con.createStatement();
                Connection con1 = RDS.getCentralDBconnection();
                Statement stmt1 = con1.createStatement();
                ResultSet rs = null;

                ArrayList<Integer> dateGroupIDs = new ArrayList<Integer>();
                for(IndicesDate aDate : indexDates.getDates())
                {
                    System.out.println("Handling " + aDate.plugin + "." + aDate.index + "." + aDate.year + "." + aDate.day);

                    Integer indexID = null;
                    rs = stmt1.executeQuery("select \"IndexID\" from \"EASTWeb\".\"Index\" where \"Name\"='" + aDate.index + "'");
                    if(rs != null && rs.next()) {
                        indexID = rs.getInt("IndexID");
                        rs.close();
                    }
                    else {
                        System.out.println("ERROR: Failed to get IndexID of '" + aDate.index + "'.");
                        if(rs != null) {
                            rs.close();
                        }
                        stmt.close();
                        con.close();
                        stmt1.close();
                        con1.close();
                        return;
                    }

                    StringBuilder query = new StringBuilder("select * from \"EASTWeb\".\"DateGroup\" where \"Year\"=" + aDate.year + " and (\"DayOfYear\"=" + aDate.day);
                    if(outDayCount > 1)
                    {
                        for(int i=1; i < outDayCount; i++) {
                            query.append(" or \"DayOfYear\"=" + aDate.day+1);
                        }
                    }
                    query.append(") ");
                    rs = stmt1.executeQuery(query.toString());
                    if(rs != null)
                    {
                        while(rs.next())
                        {
                            dateGroupIDs.add(rs.getInt("DateGroupID"));
                        }
                        rs.close();
                    }

                    query = new StringBuilder("select \"Retrieved\", \"Processed\" from \"" + schema + "\".\"IndicesCache\" "
                            + "where (\"DateGroupID\"=" + dateGroupIDs.get(0));
                    for(int i=1; i < dateGroupIDs.size(); i++)
                    {
                        query.append(" or \"DateGroupID\"="+dateGroupIDs.get(i));
                    }
                    query.append(") and \"IndexID\"=" + indexID);
                    rs = stmt.executeQuery(query.toString());
                }
            }catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

