package version2.prototype.EastWebUI.ProjectInformationUI;

import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.EastWebUI.DocumentBuilderInstance;
import version2.prototype.EastWebUI.MainWindow.MainWindowEvent;
import version2.prototype.EastWebUI.MainWindow.MainWindowListener;
import version2.prototype.EastWebUI.PluginIndicesUI.AssociatePluginPage;
import version2.prototype.EastWebUI.PluginIndicesUI.IndicesEventObject;
import version2.prototype.EastWebUI.PluginIndicesUI.IndicesListener;
import version2.prototype.EastWebUI.SummaryUI.AssociateSummaryPage;
import version2.prototype.EastWebUI.SummaryUI.SummaryEventObject;
import version2.prototype.EastWebUI.SummaryUI.SummaryListener;
import version2.prototype.ProjectInfoMetaData.ProjectInfoCollection;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.joda.time.DateTime;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.toedter.calendar.JDateChooser;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

public class ProjectInformationPage {
    private ArrayList<String> globalModisTiles;
    private boolean isEditable;
    private MainWindowEvent mainWindowEvent;

    private JFrame frame;
    private JDateChooser startDate;
    private JTextField projectName;
    private JTextField workingDirectory;
    private JTextField maskFile;
    private JTextField pixelSize;
    private JComboBox<String> timeZoneComboBox;
    private JComboBox<String> reSamplingComboBox;
    private JComboBox<String> projectCollectionComboBox;
    private JTextField masterShapeTextField;
    private JCheckBox isClippingCheckBox;
    private JCheckBox chckbxBigDataMode;
    private JDateChooser freezingDateChooser;
    private JTextField coolingTextField;
    private JDateChooser heatingDateChooser;
    private JTextField heatingTextField;

    private DefaultListModel<String> listOfAddedPluginModel;
    private DefaultListModel<String> summaryListModel;

    private Object[] possibilities = {"Monthly", "Yearly"};
    private int BigDataMode = possibilities.length - 1; // By default the Big Data mode is Yearly
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    ProjectInformationPage window =  new ProjectInformationPage(true, null);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "ProjectInformationPage.main problem with running a ProjectInformationPage window.", e);
                }
            }
        });
    }

    /**
     * Create the application.
     * @throws ParseException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public ProjectInformationPage(boolean isEditable,  MainWindowListener l) throws Exception{
        frame = new JFrame();
        globalModisTiles = new ArrayList<String>();
        mainWindowEvent = new MainWindowEvent();

        this.isEditable = isEditable;
        mainWindowEvent.addListener(l);
        frame.setVisible(true);

        initialize();
        DocumentBuilderInstance.ClearInstance();
    }

    /**
     * Initialize the contents of the frame.
     * @throws ParseException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    private void initialize() throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        frame.setBounds(100, 100, 955, 858);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);

        JLabel lblProjectInformation = new JLabel("Project Information ");
        lblProjectInformation.setFont(new Font("Courier", Font.BOLD,25));
        lblProjectInformation.setBounds(10, 11, 315, 32);
        frame.getContentPane().add(lblProjectInformation);

        CreateNewProjectButton();
        PopulatePluginList();
        BasicProjectInformation();
        ProjectInformation();
        SummaryInformation();
        UIConstrain();
    }

    // button for create/save project
    private void CreateNewProjectButton() {
        JButton saveButton = new JButton("");
        saveButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/save_32.png")));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                int months, years;
                DateTime f, l, temp;
                DateTime start = new DateTime(startDate.getDate());
                DateTime now = new DateTime();
                List<String> startDates = new ArrayList<>();
                List<String> endDates = new ArrayList<>();
                try {
                    if(chckbxBigDataMode.isSelected()) {
                        switch(BigDataMode){
                        case 0:
                            years = now.getYear() - start.getYear() + 1;
                            f = start;
                            l = start;
                            for(int i=0; i < years; i++){
                                temp = l;
                                while(l.isBefore(now.toInstant()) && l.getYear() <= f.getYear()){
                                    l = l.plusMonths(1);
                                }
                                l = l.minusMonths(1);
                                months = l.getMonthOfYear() - f.getMonthOfYear() + 1;
                                l = temp;
                                for(int j=0; j < months; j++){
                                    while(l.isBefore(now.toInstant()) && l.getMonthOfYear() <= f.getMonthOfYear() && l.getYear() <= f.getYear()){
                                        l = l.plusDays(1);
                                    }
                                    l = l.minusDays(1);
                                    f = f.withHourOfDay(0);
                                    f = f.withMinuteOfHour(0);
                                    f = f.withSecondOfMinute(0);
                                    l = l.withHourOfDay(23);
                                    l = l.withMinuteOfHour(59);
                                    l = l.withSecondOfMinute(59);
                                    startDates.add(f.toString("EEE MMM dd HH:mm:ss zzz yyyy"));
                                    endDates.add(l.toString("EEE MMM dd HH:mm:ss zzz yyyy"));
                                    f = l.plusDays(1);
                                }
                                l = l.plusDays(1);
                            }
                            CreateSubProjects(startDates,endDates);
                            break;
                        default:
                            years = now.getYear() - start.getYear();
                            years++;
                            f = start;
                            l = start;
                            for(int i=0; i<years; i++){
                                while(l.isBefore(now.toInstant()) && l.getYear() <= f.getYear()){
                                    l = l.plusDays(1);
                                }
                                l = l.minusDays(1);
                                f = f.withHourOfDay(0);
                                f = f.withMinuteOfHour(0);
                                f = f.withSecondOfMinute(0);
                                l = l.withHourOfDay(23);
                                l = l.withMinuteOfHour(59);
                                l = l.withSecondOfMinute(59);
                                startDates.add(f.toString("EEE MMM dd HH:mm:ss zzz yyyy"));
                                endDates.add(l.toString("EEE MMM dd HH:mm:ss zzz yyyy"));
                                f = l.plusDays(1);
                            }
                            CreateSubProjects(startDates,endDates);
                            break;
                        }
                    } else {
                        CreateNewProject();
                    }
                    JOptionPane.showMessageDialog(frame, "Project was saved");
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                } catch (TransformerException e) {
                    e.printStackTrace();
                }
            }
        });
        saveButton.setToolTipText("Save Project");
        saveButton.setBounds(907, 11, 32, 32);
        frame.getContentPane().add(saveButton);
    }

    // Populate UI for Plug-in
    private void PopulatePluginList() {
        listOfAddedPluginModel = new DefaultListModel<String>();

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Plugin List", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBounds(10, 58, 929, 351);
        frame.getContentPane().add(panel);
        panel.setLayout(null);

        JButton addPluginButton = new JButton("");
        addPluginButton.setBounds(10, 31, 37, 30);
        panel.add(addPluginButton);
        addPluginButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/action_add_16xLG.png")));
        addPluginButton.setToolTipText("Add Plugin");

        JButton deletePluginButton = new JButton("");
        deletePluginButton.setBounds(57, 31, 37, 30);
        panel.add(deletePluginButton);
        deletePluginButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/trashCan.png")));
        deletePluginButton.setToolTipText("Delete Plugin");

        final JList<String> listOfAddedPlugin = new JList<String>(listOfAddedPluginModel);
        listOfAddedPlugin.setBorder(new EmptyBorder(10,10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(listOfAddedPlugin);
        scrollPane.setBounds(10, 65, 909, 275);
        panel.add(scrollPane);
        deletePluginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {deleteSelectedPlugin(listOfAddedPlugin);}
        });
        addPluginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    new AssociatePluginPage(new indicesListenerImplementation(), globalModisTiles,frame);
                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "ProjectInformationPage.PopulatePluginList problem with creating new AssociatePluginPage.", e);
                }
            }
        });
    }

    // populate Basic Information panel
    private void BasicProjectInformation() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Basic Project Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBounds(10, 420, 358, 398);
        frame.getContentPane().add(panel);
        panel.setLayout(null);

        JLabel startDateLabel = new JLabel("Start Date:");
        startDateLabel.setBounds(10, 30, 130, 15);
        panel.add(startDateLabel);
        startDate = new JDateChooser ();
        startDate.setBounds(150, 20, 200, 30);
        panel.add(startDate);

        JLabel projectNameLabel = new JLabel("Project Name: ");
        projectNameLabel.setBounds(10, 60, 130, 15);
        panel.add(projectNameLabel);
        projectName = new JTextField();
        projectName.setBounds(150, 50, 200, 30);
        panel.add(projectName);
        projectName.setColumns(10);

        JLabel workingDirLabel = new JLabel("Working Dir: ");
        workingDirLabel.setBounds(10, 90, 130, 15);
        panel.add(workingDirLabel);
        workingDirectory = new JTextField();
        workingDirectory.setColumns(10);
        workingDirectory.setBounds(150, 80, 150, 30);
        panel.add(workingDirectory);

        JButton workingDirBrowsebutton = new JButton();
        workingDirBrowsebutton.setOpaque(false);
        workingDirBrowsebutton.setContentAreaFilled(false);
        workingDirBrowsebutton.setBorderPainted(false);
        workingDirBrowsebutton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        workingDirBrowsebutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {chooseWorkingDir();}
        });
        workingDirBrowsebutton.setBounds(320, 80, 30, 25);
        panel.add(workingDirBrowsebutton);

        JLabel maskingFileLabel = new JLabel("Masking File:");
        maskingFileLabel.setBounds(10, 120, 130, 15);
        panel.add(maskingFileLabel);
        maskFile = new JTextField();
        maskFile.setColumns(10);
        maskFile.setBounds(150, 110, 150, 30);
        panel.add(maskFile);

        JButton maskFileBrowseButton = new JButton();
        maskFileBrowseButton.setOpaque(false);
        maskFileBrowseButton.setContentAreaFilled(false);
        maskFileBrowseButton.setBorderPainted(false);
        maskFileBrowseButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        maskFileBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {chooseMaskingFile();}
        });
        maskFileBrowseButton.setBounds(320, 110, 30, 25);
        panel.add(maskFileBrowseButton);

        //        JLabel lblResolution = new JLabel("Masking Resolution:");
        //        lblResolution.setBounds(10, 150, 130, 15);
        //        panel.add(lblResolution);
        //
        //        resolutionTextField = new JTextField();
        //        resolutionTextField.setBounds(150, 140, 200, 30);
        //        panel.add(resolutionTextField);
        //        resolutionTextField.setColumns(10);

        final JLabel chmasterShapeFileLabel = new JLabel("Master shape file:");
        chmasterShapeFileLabel.setBounds(10, 150, 130, 15);
        panel.add(chmasterShapeFileLabel);
        masterShapeTextField = new JTextField();
        masterShapeTextField.setBounds(150, 140, 150, 30);
        panel.add(masterShapeTextField);
        masterShapeTextField.setColumns(10);

        final JButton masterShapeFileBrowseButton = new JButton();
        masterShapeFileBrowseButton.setOpaque(false);
        masterShapeFileBrowseButton.setContentAreaFilled(false);
        masterShapeFileBrowseButton.setBorderPainted(false);
        masterShapeFileBrowseButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        masterShapeFileBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {chooseMasterShapeFile();}
        });
        masterShapeFileBrowseButton.setBounds(320, 140, 30, 25);
        panel.add(masterShapeFileBrowseButton);

        JLabel lblTimeZone = new JLabel("Time Zone:");
        lblTimeZone.setBounds(10, 300, 130, 15);
        panel.add(lblTimeZone);
        timeZoneComboBox = new JComboBox<String>();
        timeZoneComboBox.setBounds(150, 290, 200, 30);
        for (String id : TimeZone.getAvailableIDs()) {
            TimeZone zone = TimeZone.getTimeZone(id);
            int offset = zone.getRawOffset()/1000;
            int hour = offset/3600;
            int minutes = (offset % 3600)/60;
            String timeZoneString = String.format("(GMT%+d:%02d) %s", hour, minutes, id);
            timeZoneComboBox.addItem(timeZoneString);
        }
        panel.add(timeZoneComboBox);

        JLabel lblFreezingStartDate = new JLabel("Freezing Start Date: ");
        lblFreezingStartDate.setBounds(10, 180, 130, 15);
        panel.add(lblFreezingStartDate);
        freezingDateChooser = new JDateChooser();
        freezingDateChooser.setDateFormatString("MMM d");
        freezingDateChooser.setBounds(150, 170, 200, 30);
        panel.add(freezingDateChooser);

        JLabel lblHeatingStartDate = new JLabel("Heating Start Date:");
        lblHeatingStartDate.setBounds(10, 240, 130, 15);
        panel.add(lblHeatingStartDate);
        heatingDateChooser = new JDateChooser();
        heatingDateChooser.setDateFormatString("MMM d");
        heatingDateChooser.setBounds(150, 230, 200, 30);
        panel.add(heatingDateChooser);

        JLabel lblCoolingDegreeThreshold = new JLabel("Cooling degree:");
        lblCoolingDegreeThreshold.setToolTipText("Cooling degree threshold");
        lblCoolingDegreeThreshold.setBounds(10, 210, 130, 15);
        panel.add(lblCoolingDegreeThreshold);
        coolingTextField = new JTextField();
        coolingTextField.setBounds(150, 200, 100, 30);
        panel.add(coolingTextField);
        coolingTextField.setColumns(10);
        JLabel lblCelcius = new JLabel("Celsius");
        lblCelcius.setBounds(275, 210, 50, 15);
        panel.add(lblCelcius);

        JLabel lblHeatingDegreeThreshold = new JLabel("Heating degree");
        lblHeatingDegreeThreshold.setToolTipText("Heating degree threshold");
        lblHeatingDegreeThreshold.setBounds(10, 270, 130, 15);
        panel.add(lblHeatingDegreeThreshold);
        heatingTextField = new JTextField();
        heatingTextField.setBounds(150, 260, 100, 30);
        panel.add(heatingTextField);
        heatingTextField.setColumns(10);
        JLabel lblCelsius = new JLabel("Celsius");
        lblCelsius.setBounds(275, 270, 50, 15);
        panel.add(lblCelsius);

        JLabel lblClipping = new JLabel("Clipping:");
        lblClipping.setBounds(10, 330, 130, 15);
        panel.add(lblClipping);
        isClippingCheckBox = new JCheckBox("");
        isClippingCheckBox.setBounds(150, 330, 200, 15);
        panel.add(isClippingCheckBox);

        JLabel lblBigDataMode = new JLabel("Big Data mode:");
        lblBigDataMode.setBounds(10, 356, 130, 14);
        panel.add(lblBigDataMode);

        chckbxBigDataMode = new JCheckBox("");
        chckbxBigDataMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(chckbxBigDataMode.isSelected()) {
                        String s = (String)JOptionPane.showInputDialog(
                                frame,
                                "How do you want the data to be split:",
                                "Advanced Configuration",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                possibilities,
                                possibilities[BigDataMode]);

                        //If a string was returned, say so.
                        if ((s != null) && (s.length() > 0)) {
                            for(int i = 0; i < possibilities.length;i++) {
                                if(s.equals(possibilities[i])){
                                    BigDataMode = i;
                                    break;
                                }
                            }
                        }

                        //If you're here, the return value was null/empty.
                        //setLabel("Come on, finish the sentence!");
                        //new ProjectInformationAdvancedMode(BigDataMode,frame);
                        //ProjectInformationAdvancedMode p = new ProjectInformationAdvancedMode(BigDataMode);
                        //f7.setModalityExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDED);
                        //p.setVisible(true);
                        //JOptionPane.showMessageDialog(null, ""+p.GetBigDataMode());
                    }
                } catch (Exception ex) {
                    ErrorLog.add(Config.getInstance(), "ProjectInformationPage.ProjectInformationAdvancedMode problem with configuring the Big Data mode.", ex);
                }
            }
        });
        chckbxBigDataMode.setBounds(150, 352, 200, 23);
        panel.add(chckbxBigDataMode);
    }

    // populate project information UI
    private void ProjectInformation() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBorder(new TitledBorder(null, "Projection Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBounds(365, 420, 297, 398);
        frame.getContentPane().add(panel);

        JLabel reSamplingLabel = new JLabel("Re-sampling Type:");
        reSamplingLabel.setBounds(12, 23, 109, 14);
        panel.add(reSamplingLabel);
        reSamplingComboBox = new JComboBox<String>();
        reSamplingComboBox.setBounds(125, 20, 150, 30);
        reSamplingComboBox.addItem("NEAREST_NEIGHBOR");
        reSamplingComboBox.addItem("BILINEAR");
        reSamplingComboBox.addItem("CUBIC_CONVOLUTION");
        panel.add(reSamplingComboBox);

        JLabel pixelSizeLabel = new JLabel("Pixel size meters:");
        pixelSizeLabel.setBounds(12, 58, 109, 14);
        panel.add(pixelSizeLabel);
        pixelSize = new JTextField();
        pixelSize.setColumns(10);
        pixelSize.setBounds(125, 50, 150, 30);
        panel.add(pixelSize);
    }

    // populate summary information UI
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void SummaryInformation() {
        summaryListModel = new DefaultListModel();

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(null);
        summaryPanel.setBorder(new TitledBorder(null, "Summary Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        summaryPanel.setBounds(658, 420, 281, 398);
        frame.getContentPane().add(summaryPanel);

        JButton editSummaryButton = new JButton();
        editSummaryButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/action_add_16xLG.png")));
        editSummaryButton.setToolTipText("Add summary");
        editSummaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { new AssociateSummaryPage(new summaryListenerImplementation(),frame);}
        });
        editSummaryButton.setBounds(205, 11, 33, 30);
        summaryPanel.add(editSummaryButton);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 52, 261, 335);
        summaryPanel.add(scrollPane);

        final JList summaryList = new JList(summaryListModel);
        scrollPane.setViewportView(summaryList);
        summaryList.setLayoutOrientation(JList.HORIZONTAL_WRAP);

        JButton deleteSummaryButton = new JButton();
        deleteSummaryButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/trashCan.png")));
        deleteSummaryButton.setToolTipText("Delete Selected Summary");
        deleteSummaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {deleteSelectedSummary(summaryList);}
        });
        deleteSummaryButton.setBounds(238, 11, 33, 30);
        summaryPanel.add(deleteSummaryButton);
    }

    /**
     * constrains the UI base on editable tag
     */
    private void UIConstrain() {
        if(!isEditable){
            JLabel lblProject = new JLabel("Project: ");
            lblProject.setBounds(616, 25, 46, 14);
            frame.getContentPane().add(lblProject);

            projectCollectionComboBox = new JComboBox<String>();
            projectCollectionComboBox.setBounds(668, 23, 229, 20);
            projectCollectionComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {PopulateProjectInfo();}
            });
            frame.getContentPane().add(projectCollectionComboBox);
            for(ProjectInfoFile project : ProjectInfoCollection.GetAllProjectInfoFiles(Config.getInstance())){
                projectCollectionComboBox.addItem(project.GetProjectName());
            }
        }
    }

    // Create or save new project
    private void CreateNewProject() throws TransformerException{
        try {
            // root elements
            Document doc = DocumentBuilderInstance.Instance().GetDocument();
            Element projectInfo = doc.createElement("ProjectInfo");
            doc.appendChild(projectInfo);

            // Plugin elements
            Element plugins = doc.createElement("Plugins");
            projectInfo.appendChild(plugins);

            //list of plugin associate to project
            for(Object item:listOfAddedPluginModel.toArray()){
                Element plugin = doc.createElement("Plugin");

                // set attribute to staff element
                Attr attr = doc.createAttribute("name");
                String noFormat = item.toString().replaceAll("<html>Plugin: ","");
                noFormat = noFormat.replaceAll("<br>Indices: ", "");
                noFormat = noFormat.replaceAll("<br>Quality: ","");
                noFormat = noFormat.replaceAll("<br>Modis Tiles: ","");
                noFormat = noFormat.replaceAll("</html>", "");
                noFormat = noFormat.replaceAll("</span>", "");
                noFormat = noFormat.replaceAll("<span>", "");

                String[] array = noFormat.split(";");

                attr.setValue(array[0].toString());
                plugin.setAttributeNode(attr);

                if(array.length < 3){
                    // start Date
                    Element qc = doc.createElement("QC");
                    qc.appendChild(doc.createTextNode(array[1].toString()));
                    plugin.appendChild(qc);
                }
                else{
                    for(int i = 1; i < array.length -1; i++){

                        if(isValueAModisTile(array, i))
                        {
                            Element modisTile = doc.createElement("ModisTile");
                            modisTile.appendChild(doc.createTextNode(array[i].toString()));
                            plugin.appendChild(modisTile);
                        }
                        else
                        {
                            Element indices = doc.createElement("Indices");
                            indices.appendChild(doc.createTextNode(array[i].toString()));
                            plugin.appendChild(indices);
                        }
                    }

                    Element qc = doc.createElement("QC");
                    qc.appendChild(doc.createTextNode(array[array.length - 1].toString()));
                    plugin.appendChild(qc);

                    for(int i = 1; i < array.length -1; i++){

                    }
                }

                // add a new node for plugin element
                plugins.appendChild(plugin);
            }

            // start Date
            Element startDate = doc.createElement("StartDate");
            startDate.appendChild(doc.createTextNode(this.startDate.getDate().toString()));
            projectInfo.appendChild(startDate);

            // project name
            Element projectName = doc.createElement("ProjectName");
            projectName.appendChild(doc.createTextNode(this.projectName.getText()));
            projectInfo.appendChild(projectName);

            // working directory
            Element workingDirectory = doc.createElement("WorkingDir");
            workingDirectory.appendChild(doc.createTextNode(this.workingDirectory.getText()));
            projectInfo.appendChild(workingDirectory);

            // masking file
            Element masking = doc.createElement("Masking");
            projectInfo.appendChild(masking);

            Element maskingFile = doc.createElement("File");
            maskingFile.appendChild(doc.createTextNode(maskFile.getText()));
            masking.appendChild(maskingFile);

            Dataset hDataset;
            double[] adfGeoTransform = new double[6];
            String pszFilename = maskFile.getText();
            String res="";

            gdal.AllRegister();

            hDataset = gdal.Open(pszFilename, gdalconstConstants.GA_ReadOnly);

            if (hDataset != null)
            {
                hDataset.GetGeoTransform(adfGeoTransform);
                {
                    if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
                        res = "" + ((int) (adfGeoTransform[1] + 0.5));
                    }
                }

                hDataset.delete();
            }

            Element resolution = doc.createElement("Resolution");
            resolution.appendChild(doc.createTextNode(res));
            masking.appendChild(resolution);

            Element masterShapeFile = doc.createElement("MasterShapeFile");
            masterShapeFile.appendChild(doc.createTextNode(masterShapeTextField.getText()));
            projectInfo.appendChild(masterShapeFile);

            Element timeZone = doc.createElement("TimeZone");
            timeZone.appendChild(doc.createTextNode(String.valueOf(timeZoneComboBox.getSelectedItem())));
            projectInfo.appendChild(timeZone);

            Element isClipping = doc.createElement("Clipping");
            isClipping.appendChild(doc.createTextNode(String.valueOf(isClippingCheckBox.isSelected())));
            projectInfo.appendChild(isClipping);

            // Freezing start Date
            Element freezingstartDate = doc.createElement("FreezingDate");
            freezingstartDate.appendChild(doc.createTextNode(freezingDateChooser.getDate().toString()));
            projectInfo.appendChild(freezingstartDate);

            // Cooling degree value
            Element coolingDegree = doc.createElement("CoolingDegree");
            coolingDegree.appendChild(doc.createTextNode(coolingTextField.getText()));
            projectInfo.appendChild(coolingDegree);

            // Heating start Date
            Element heatingstartDate = doc.createElement("HeatingDate");
            heatingstartDate.appendChild(doc.createTextNode(heatingDateChooser.getDate().toString()));
            projectInfo.appendChild(heatingstartDate);

            // heating degree value
            Element heatingDegree = doc.createElement("HeatingDegree");
            heatingDegree.appendChild(doc.createTextNode(heatingTextField.getText()));
            projectInfo.appendChild(heatingDegree);

            // re-sampling
            Element reSampling = doc.createElement("ReSampling");
            reSampling.appendChild(doc.createTextNode(String.valueOf(reSamplingComboBox.getSelectedItem())));
            projectInfo.appendChild(reSampling);

            //datum
            Element pixelSize = doc.createElement("PixelSize");
            pixelSize.appendChild(doc.createTextNode(String.valueOf(this.pixelSize.getText())));
            projectInfo.appendChild(pixelSize);

            //list of summary tiles
            Element summaries = doc.createElement("Summaries");
            projectInfo.appendChild(summaries);

            int summaryCounter = 1;
            for(Object item:summaryListModel.toArray()){
                Element summary = doc.createElement("Summary");
                summary.setAttribute("ID", String.valueOf(summaryCounter));
                summaryCounter ++;

                summary.appendChild(doc.createTextNode(item.toString()));
                summaries.appendChild(summary);
            }

            if(ProjectInfoCollection.WriteProjectToFile(doc, this.projectName.getText())){
                System.out.println("File saved!");
                mainWindowEvent.fire();
            }else{
                System.out.println("Error in saving");
            }
        } catch (ParserConfigurationException e) {
            ErrorLog.add(Config.getInstance(), "ProjectInformationPage.CreateNewProject problem with creating new project.", e);
        }
    }

    // Create or save new project
    private void CreateSubProjects(List<String> startDates, List<String> endDates) throws TransformerException{
        try {
            // root elements
            Document doc = DocumentBuilderInstance.Instance().GetDocument();
            Element projectInfo = doc.createElement("ProjectInfo");
            doc.appendChild(projectInfo);

            // Plugin elements
            Element plugins = doc.createElement("Plugins");
            projectInfo.appendChild(plugins);

            //list of plugin associate to project
            for(Object item:listOfAddedPluginModel.toArray()){
                Element plugin = doc.createElement("Plugin");

                // set attribute to staff element
                Attr attr = doc.createAttribute("name");
                String noFormat = item.toString().replaceAll("<html>Plugin: ","");
                noFormat = noFormat.replaceAll("<br>Indices: ", "");
                noFormat = noFormat.replaceAll("<br>Quality: ","");
                noFormat = noFormat.replaceAll("<br>Modis Tiles: ","");
                noFormat = noFormat.replaceAll("</html>", "");
                noFormat = noFormat.replaceAll("</span>", "");
                noFormat = noFormat.replaceAll("<span>", "");

                String[] array = noFormat.split(";");

                attr.setValue(array[0].toString());
                plugin.setAttributeNode(attr);

                if(array.length < 3){
                    // start Date
                    Element qc = doc.createElement("QC");
                    qc.appendChild(doc.createTextNode(array[1].toString()));
                    plugin.appendChild(qc);
                }
                else{
                    for(int i = 1; i < array.length -1; i++){

                        if(isValueAModisTile(array, i))
                        {
                            Element modisTile = doc.createElement("ModisTile");
                            modisTile.appendChild(doc.createTextNode(array[i].toString()));
                            plugin.appendChild(modisTile);
                        }
                        else
                        {
                            Element indices = doc.createElement("Indices");
                            indices.appendChild(doc.createTextNode(array[i].toString()));
                            plugin.appendChild(indices);
                        }
                    }

                    Element qc = doc.createElement("QC");
                    qc.appendChild(doc.createTextNode(array[array.length - 1].toString()));
                    plugin.appendChild(qc);

                    for(int i = 1; i < array.length -1; i++){

                    }
                }

                // add a new node for plugin element
                plugins.appendChild(plugin);
            }

            // start Date
            Element startDate = doc.createElement("StartDate");
            startDate.appendChild(doc.createTextNode(this.startDate.getDate().toString()));
            projectInfo.appendChild(startDate);

            // project name
            Element projectName = doc.createElement("ProjectName");
            projectName.appendChild(doc.createTextNode(this.projectName.getText()));
            projectInfo.appendChild(projectName);

            // working directory
            Element workingDirectory = doc.createElement("WorkingDir");
            workingDirectory.appendChild(doc.createTextNode(this.workingDirectory.getText()));
            projectInfo.appendChild(workingDirectory);

            // masking file
            Element masking = doc.createElement("Masking");
            projectInfo.appendChild(masking);

            Element maskingFile = doc.createElement("File");
            maskingFile.appendChild(doc.createTextNode(maskFile.getText()));
            masking.appendChild(maskingFile);

            Dataset hDataset;
            double[] adfGeoTransform = new double[6];
            String pszFilename = maskFile.getText();
            String res="";

            gdal.AllRegister();

            hDataset = gdal.Open(pszFilename, gdalconstConstants.GA_ReadOnly);

            if (hDataset != null)
            {
                hDataset.GetGeoTransform(adfGeoTransform);
                {
                    if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
                        res = "" + ((int) (adfGeoTransform[1] + 0.5));
                    }
                }

                hDataset.delete();
            }

            Element resolution = doc.createElement("Resolution");
            resolution.appendChild(doc.createTextNode(res));
            masking.appendChild(resolution);

            Element masterShapeFile = doc.createElement("MasterShapeFile");
            masterShapeFile.appendChild(doc.createTextNode(masterShapeTextField.getText()));
            projectInfo.appendChild(masterShapeFile);

            Element timeZone = doc.createElement("TimeZone");
            timeZone.appendChild(doc.createTextNode(String.valueOf(timeZoneComboBox.getSelectedItem())));
            projectInfo.appendChild(timeZone);

            Element isClipping = doc.createElement("Clipping");
            isClipping.appendChild(doc.createTextNode(String.valueOf(isClippingCheckBox.isSelected())));
            projectInfo.appendChild(isClipping);

            // Freezing start Date
            Element freezingstartDate = doc.createElement("FreezingDate");
            freezingstartDate.appendChild(doc.createTextNode(freezingDateChooser.getDate().toString()));
            projectInfo.appendChild(freezingstartDate);

            // Cooling degree value
            Element coolingDegree = doc.createElement("CoolingDegree");
            coolingDegree.appendChild(doc.createTextNode(coolingTextField.getText()));
            projectInfo.appendChild(coolingDegree);

            // Heating start Date
            Element heatingstartDate = doc.createElement("HeatingDate");
            heatingstartDate.appendChild(doc.createTextNode(heatingDateChooser.getDate().toString()));
            projectInfo.appendChild(heatingstartDate);

            // heating degree value
            Element heatingDegree = doc.createElement("HeatingDegree");
            heatingDegree.appendChild(doc.createTextNode(heatingTextField.getText()));
            projectInfo.appendChild(heatingDegree);

            // re-sampling
            Element reSampling = doc.createElement("ReSampling");
            reSampling.appendChild(doc.createTextNode(String.valueOf(reSamplingComboBox.getSelectedItem())));
            projectInfo.appendChild(reSampling);

            //datum
            Element pixelSize = doc.createElement("PixelSize");
            pixelSize.appendChild(doc.createTextNode(String.valueOf(this.pixelSize.getText())));
            projectInfo.appendChild(pixelSize);

            //list of summary tiles
            Element summaries = doc.createElement("Summaries");
            projectInfo.appendChild(summaries);

            int summaryCounter = 1;
            for(Object item:summaryListModel.toArray()){
                Element summary = doc.createElement("Summary");
                summary.setAttribute("ID", String.valueOf(summaryCounter));
                summaryCounter ++;

                summary.appendChild(doc.createTextNode(item.toString()));
                summaries.appendChild(summary);
            }

            if(ProjectInfoCollection.WriteProjectToFile(doc, this.projectName.getText())){
                System.out.println("File saved!");
                Element startDateSUB, endDateSUB = null;
                projectInfo.removeChild(startDate);
                projectInfo.removeChild(projectName);

                for(int s=0; s < startDates.size(); s++){
                    startDateSUB = doc.createElement("StartDate");
                    projectName = doc.createElement("ProjectName");
                    startDateSUB.appendChild(doc.createTextNode(startDates.get(s)));
                    projectName.appendChild(doc.createTextNode(this.projectName.getText()+"-SUB"+s));
                    projectInfo.appendChild(projectName);
                    projectInfo.appendChild(startDateSUB);
                    if( s + 1 < startDates.size()){ // Don't include an EndDate for the last subproject
                        endDateSUB = doc.createElement("EndDate");
                        endDateSUB.appendChild(doc.createTextNode(endDates.get(s)));
                        projectInfo.appendChild(endDateSUB);
                    }
                    if(ProjectInfoCollection.WriteProjectToFile(doc, this.projectName.getText()+"-SUB"+s)){
                        System.out.println("Subproject saved!");
                        projectInfo.removeChild(startDateSUB);
                        if( s + 1 < startDates.size()){
                            projectInfo.removeChild(endDateSUB);
                        }
                        projectInfo.removeChild(projectName);
                    } else {
                        System.out.println("Error in saving");
                    }
                }
                mainWindowEvent.fire();
            }else{
                System.out.println("Error in saving");
            }
        } catch (ParserConfigurationException e) {
            ErrorLog.add(Config.getInstance(), "ProjectInformationPage.CreateNewProject problem with creating new project.", e);
        }
    }

    /**
     * populate project info for edit
     */
    private void PopulateProjectInfo(){
        ProjectInfoFile project = ProjectInfoCollection.GetProject(
                Config.getInstance(),
                String.valueOf(projectCollectionComboBox.getSelectedItem()));

        if(project == null) {
            return;
        }else{
            startDate.setEnabled(false);
            startDate.setDate(Date.from(project.GetStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            projectName.setEnabled(false);
            projectName.setText(project.GetProjectName());
            workingDirectory.setEnabled(false);
            workingDirectory.setText(project.GetWorkingDir());
            maskFile.setEnabled(false);
            maskFile.setText(project.GetMaskingFile());
            //  resolutionTextField.setEnabled(false);
            // resolutionTextField.setText((project.GetMaskingResolution() != null) ? project.GetMaskingResolution().toString() : null);
            masterShapeTextField.setEnabled(false);
            masterShapeTextField.setText(project.GetMasterShapeFile());
            timeZoneComboBox.setEnabled(false);
            timeZoneComboBox.setSelectedItem(project.GetTimeZone());
            freezingDateChooser.setEnabled(false);
            freezingDateChooser.setDate(Date.from(project.GetFreezingDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            coolingTextField.setEnabled(false);
            coolingTextField.setText(project.GetCoolingDegree().toString());
            heatingDateChooser.setEnabled(false);
            heatingDateChooser.setDate(Date.from(project.GetHeatingDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            heatingTextField.setEnabled(false);
            heatingTextField.setText(project.GetHeatingDegree().toString());
            isClippingCheckBox.setEnabled(false);
            isClippingCheckBox.setSelected(project.GetClipping());

            reSamplingComboBox.setEnabled(false);
            reSamplingComboBox.setSelectedItem(project.GetProjection().getResamplingType());
            pixelSize.setEnabled(false);
            pixelSize.setText(String.valueOf(project.GetProjection().getPixelSize()));

            summaryListModel.clear();
            for(ProjectInfoSummary summary: project.GetSummaries()){
                summaryListModel.addElement(summary.toString());
            }

            listOfAddedPluginModel.clear();
            for(ProjectInfoPlugin plugin: project.GetPlugins()){
                String formatString = String.format("<html>Plugin: %s;<br>Indices: %s</span> <br>%s</span> <br>Quality: %s;</span></html>",
                        String.valueOf(plugin.GetName()),
                        getIndicesFormat(plugin.GetIndices()),
                        getModisTilesFormat(plugin.GetModisTiles()),
                        String.valueOf(plugin.GetQC())
                        );
                listOfAddedPluginModel.addElement(formatString);
            }
        }
    }

    // delete selected plugin from plugin list
    @SuppressWarnings("rawtypes")
    private void deleteSelectedPlugin(final JList<String> listOfAddedPlugin) {
        DefaultListModel model = (DefaultListModel) listOfAddedPlugin.getModel();
        int selectedIndex = listOfAddedPlugin.getSelectedIndex();

        if (selectedIndex != -1) {
            model.remove(selectedIndex);
        }
    }

    // select working directory
    private void chooseWorkingDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Browse the folder to process");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "+ chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "+ chooser.getSelectedFile());
            workingDirectory.setText(chooser.getSelectedFile().toString());
        } else {
            System.out.println("No Selection ");
        }
    }

    // select masking file
    private void chooseMaskingFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("tiff files", "tiff", "tif");
        chooser.setFileFilter(xmlfilter);
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Browse the folder to process");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "+ chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "+ chooser.getSelectedFile());
            maskFile.setText(chooser.getSelectedFile().toString());
        } else {
            System.out.println("No Selection ");
        }
    }

    // choose master shape file
    private void chooseMasterShapeFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Browse the folder to process");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "+ chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "+ chooser.getSelectedFile());
            masterShapeTextField.setText(chooser.getSelectedFile().toString());
        } else {
            System.out.println("No Selection ");
        }
    }

    // delete selected summary in list of summary
    @SuppressWarnings("rawtypes")
    private void deleteSelectedSummary(final JList summaryList) {
        DefaultListModel model = (DefaultListModel) summaryList.getModel();
        int selectedIndex = summaryList.getSelectedIndex();

        if (selectedIndex != -1) {
            model.remove(selectedIndex);
        }
    }

    // verify if the string array is a modis value
    private boolean isValueAModisTile(String[] array, int i) {
        String tile = array[i].toString().toUpperCase();

        if(tile.charAt(1) != 'H' || tile.charAt(4) != 'V' || tile.length() > 7) {
            return false;
        } else{
            try {
                Integer.parseInt(String.format("%s%s", tile.toUpperCase().charAt(2), tile.toUpperCase().charAt(3)));
                Integer.parseInt(String.format("%c%c", tile.toUpperCase().charAt(5), tile.toUpperCase().charAt(6)));

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    // convert array list of string to a indices format
    private String getIndicesFormat(ArrayList<String> m){
        String formatString = "";

        for(String indici : m){
            formatString += String.format("<span>%s;</span>",   indici);
        }

        return formatString;
    }

    // format array list of string to a modis format
    private String getModisTilesFormat(ArrayList<String> m) {
        if(m.isEmpty()) {
            return "";
        } else
        {
            String formatString = "Modis Tiles: ";

            for(Object tile : m) {
                formatString += tile.toString() + "; ";
                globalModisTiles.add(tile.toString());
            }

            return formatString;
        }
    }

    // communication between plugin window (associatePluginPage.java) and this window
    class indicesListenerImplementation implements IndicesListener{

        @Override
        public void AddPlugin(IndicesEventObject e) {
            listOfAddedPluginModel.addElement(e.getPlugin());

            // check if the tiles was change
            if(equalLists(globalModisTiles, e.getTiles())) {
                return ;
            } else {
                //modis change so you have to format all modis to format the same
                globalModisTiles.clear();

                for(String tile : e.getTiles()) {
                    globalModisTiles.add(tile);
                }

                DefaultListModel<String> temp = new DefaultListModel<String>();
                for(Object item:listOfAddedPluginModel.toArray()){
                    if(item.toString().toUpperCase().contains("MODIS")){
                        temp.addElement(item.toString());
                        listOfAddedPluginModel.removeElement(item);
                    }
                }

                //<br>Modis Tiles: h01v01; </span>
                for(Object item:temp.toArray()){
                    String pluginFormat = item.toString();
                    String [] section1 = pluginFormat.split("<br>Modis Tiles: ");
                    String [] section2 = pluginFormat.split("<br>Quality:");
                    String newPluginFormat = String.format("%s%s<br>Quality:%s", section1[0], newModisFormat(globalModisTiles), section2[1]);
                    listOfAddedPluginModel.addElement(newPluginFormat);
                }

            }
        }

        // check if 2 list are the same
        private  boolean equalLists(List<String> one, List<String> two){
            if (one == null && two == null){
                return true;
            }

            if((one == null && two != null)
                    || one != null && two == null
                    || one.size() != two.size()){
                return false;
            }

            //to avoid messing the order of the lists we will use a copy
            //as noted in comments by A. R. S.
            one = new ArrayList<String>(one);
            two = new ArrayList<String>(two);

            Collections.sort(one);
            Collections.sort(two);
            return one.equals(two);
        }

        private String newModisFormat(ArrayList<String> globalModisTiles) {
            String formatString = "Modis Tiles: ";

            for(Object tile : globalModisTiles.toArray()) {
                formatString += tile.toString() + "; ";
            }

            return String.format("<br>%s</span>",formatString);
        }
    }

    class summaryListenerImplementation implements SummaryListener{
        @Override
        public void AddSummary(SummaryEventObject e) {
            summaryListModel.addElement(e.getPlugin());
        }
    }
}