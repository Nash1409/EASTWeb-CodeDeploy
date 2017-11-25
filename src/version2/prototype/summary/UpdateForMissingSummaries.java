package version2.prototype.summary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.util.Schemas;

/**
 *
 */

/**
 * @author Michael DeVos
 *
 */
public class UpdateForMissingSummaries {

    private static int portNo = 5432;
    /**
     * @param args
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws ClassNotFoundException, SQLException, FileNotFoundException, InterruptedException {
        if(args.length < 10) {
            System.out.print("UpdateForMissingSummaries");
            for(String arg : args) {
                System.out.print(" " + arg);
            }
            System.out.println();
            System.out.println("usage: UpdateForMissingSummaries.bat databaseName user password schema plugin summary temporalSummaryLength inDayCount outDayCount");
            return;
        }

        final String databaseName = args[0];
        final String user = args[1];
        final String password = args[2];
        final String schema = args[3];
        final int portNo = 5432;
        final String projectName = args[4];
        final String start = args[5];
        final String compStrategy = args[6];
        final String summaryName = args[7];
        final Integer inDayCount = Integer.parseInt(args[8]);
        final Integer outDayCount = Integer.parseInt(args[9]);

        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/" + databaseName;

        Connection con = DriverManager.getConnection(url, user, password);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(start, dtf);

        final String rootDir = "C:\\eastweb\\Projects";
        findMissingSummaries(con, rootDir, "EASTWeb", portNo, schema,  projectName, startDate, summaryName, inDayCount, outDayCount, compStrategy, true);
        con.close();
    }

    /**
     * @param con  - Connection to database
     * @param rootDir  - root directory of where the project directory is located
     * @param globalSchema  - database schema name of the global schema (usually: "EASTWeb")
     * @param projectSchema  - database schema name of the project and plugin
     * @param projectName  - project name
     * @param startDate
     * @param summaryName
     * @param inDayCount  - number of days each input file represents
     * @param outDayCount  - number of days each output file represents
     * @param CompositesContinueIntoNextYear
     * @return
     * @throws NumberFormatException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws FileNotFoundException
     */
    public static boolean findMissingSummaries(Connection con, final String rootDir, final String globalSchema, final int port, final String projectSchema, final String projectName, final LocalDate startDate,
            final String summaryName, final Integer inDayCount, final Integer outDayCount, final String compStrategy, final Boolean CompositesContinueIntoNextYear) throws NumberFormatException, ClassNotFoundException, SQLException, FileNotFoundException {
        final String projectRoot;
        String downloadDir;
        boolean summariesMissing = false;

        portNo = port;

        if(rootDir.endsWith("\\") || rootDir.endsWith("/")) {
            projectRoot = rootDir + projectName + "\\";
            downloadDir = rootDir + "\\";
        } else {
            projectRoot = rootDir + "\\" + projectName + "\\";
            downloadDir = rootDir + "\\";
        }
        String[] plugins = new File(projectRoot).list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        System.out.println("Checking for missing summaries..");

        // If summary compositions continue into the next year if incomplete then calculate valid days from project start date. Otherwise, calculate valid days starting at day 1 for each year (e.g. MODIS).
        HashMap<Integer, ArrayList<Integer>> validDays = new HashMap<Integer, ArrayList<Integer>>();
        Statement stmt = con.createStatement();
        LocalDate latest = Schemas.getLatestDateInIndicesCache(globalSchema, projectSchema, stmt);
        stmt.close();

        if(CompositesContinueIntoNextYear && (compStrategy.equals("WHOWeeklyStrategy") || compStrategy.equals("CDCWeeklyStrategy")))
        {
            LocalDate start = startDate;
            if(compStrategy.equals("WHOWeeklyStrategy"))
            {
                if(startDate.getDayOfWeek().getValue() > 1)
                {
                    start = start.plusDays(8 - startDate.getDayOfWeek().getValue());
                }

            }
            else if(compStrategy.equals("CDCWeeklyStrategy"))
            {
                if(startDate.getDayOfWeek().getValue() < 7)
                {
                    start = start.plusDays(7 - startDate.getDayOfWeek().getValue());
                }

            }

            ArrayList<Integer> daysListTemp = new ArrayList<Integer>();
            int currentYear = startDate.getYear();
            for(LocalDate dt = start; dt.compareTo(latest) < 1; dt = dt.plusDays(outDayCount))
            {
                if(dt.getYear() != currentYear)
                {
                    validDays.put(currentYear, daysListTemp);
                    daysListTemp = new ArrayList<Integer>();
                    currentYear = dt.getYear();
                }

                if(!dt.plusDays(outDayCount).isAfter(latest))
                {
                    daysListTemp.add(dt.getDayOfYear());
                }
            }

            validDays.put(currentYear, daysListTemp);
        }
        else if(compStrategy.equals("GregorianMonthlyStrategy"))
        {
            ArrayList<Integer> daysListTemp = new ArrayList<Integer>();
            int currentYear = startDate.getYear();
            for(LocalDate dt = startDate; dt.compareTo(latest) < 1; dt = dt.plusDays(dt.lengthOfMonth()))
            {
                if(dt.getYear() != currentYear)
                {
                    validDays.put(currentYear, daysListTemp);
                    daysListTemp = new ArrayList<Integer>();
                    currentYear = dt.getYear();
                }

                if(!dt.plusDays(outDayCount).isAfter(latest))
                {
                    daysListTemp.add(dt.getDayOfYear());
                }
            }

            validDays.put(currentYear, daysListTemp);
        }
        else
        {
            ArrayList<Integer> standardDays = new ArrayList<Integer>();
            for(int d=1; d <= 365; d+=outDayCount){
                standardDays.add(d);
            }
            ArrayList<Integer> leapYearDays = new ArrayList<Integer>();
            for(int d=1; d <= 366; d+=outDayCount){
                leapYearDays.add(d);
            }

            ArrayList<Integer> daysListTemp;
            for(Integer yr=startDate.getYear(); yr <= latest.getYear(); yr++)
            {
                if(LocalDate.ofYearDay(yr, 1).isLeapYear())
                {
                    daysListTemp = (ArrayList<Integer>)leapYearDays.clone();
                }
                else
                {
                    daysListTemp = (ArrayList<Integer>)standardDays.clone();
                }

                if(yr == startDate.getYear() && yr == latest.getYear())
                {
                    while((daysListTemp.size() > 0) && (daysListTemp.get(0) < startDate.getDayOfYear()))
                    {
                        daysListTemp.remove(0);
                    }

                    while((daysListTemp.size() > 0) && (daysListTemp.get(daysListTemp.size()-1) > latest.getDayOfYear()))
                    {
                        daysListTemp.remove(daysListTemp.size()-1);
                    }

                    if(daysListTemp.get(daysListTemp.size()-1) + outDayCount > latest.getDayOfYear())
                    {
                        daysListTemp.remove(daysListTemp.size()-1);
                    }
                }
                else if(yr == startDate.getYear())
                {
                    while((daysListTemp.size() > 0) && (daysListTemp.get(0) < startDate.getDayOfYear()))
                    {
                        daysListTemp.remove(0);
                    }
                }
                else if(yr == latest.getYear())
                {
                    while((daysListTemp.size() > 0) && (daysListTemp.get(daysListTemp.size()-1) > latest.getDayOfYear()))
                    {
                        daysListTemp.remove(daysListTemp.size()-1);
                    }
                }

                validDays.put(yr, daysListTemp);
            }
        }

        if(validDays.isEmpty())
        {
            return true;
        }

        UpdateForMissingSummaries instance = new UpdateForMissingSummaries();
        MissingDates missingDates = instance.new MissingDates();

        ArrayList<String> fileOfMissingDatesContents = new ArrayList<String>();
        fileOfMissingDatesContents.add("schema, project, plugin, Index, Summary, Year, Day");
        ArrayList<String> fileCurrentValuesContents = new ArrayList<String>();
        fileCurrentValuesContents.add("schema, project, plugin, Index, Summary, Year, Day, Retrieved, Processed");


        /*System.out.println(validDays);*/

        for (String plugin : plugins)
        {
            String outputRoot = projectRoot + plugin + "\\Summary\\Output\\";
            String[] indices = new File(outputRoot).list(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });

            for(String index : indices)
            {
                String indexRoot = outputRoot + index + "\\";

                /* Summaries must be looped through outside of class because each summary will
                 * have a different outDayCount and will cause incorrect missing dates to be
                 * added and reported
                 */

                //            String[] summaries = new File(indexRoot).list(new FilenameFilter(){
                //                @Override
                //                public boolean accept(File dir, String name) {
                //                    return new File(dir, name).isDirectory();
                //                }
                //            });

                String summaryRoot = indexRoot + summaryName + "\\";
                String[] years = new File(summaryRoot).list(new FilenameFilter(){
                    @Override
                    public boolean accept(File dir, String name) {
                        return new File(dir, name).isDirectory();
                    }
                });

                // Check if any year directories are missing.
                boolean missing = false;
                for(Integer year : validDays.keySet())
                {

                    for(String yearStr : years)
                    {
                        if(yearStr.equals(year.toString()))
                        {
                            missing = false;
                            break;
                        }

                        missing = true;
                    }

                    if(missing)
                    {
                        System.out.println("Current Year Missing...");
                        // If execution reaches here then current year is missing
                        String yearRoot = summaryRoot + year + "\\";
                        for(Integer day : validDays.get(year))
                        {
                            missingDates.addDate(plugin, index, summaryName, year, day);
                            fileOfMissingDatesContents.add(projectSchema + ", " + projectName + ", " + plugin + ", " + index + ", " + summaryName + ", " + year + ", " + day);
                            System.out.println("Missing '" + yearRoot + String.format("%03d", day) + ".csv");
                        }
                    }


                }

                // Check existing year directories for any day files that are missing.
                for(String year : years)
                {
                    String yearRoot = summaryRoot + year + "\\";
                    //                    String[] days = new File(yearRoot).list(new FilenameFilter(){
                    //                        @Override
                    //                        public boolean accept(File dir, String name) {
                    //                            return new File(dir, name).isFile();
                    //                        }
                    //                    });

                    String downloadRoot = downloadDir.replace("\\Projects\\", "\\Downloads\\") + plugin + "\\data\\" + year + "\\";
                    File[] days = new File(downloadRoot).listFiles();
                    Integer originDate;

                    if(days != null)
                    {
                        originDate = Integer.parseInt(days[0].getName());
                    }
                    else
                    {
                        originDate = startDate.getDayOfYear();
                    }

                    for(Integer validDay : validDays.get(Integer.parseInt(year)))
                    {
                        if((year.equals("" + startDate.getYear()) && validDay < originDate) ||
                                (plugin.equals("ModisNBARV6") && ((year.equals("2001") && (validDay.equals(174) || validDay.equals(175) || validDay.equals(176) || validDay.equals(177))) ||
                                        (year.equals("2012") && validDay.equals(212)))))
                        {
                            continue;
                        }

                        if(!new File(yearRoot + String.format("%03d", validDay) + ".csv").exists())
                        {
                            missingDates.addDate(plugin, index, summaryName, Integer.parseInt(year), validDay);
                            fileOfMissingDatesContents.add(projectSchema + ", " + projectName + ", " + plugin + ", " + index + ", " + summaryName + ", " + year + ", " + validDay);
                            System.out.println("Missing '" + yearRoot + String.format("%03d", validDay) + ".csv");
                        }
                    }
                }

            }

        }
        //		ThreadPoolExecutor executor = new ThreadPoolExecutor(24,24,0,TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        //		executor.allowCoreThreadTimeOut(true);
        //
        //		ArrayList<ArrayList<String>> fileOfMissingDatesContents = new ArrayList<ArrayList<String>>();
        //		for(int i=0; i<24; i++) {
        //			fileOfMissingDatesContents.add(new ArrayList<String>());
        //		}
        //
        //		int sliceSize = missingDates.getDates().size() / 24;
        //		for(int i=0; i < 23; i++)
        //		{
        //			executor.execute(new Task(schema, projectName, databaseName, user, password, outDayCount, missingDates.getDates().subList(i, sliceSize), fileOfMissingDatesContents.get(i));
        //		}
        //
        //
        //		executor.shutdown();
        //		executor.awaitTermination(10, TimeUnit.MINUTES);

        stmt = con.createStatement();
        ResultSet rs = null;
        Connection con1 = RDS.getCentralDBconnection();
        Statement stmt1 = con1.createStatement();
        ArrayList<Integer> dateGroupIDs;
        for(MissingDate aDate : missingDates.getDates())
        {
            System.out.println("Handling " + aDate.plugin + "." + aDate.index + "." + aDate.summary + "." + aDate.year + "." + aDate.day);
            dateGroupIDs = new ArrayList<Integer>();

            // Get IndexID
            //Schemas.getIndexID(globalSchema, aDate.index, stmt1);
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
                return false;
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
                summariesMissing = true;
                continue;
            }

            // Get projectSummaryID
            Integer projectSummaryID = Schemas.getProjectSummaryID(globalSchema, projectName, aDate.summaryIDNum, stmt1);

            // Make sure there are no rows for the current dates in the ZonalStat table
            query = new StringBuilder("delete from " + projectSchema + ".\"ZonalStat\" where \"ProjectSummaryID\"=" + projectSummaryID + " and \"IndexID\"=" + indexID
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
            if(rs != null)
            {
                while(rs.next())
                {
                    fileCurrentValuesContents.add(projectSchema + ", " + projectName + ", " + aDate.plugin + ", " + aDate.index + ", " + aDate.summary + ", " + aDate.year + ", " + aDate.day + ", " + rs.getBoolean("Retrieved") + ", " + rs.getBoolean("Processed"));
                }
                rs.close();
            }

            // Correct IndicesCache table
            query = new StringBuilder("update \"" + projectSchema + "\".\"IndicesCache\" set \"Retrieved\"=false, \"Processed\"=false where \"IndexID\"=" + indexID + " and (\"DateGroupID\"=" + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++) {
                query.append(" or \"DateGroupID\"=" + dateGroupIDs.get(i));
            }
            query.append(")");
            stmt.execute(query.toString());

            // Progress message
            StringBuilder msg = new StringBuilder("Handled " + aDate.plugin + "." + aDate.index + "." + aDate.summary + "." + aDate.year + "." + aDate.day + " with DateGroupIDs: " + dateGroupIDs.get(0));
            for(int i=1; i < dateGroupIDs.size(); i++) {
                msg.append(", " + dateGroupIDs.get(i));
            }
            System.out.println(msg.toString());
        }
        stmt.close();
        stmt1.close();

        //		if(rs != null)
        //		{
        //			while(rs.next())
        //			{
        //				f = new File(rs.getString("FilePath"));
        //				if(!f.exists())
        //				{
        //					fileContents.add(schema + "," + rs.getString("Index") + "," + rs.getString("Year") + "," + rs.getString("DayOfYear"));
        //					System.out.println("Missing '" + rs.getString("FilePath") + "'.");
        //				}
        //			}
        //			rs.close();
        //		}

        LocalDateTime temp = LocalDateTime.now();
        String timestamp = LocalDate.now().getYear() + "_" + LocalDate.now().getMonthValue() + "_" + LocalDate.now().getDayOfMonth() + "_" + String.format("%02d", temp.getHour())
        + String.format("%02d", temp.getMinute()) + String.format("%02d", temp.getSecond());

        if(fileOfMissingDatesContents.size() > 1) {
            summariesMissing = false;
            File f = new File(projectRoot + projectName + "_MissingFilesZonalStat_" + timestamp + ".csv");
            if(f.exists()) { f.delete(); }
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            for(String line : fileOfMissingDatesContents) {
                ps.println(line);
            }
            ps.flush();
            ps.close();
        }
        if(fileCurrentValuesContents.size() > 1) {
            File f = new File(projectRoot + projectName + "_MissingFilesCurrentValuesIndicesCache_" + timestamp + ".csv");
            if(f.exists()) { f.delete(); }
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            for(String line : fileCurrentValuesContents) {
                ps.println(line);
            }
            ps.flush();
            ps.close();
        }

        System.out.println("Finished checking for missing summaries..");
        return summariesMissing;
    }

    public class MissingDates
    {
        private ArrayList<MissingDate> dates;

        public MissingDates()
        {
            dates = new ArrayList<MissingDate>();
        }

        public void addDate(String plugin, String index, String summary, Integer year, Integer day)
        {
            dates.add(new MissingDate(plugin, index, summary, year, day));
        }

        public ArrayList<MissingDate> getDates() { return (ArrayList<MissingDate>) dates.clone(); }
    }

    public class MissingDate
    {
        public final int year;
        public final int day;
        public final String index;
        public final String summary;
        public final Integer summaryIDNum;
        public final String plugin;

        public MissingDate(String plugin, String index, String summary, int year, int day)
        {
            this.plugin = plugin;
            this.summary = summary;
            this.index = index;
            this.year = year;
            this.day = day;

            // Extract summaryIDNum from summary directory name
            summaryIDNum = Integer.parseInt(summary.substring("Summary ".length()));
        }
    }

    public class Task implements Runnable
    {
        private final MissingDates missingDates;
        private final String schema;
        private final String projectName;
        private final String databaseName;
        private final String user;
        private final String password;
        private final Integer outDayCount;
        private ArrayList<String> fileOfMissingDatesContents;

        public Task(String schema, String projectName, String databaseName, String user, String password, Integer outDayCount, MissingDates missingDates, ArrayList<String> fileOfMissingDatesContents)
        {
            this.schema = schema;
            this.projectName = projectName;
            this.databaseName = databaseName;
            this.user = user;
            this.password = password;
            this.outDayCount = outDayCount;
            this.missingDates = missingDates;
            this.fileOfMissingDatesContents = fileOfMissingDatesContents;
            this.fileOfMissingDatesContents.add("schema, project, plugin, Index, Summary, Year, Day, Retrieved, Processed");
        }

        @Override
        public void run()
        {
            try{
                Class.forName("org.postgresql.Driver");
                String url = "jdbc:postgresql://localhost:" + portNo + "/" + databaseName;
                Connection con = DriverManager.getConnection(url, user, password);
                Statement stmt = con.createStatement();
                Connection con1 = RDS.getCentralDBconnection();
                Statement stmt1 = con1.createStatement();
                ResultSet rs = null;

                ArrayList<Integer> dateGroupIDs = new ArrayList<Integer>();
                for(MissingDate aDate : missingDates.getDates())
                {
                    System.out.println("Handling " + aDate.plugin + "." + aDate.index + "." + aDate.summary + "." + aDate.year + "." + aDate.day);

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
                    if(rs != null)
                    {
                        while(rs.next())
                        {
                            fileOfMissingDatesContents.add(schema + ", " + projectName + ", " + aDate.plugin + ", " + aDate.index + ", " + aDate.summary + ", " + aDate.year + ", " + aDate.day + ", " + rs.getBoolean("Retrieved") + ", " + rs.getBoolean("Processed"));
                        }
                        rs.close();
                    }
                    //			query = new StringBuilder("update \"" + schema + "\".\"IndicesCache\" set \"Retrieved\"=false, \"Processed\"=false where \"IndexID\"=" + indexID + " and (\"DateGroupID\"=" + dateGroupIDs.get(0));
                    //			for(int i=1; i < dateGroupIDs.size(); i++) {
                    //				query.append(" or \"DateGroupID\"=" + dateGroupIDs.get(i));
                    //			}
                    //			query.append(")");
                    //			stmt.execute(query.toString());
                }
            }catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
