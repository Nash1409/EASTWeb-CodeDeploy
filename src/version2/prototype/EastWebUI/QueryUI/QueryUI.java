package version2.prototype.EastWebUI.QueryUI;

import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.ProjectInfoMetaData.ProjectInfoCollection;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.util.EASTWebQuery;
import version2.prototype.util.EASTWebResult;
import version2.prototype.util.EASTWebResults;

public class QueryUI {
    boolean isViewSQL = true;
    String[] operationList = {"<", ">", "=", "<>", "<=", ">="};

    private JFrame frame;
    private JComboBox<String> projectListComboBox ;
    private JComboBox<String> pluginComboBox;
    private JCheckBox chckbxCount;
    private JCheckBox chckbxSum;
    private JCheckBox chckbxMean;
    private JCheckBox chckbxStdev;
    private JCheckBox minCheckBox;
    private JCheckBox maxCheckBox;
    private JCheckBox sqrSumCheckBox;
    private JCheckBox chckbxZone;
    private JCheckBox chckbxYear;
    private JCheckBox chckbxDay;
    private JComboBox<Object> yearComboBox;
    private JComboBox<Object> dayComboBox;
    private JTextField yearTextField;
    private JTextField dayTextField;
    private JList<String> includeListIndices;
    private JList<String> excludeListIndices;
    private JList<String> includeListZone;
    private JList<String> excludeListZone;
    private JTextPane sqlViewTextPanel;
    private JTextPane resultTextPane;

    private DefaultListModel<String> includeIndicesListModel ;
    private DefaultListModel<String> excludeIndicesListModel ;

    private DefaultListModel<String> includeZoneListModel ;
    private DefaultListModel<String> excludeZoneListModel ;
    private JPanel panel;
    private JLabel lblQueryWindow;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    @SuppressWarnings("unused")
                    QueryUI window = new QueryUI();

                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "QueryUI.main problem with running a QueryUI window.", e);
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public QueryUI() throws Exception {
        initialize();

        // And From your main() method or any other method
        Timer timer = new Timer();
        timer.schedule(new UpdateQuery(), 0, 100);
    }

    // initialize for the UI
    private void initialize() throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

        frame = new JFrame();
        frame.setVisible(true);
        frame.setBounds(100, 100, 1149, 800);
        //frame.setBounds(100, 100, 400, 800);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setResizable(false); //Disable the Resize Button

        includeZoneListModel = new DefaultListModel<String>();
        excludeZoneListModel = new DefaultListModel<String>();
        includeIndicesListModel = new DefaultListModel<String>();
        excludeIndicesListModel = new DefaultListModel<String>();

        PopulateQueryBuilderUI();
        CreateSQLView();
    }

    // populate the query builder UI
    private void PopulateQueryBuilderUI() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Query Builder", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBounds(10, 73, 1113, 370);
        frame.getContentPane().add(panel);
        panel.setLayout(null);

        lblQueryWindow = new JLabel("Query Window");
        lblQueryWindow.setFont(new Font("Courier", Font.BOLD,25));
        lblQueryWindow.setBounds(10, 11, 255, 51);
        frame.getContentPane().add(lblQueryWindow);

        // close application
        JButton btnClose = new JButton("");
        btnClose.setToolTipText("Close Window");
        btnClose.setOpaque(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setBounds(1061, 21, 48, 41);
        btnClose.setIcon(new ImageIcon(QueryUI.class.getResource("/version2/prototype/Images/remove_32.png")));
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));}
        });
        frame.getContentPane().add(btnClose);

        // view full query
        JButton viewSQLButton = new JButton("");
        viewSQLButton.setOpaque(false);
        viewSQLButton.setContentAreaFilled(false);
        viewSQLButton.setBorderPainted(false);
        viewSQLButton.setToolTipText("View Results");
        viewSQLButton.setBounds(1003, 21, 48, 41);
        viewSQLButton.setIcon(new ImageIcon(QueryUI.class.getResource("/version2/prototype/Images/Very-Basic-Binoculars-icon.png")));
        viewSQLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {setSQLView();}
        });
        frame.getContentPane().add(viewSQLButton);

        populateFields();
        populateClause();
        populateIndices();
    }

    // populate fields UI
    private void populateFields() {
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setBounds(10, 84, 364, 275);
        fieldsPanel.setLayout(null);
        fieldsPanel.setBorder(new TitledBorder(null, "Fields", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(fieldsPanel);

        chckbxCount = new JCheckBox("count");
        chckbxCount.setBounds(6, 21, 63, 23);
        fieldsPanel.add(chckbxCount);

        chckbxSum = new JCheckBox("sum");
        chckbxSum.setBounds(6, 51, 63, 23);
        fieldsPanel.add(chckbxSum);

        chckbxMean = new JCheckBox("mean");
        chckbxMean.setBounds(6, 77, 63, 23);
        fieldsPanel.add(chckbxMean);

        chckbxStdev = new JCheckBox("stdev");
        chckbxStdev.setBounds(131, 21, 75, 23);
        fieldsPanel.add(chckbxStdev);

        minCheckBox = new JCheckBox("min");
        minCheckBox.setBounds(131, 51, 75, 23);
        fieldsPanel.add(minCheckBox);

        maxCheckBox = new JCheckBox("max");
        maxCheckBox.setBounds(131, 77, 75, 23);
        fieldsPanel.add(maxCheckBox);

        sqrSumCheckBox = new JCheckBox("sqrSum");
        sqrSumCheckBox.setBounds(261, 21, 97, 23);
        fieldsPanel.add(sqrSumCheckBox);
    }

    // populate clause UI
    private void populateClause() {
        JPanel clausePanel = new JPanel();
        clausePanel.setBounds(375, 84, 364, 275);
        clausePanel.setLayout(null);
        clausePanel.setBorder(new TitledBorder(null, "Clause Statement", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(clausePanel);

        chckbxYear = new JCheckBox("Year");
        chckbxYear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                yearComboBox.setEnabled(chckbxYear.isSelected());
                yearTextField.setEnabled(chckbxYear.isSelected());
            }
        });
        chckbxYear.setBounds(6, 22, 70, 23);
        clausePanel.add(chckbxYear);

        yearComboBox = new JComboBox<Object>(operationList);
        yearComboBox.setBounds(82, 23, 97, 20);
        yearComboBox.setEnabled(false);
        clausePanel.add(yearComboBox);

        yearTextField = new JTextField();
        yearTextField.setColumns(10);
        yearTextField.setBounds(189, 23, 165, 22);
        yearTextField.setEnabled(false);
        clausePanel.add(yearTextField);

        chckbxDay = new JCheckBox("Day");
        chckbxDay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                dayComboBox.setEnabled(chckbxDay.isSelected());
                dayTextField.setEnabled(chckbxDay.isSelected());
            }
        });
        chckbxDay.setBounds(6, 48, 70, 23);
        clausePanel.add(chckbxDay);

        dayComboBox = new JComboBox<Object>(operationList);
        dayComboBox.setBounds(82, 52, 97, 20);
        dayComboBox.setEnabled(false);
        clausePanel.add(dayComboBox);

        dayTextField = new JTextField();
        dayTextField.setColumns(10);
        dayTextField.setBounds(189, 50, 165, 22);
        dayTextField.setEnabled(false);
        clausePanel.add(dayTextField);

        final JLabel includeLbl = new JLabel("Include");
        includeLbl.setBounds(16, 104, 127, 14);
        includeLbl.setEnabled(false);
        clausePanel.add(includeLbl);

        final JScrollPane includeScrollPaneZone = new JScrollPane();
        includeScrollPaneZone.setBounds(10, 125, 125, 139);
        includeScrollPaneZone.setEnabled(false);
        clausePanel.add(includeScrollPaneZone);

        includeListZone = new JList<String>(includeZoneListModel);
        includeScrollPaneZone.setViewportView(includeListZone);
        includeListZone.setEnabled(false);

        final JLabel excludeLbl = new JLabel("Exclude");
        excludeLbl.setBounds(228, 104, 126, 14);
        clausePanel.add(excludeLbl);
        excludeLbl.setEnabled(false);

        final JScrollPane excludedScrollPaneZone = new JScrollPane();
        excludedScrollPaneZone.setBounds(229, 127, 125, 137);
        excludedScrollPaneZone.setEnabled(false);
        clausePanel.add(excludedScrollPaneZone);

        excludeListZone = new JList<String>(excludeZoneListModel);
        excludedScrollPaneZone.setViewportView(excludeListZone);
        excludeListZone.setEnabled(false);

        final JButton excludeButton = new JButton(">>");
        excludeButton.setBounds(148, 146, 70, 23);
        excludeButton.setEnabled(false);
        excludeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {deleteSelectedZone();}
        });
        clausePanel.add(excludeButton);

        final JButton includeButton = new JButton("<<");
        includeButton.setBounds(148, 219, 70, 23);
        includeButton.setEnabled(false);
        includeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {addSelectedZone();}
        });
        clausePanel.add(includeButton);

        chckbxZone = new JCheckBox("Zone");
        chckbxZone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                includeLbl.setEnabled(chckbxZone.isSelected());
                includeScrollPaneZone.setEnabled(chckbxZone.isSelected());
                includeListZone.setEnabled(chckbxZone.isSelected());
                excludeLbl.setEnabled(chckbxZone.isSelected());
                excludedScrollPaneZone.setEnabled(chckbxZone.isSelected());
                excludeListZone.setEnabled(chckbxZone.isSelected());
                excludeButton.setEnabled(chckbxZone.isSelected());
                includeButton.setEnabled(chckbxZone.isSelected());}
        });
        chckbxZone.setBounds(6, 74, 70, 23);
        clausePanel.add(chckbxZone);
    }

    // populate indices UI
    private void populateIndices() {
        JPanel indicesPanel = new JPanel();
        indicesPanel.setBounds(739, 84, 364, 275);
        indicesPanel.setLayout(null);
        indicesPanel.setBorder(new TitledBorder(null, "Enviromental Index", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(indicesPanel);

        JLabel lblInclude = new JLabel("Include");
        lblInclude.setBounds(10, 26, 127, 14);
        indicesPanel.add(lblInclude);

        JScrollPane includeScrollPanel = new JScrollPane();
        includeScrollPanel.setBounds(10, 40, 127, 224);
        includeListIndices = new JList<String>(includeIndicesListModel);
        includeScrollPanel.setViewportView(includeListIndices);
        indicesPanel.add(includeScrollPanel);

        JButton includeButton = new JButton("<<");
        includeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {addSelectedIndices();}
        });
        includeButton.setBounds(147, 226, 70, 23);
        indicesPanel.add(includeButton);

        JLabel lblExclude = new JLabel("Exclude");
        lblExclude.setBounds(228, 26, 126, 14);
        indicesPanel.add(lblExclude);

        JScrollPane excludeScrollPanel = new JScrollPane();
        excludeScrollPanel.setBounds(227, 40, 127, 224);
        excludeListIndices = new JList<String>(excludeIndicesListModel);
        excludeScrollPanel.setViewportView(excludeListIndices);
        indicesPanel.add(excludeScrollPanel);

        JButton excludeButton = new JButton(">>");
        excludeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {removeSelectedIndices();}
        });
        excludeButton.setBounds(147, 55, 70, 23);
        indicesPanel.add(excludeButton);

        JLabel lblProject = new JLabel("Project:");
        lblProject.setBounds(10, 25, 89, 14);
        panel.add(lblProject);

        projectListComboBox = new JComboBox<String>();
        projectListComboBox.setBounds(109, 22, 157, 20);
        projectListComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {populateInfoForProject();}
        });
        projectListComboBox.removeAllItems();
        projectListComboBox.addItem("");
        panel.add(projectListComboBox);

        JLabel lblPlugin = new JLabel("Plugin: ");
        lblPlugin.setBounds(10, 56, 46, 14);
        panel.add(lblPlugin);

        pluginComboBox = new JComboBox<String>();
        pluginComboBox.setBounds(109, 53, 157, 20);
        pluginComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {populateIndicesInfo();}
        });
        panel.add(pluginComboBox);

        JButton btnQuery = new JButton();
        btnQuery.setToolTipText("Query Database");
        btnQuery.setBounds(1057, 29, 46, 41);
        btnQuery.setOpaque(false);
        btnQuery.setContentAreaFilled(false);
        btnQuery.setBorderPainted(false);
        btnQuery.setIcon(new ImageIcon(QueryUI.class.getResource("/version2/prototype/Images/Database-icon.png")));
        btnQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {queryProject();}
        });
        panel.add(btnQuery);
    }

    // populate the detail SQL view
    private void CreateSQLView() {
        for(ProjectInfoFile project : ProjectInfoCollection.GetAllProjectInfoFiles(Config.getInstance())) {
            projectListComboBox.addItem(project.GetProjectName());
        }

        JPanel resultPanel = new JPanel();
        resultPanel.setBorder(new TitledBorder(null, "Results", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        resultPanel.setBounds(10, 454, 1113, 296);
        frame.getContentPane().add(resultPanel);
        resultPanel.setLayout(null);

        JLabel lblQueryStatement = new JLabel("Query Statement");
        lblQueryStatement.setBounds(10, 35, 109, 14);
        resultPanel.add(lblQueryStatement);

        JScrollPane sqlScrollPane = new JScrollPane();
        sqlScrollPane.setBounds(10, 60, 500, 225);
        resultPanel.add(sqlScrollPane);

        sqlViewTextPanel = new JTextPane();
        sqlScrollPane.setViewportView(sqlViewTextPanel);

        JLabel lblQueryResults = new JLabel("Query Results");
        lblQueryResults.setBounds(603, 35, 109, 14);
        resultPanel.add(lblQueryResults);

        JScrollPane resultScrollPane = new JScrollPane();
        resultScrollPane.setBounds(603, 60, 500, 225);
        resultPanel.add(resultScrollPane);

        resultTextPane = new JTextPane();
        resultScrollPane.setViewportView(resultTextPane);
    }

    // remove selected indices
    private void removeSelectedIndices() {
        DefaultListModel<String> model = (DefaultListModel<String>) includeListIndices.getModel();
        List<String> selectedIndexs = includeListIndices.getSelectedValuesList();

        for(String selectedIndex : selectedIndexs){
            if (selectedIndex != null) {
                excludeIndicesListModel.addElement(selectedIndex);
                model.removeElement(selectedIndex);
            }
        }
    }

    // add selected indices
    private void addSelectedIndices() {
        DefaultListModel<String> model = (DefaultListModel<String>) excludeListIndices.getModel();
        List<String> selectedIndexs = excludeListIndices.getSelectedValuesList();

        for(String selectedIndex : selectedIndexs){
            if (selectedIndex != null) {
                includeIndicesListModel.addElement(selectedIndex);
                model.removeElement(selectedIndex);
            }
        }
    }

    // deleted selected zones
    private void deleteSelectedZone() {
        DefaultListModel<String> model = (DefaultListModel<String>) includeListZone.getModel();
        List<String> selectedIndexs = includeListZone.getSelectedValuesList();

        for(String selectedIndex : selectedIndexs){
            if (selectedIndex != null) {
                excludeZoneListModel.addElement(selectedIndex);
                model.removeElement(selectedIndex);
            }
        }
    }

    // add selected zones
    private void addSelectedZone() {
        DefaultListModel<String> model = (DefaultListModel<String>) excludeListZone.getModel();
        List<String> selectedIndexs = excludeListZone.getSelectedValuesList();

        for(String selectedIndex : selectedIndexs){
            if (selectedIndex != null) {
                includeZoneListModel.addElement(selectedIndex);
                model.removeElement(selectedIndex);
            }
        }
    }

    // populate project information
    private void populateInfoForProject() {
        String selectedProject = String.valueOf(projectListComboBox.getSelectedItem());

        if(selectedProject != "") {
            ProjectInfoFile project = ProjectInfoCollection.GetProject(Config.getInstance(), selectedProject);
            pluginComboBox.removeAllItems();

            for(ProjectInfoPlugin plugin : project.GetPlugins()){
                pluginComboBox.addItem(plugin.GetName());
            }
        }
    }

    // toggle SQL view
    private void setSQLView() {
        isViewSQL = !isViewSQL;

        if(isViewSQL) {
            frame.setBounds(100, 100, 1149, 800);
        } else {
            frame.setBounds(100, 100, 1149, 485);
        }
    }

    // query project that was set in the UI
    private void queryProject() {
        String projectName = null;
        ProjectInfoFile project = ProjectInfoCollection.GetProject(Config.getInstance(), String.valueOf(projectListComboBox.getSelectedItem()));
        Map<Integer, EASTWebQuery> ewQuery = new HashMap<Integer, EASTWebQuery>();
        String[] indices = new String[includeIndicesListModel.toArray().length];

        for(int i=0; i < includeIndicesListModel.toArray().length; i++){
            indices[i] = includeIndicesListModel.get(i);
        }

        for(ProjectInfoSummary summary : project.GetSummaries()) {
            try {
                projectName = String.valueOf(projectListComboBox.getSelectedItem());
                projectName = projectName.substring(0,projectName.lastIndexOf('_'));
                javax.swing.JOptionPane.showMessageDialog(frame, "QueryUI, projectName: "+projectName);
                ewQuery.put(summary.GetID(), EASTWebResults.GetEASTWebQuery(
                        Config.getInstance().getGlobalSchema(),
                        projectName,
                        String.valueOf(pluginComboBox.getSelectedItem()),
                        chckbxCount.isSelected(),
                        maxCheckBox.isSelected(),
                        minCheckBox.isSelected(),
                        chckbxSum.isSelected(),
                        chckbxMean.isSelected(),
                        sqrSumCheckBox.isSelected(),
                        chckbxStdev.isSelected(),
                        (String[])includeZoneListModel.toArray(),
                        String.valueOf(yearComboBox.getSelectedItem()),
                        (yearTextField.getText().equals("") ? null : Integer.parseInt(yearTextField.getText())),
                        String.valueOf(dayComboBox.getSelectedItem()),
                        (dayTextField.getText().equals("") ? null : Integer.parseInt(dayTextField.getText())),
                        indices,
                        new Integer[]{summary.GetID()}));
            } catch (NumberFormatException e) {
                ErrorLog.add(Config.getInstance(), "QueryUI.CreateSQLView problem with getting csv result files.", e);
            }
        }

        if(ewQuery != null){
            String path = JOptionPane.showInputDialog(frame, "Enter file path to write csv file to:");

            if (path != null) {
                try {
                    Iterator<Integer> keysIt = ewQuery.keySet().iterator();
                    Integer key;

                    while(keysIt.hasNext()){
                        key = keysIt.next();
                        String tempPath = path + " - Summary " + key;
                        ArrayList<EASTWebResult> queryResults = EASTWebResults.GetEASTWebResults(ewQuery.get(key));
                        EASTWebResults.WriteEASTWebResultsToCSV(tempPath, queryResults);
                    }
                } catch (IOException | ClassNotFoundException | SQLException | ParserConfigurationException | SAXException e) {
                    JOptionPane.showMessageDialog(frame, "Folder not found");
                    ErrorLog.add(Config.getInstance(), "QueryResultWindow.initialize problem with copying files to different directory.", e);
                }
            }
        }
    }

    // populate indices information for UI
    private void populateIndicesInfo() {
        String pluginName = String.valueOf(pluginComboBox.getSelectedItem());
        String selectedProject = String.valueOf(projectListComboBox.getSelectedItem());
        ProjectInfoFile project = ProjectInfoCollection.GetProject(Config.getInstance(), String.valueOf(projectListComboBox.getSelectedItem()));

        includeIndicesListModel.removeAllElements();
        excludeIndicesListModel.removeAllElements();
        includeZoneListModel.removeAllElements();
        excludeZoneListModel.removeAllElements();

        for(ProjectInfoPlugin plugin: project.GetPlugins()){
            if( plugin.GetName().equals(pluginName)){
                for(String indice: plugin.GetIndices()) {
                    excludeIndicesListModel.addElement(indice);
                }
            }
        }

        for(String zone: EASTWebResults.GetZonesListFromProject(selectedProject, String.valueOf(pluginComboBox.getSelectedItem()))){
            excludeZoneListModel.addElement(zone);
        }
    }

    // time task to update the SQL view every x seconds
    class UpdateQuery extends TimerTask {
        @Override
        public void run() {
            sqlViewTextPanel.setText(String.format("%s\n%s\n%s", SelectStatement(), FromStatement(), WhereStatement()));
        }

        private String FromStatement() {
            return String.format("FROM %s", String.valueOf(projectListComboBox.getSelectedItem()));
        }

        private String SelectStatement() {
            String query = String.format("SELECT  name, year, day, index");

            if(chckbxCount.isSelected()) {
                query += ", count";
            }
            if(chckbxSum.isSelected()) {
                query += ", sum";
            }
            if(chckbxMean.isSelected()) {
                query += ", mean";
            }
            if(chckbxStdev.isSelected()) {
                query += ", stdev";
            }
            if(minCheckBox.isSelected()) {
                query += ", min";
            }
            if(maxCheckBox.isSelected()) {
                query += ", max";
            }
            if(sqrSumCheckBox.isSelected()) {
                query += ", sqrSum";
            }
            if(includeIndicesListModel.size() > 0){
                query += "\n\t,";
                for(int i = 0; i < includeIndicesListModel.size(); i ++){
                    query += String.format("%s,", includeIndicesListModel.elementAt(i));
                }

                query = query.substring(0, query.length()-1);
            }
            return query;
        }

        private String WhereStatement() {
            String query = "";

            if(chckbxYear.isSelected()){
                query += String.format("    year%s%s\n", String.valueOf(yearComboBox.getSelectedItem()), yearTextField.getText());
            }
            if(chckbxDay.isSelected()){
                query += String.format("    day%s%s\n", String.valueOf(dayComboBox.getSelectedItem()), dayTextField.getText());
            }
            if(chckbxZone.isSelected()) {
                for(int i = 0; i < includeZoneListModel.size(); i ++){
                    query += String.format("    Zone = %s,\n", includeZoneListModel.elementAt(i));
                }
                query = query.substring(0, query.length()-1);
            }

            query = String.format("WHERE\n") + query;

            return query;
        }
    }
}
