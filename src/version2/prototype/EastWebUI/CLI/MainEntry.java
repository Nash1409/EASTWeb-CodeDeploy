package version2.prototype.EastWebUI.CLI;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.amazonaws.samples.DeployCode.S3;
import com.amazonaws.samples.ec2.CreateInstance;
import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.EASTWebManager;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
import version2.prototype.ProjectInfoMetaData.ProjectInfoCollection;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.Scheduler.SchedulerData;
import version2.prototype.Scheduler.SchedulerStatus;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.Schemas;

public class MainEntry {
    public static ProjectInfoFile project;

    public static void main(String[] args) {
        Boolean Flag = true;
        EASTWebManager.Start();
        //String selectedProject = ReadSubprojectFile();

        String selectedProject = "Test1_2";
        System.out.println(selectedProject);
        run(selectedProject);
        while (Flag){

            try{
                SchedulerStatus status = EASTWebManager.GetSchedulerStatus(selectedProject);

                if (status.State != null) {
                    System.out.println(status.State);
                }

                /* if (status != null && status.State != null && status.ProjectUpToDate) {
                    if (IsThereUnprocessedFile()){
                        selectedProject = ReadSubprojectFile();
                        run(selectedProject);
                    }
                    else{
                        System.out.println("Project is up to date. or completed---------1");
                        System.exit(0);
                    }
                }*/
                if (status != null && status.State != null && status.State == TaskState.STOPPED && status.ProjectUpToDate) {

                    if (IsThereUnprocessedFile()){

                        System.out.println("Done Processing project: "+ project.GetProjectName());
                        //Connection con = RDS.getLocalDBconnection();
                        DatabaseConnection con = DatabaseConnector.getConnection();
                        String proName= ""+ project.GetProjectName().trim();
                        String pluginName = ""+project.GetPlugins().get(0).GetName().trim();
                        String Pr_schema = Schemas.getSchemaName(proName,pluginName);
                        System.out.println(Pr_schema);
                        try {
                            Statement stmt = con.createStatement();
                            RDS.createPGdumpFiles(Pr_schema);
                            RDS.ImportPGdumpFile(Pr_schema,"central");
                            //Schemas.dropSchemaIfExists(Pr_schema,stmt);
                            selectedProject = ReadSubprojectFile();
                            run(selectedProject);

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        //Connection con = RDS.getLocalDBconnection();
                        DatabaseConnection con = DatabaseConnector.getConnection();
                        try {
                            String proName= ""+ project.GetProjectName().trim();
                            String pluginName = ""+project.GetPlugins().get(0).GetName().trim();
                            String Pr_schema = Schemas.getSchemaName(proName,pluginName);
                            System.out.println(Pr_schema);
                            Statement stmt = con.createStatement();

                            RDS.createPGdumpFiles(Pr_schema);
                            RDS.ImportPGdumpFile(Pr_schema,"central");
                            System.out.println("Successfuly improt project schema to cloud DB");
                            //RDS.dropSchemaIfExists("accumindex_test_2_nldasforcing",stmt);
                            stmt.close();
                            con.close();
                            EASTWebManager.StopAndShutdown();
                            CreateInstance.stopInstance(CreateInstance.GetInstanceId());
                            System.exit(0);
                            //System.out.println("Project is up to date. or completed---------2");



                        } catch (SQLException e) {e.printStackTrace();
                        }catch (IOException e) {e.printStackTrace();}
                    }
                }
                /*if (status.State != TaskState.STOPPED && status.ProjectUpToDate) {
                    System.out.println("Project is up to date. or completed---------3");
                    EASTWebManager.StopAndShutdown();
                    System.exit(0);
                }*/

                Thread.sleep(5000);
            }catch(Exception e){e.printStackTrace();}
        }
    }

    private static boolean IsThereUnprocessedFile() {
        // TODO Auto-generated method stub
        //S3 s3 = new S3();
        ArrayList<String> list = S3.GetListOfAllS3Objects();
        if (list.size() > 0) {
            System.out.println("the number of subproject Files"+list.size());
            return true;
        } else {
            System.out.println("There is no file in S3 bucket");
            return false;
        }
    }

    private static void run(String selectedProject) {
        // TODO Auto-generated method stub
        project = ProjectInfoCollection.GetProject(Config.getInstance(), selectedProject);
        try {
            SchedulerData data = new SchedulerData(project);
            EASTWebManager.LoadNewScheduler(data, false);
            EASTWebManager.StartExistingScheduler(selectedProject, true);
        }catch (PatternSyntaxException | DOMException | ParserConfigurationException | SAXException | IOException e) {
            ErrorLog.add(Config.getInstance(), "MainWindow.FileMenu problem with creating new file from Desktop.", e);
        } catch (Exception e) {
            ErrorLog.add(Config.getInstance(), "MainWindow.FileMenu problem with creating new file from Desktop.", e);
        }

    }

    private static String ReadSubprojectFile() {
        S3 s3 = new S3();
        ArrayList<String> list = S3.GetListOfAllS3Objects(); //get the xml files' name from S3 bucket

        String filename = list.get(0); //take first file in the bucket
        String Path = System.getProperty("user.dir");
        System.out.println(Path);
        if(S3.GetObject(filename,Path)){
            //Delete the file from S3 bucket after download the file in the instance
            S3.deleteFile(filename);
        }

        int i = filename.indexOf("/")+1;
        int j = filename.indexOf(".");
        String selectedProject = filename.substring(i, j);

        System.out.println(selectedProject);
        ProjectInfoFile pro = ProjectInfoCollection.GetProject(Config.getInstance(), selectedProject);
        CheckDownloadedFiles.checkPathes(pro);
        return selectedProject;

    }

}
