package version2.prototype.EastWebUI.SettingsUI;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.HeadlessException;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import version2.prototype.Config;
import version2.prototype.EastWebUI.DocumentBuilderInstance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTextArea;

public class SettingsUI {
    private JFrame frame;
    private JTextField errorLogDirTextField;
    private JTextField downloadDirtextField;
    private JTextField GlobalSchematextField;
    private JTextField textFieldHostName;
    private JTextField textFieldPort;
    private JTextField textFieldDatabaseName;
    private JTextField textFieldUserName;
    private JTextField textFieldPassword;
    private JTextField textFieldMaxNumOfConnectionsPerInstance;
    private JTextArea textArea;
    private JTree tree;
    private JScrollPane treePanel;
    private JPanel advancePanel;
    private JScrollPane textAreaPane;

    private boolean isAdvance = false;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new SettingsUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public SettingsUI()throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        initialize();
        initializeUIValue();

        // And From your main() method or any other method
        Timer timer = new Timer();
        timer.schedule(new UpdateModel(), 0, 100);
    }

    /**
     * Initialize the contents of the frame.
     * @throws IOException
     */
    private void initialize() throws IOException {
        frame = new JFrame();
        frame.setVisible(true);
        frame.setBounds(100, 100, 875, 326);
        frame.getContentPane().setLayout(null);

        JLabel lblProjectInformation = new JLabel("System Settings");
        lblProjectInformation.setFont(new Font("Courier", Font.BOLD,25));
        lblProjectInformation.setBounds(10, 11, 315, 32);
        frame.getContentPane().add(lblProjectInformation);

        // save button
        JButton saveButton = new JButton();
        saveButton.setBounds(805, 11, 45, 40);
        saveButton.setIcon(new ImageIcon(SettingsUI.class.getResource("/version2/prototype/Images/save_32.png")));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {saveConfig();}
        });
        saveButton.setToolTipText("Save Settings");
        frame.getContentPane().add(saveButton);

        // error log directory
        JLabel lblErrorLogDir = new JLabel("Error Log Directory:");
        lblErrorLogDir.setBounds(10, 60, 143, 14);
        frame.getContentPane().add(lblErrorLogDir);
        errorLogDirTextField = new JTextField();
        errorLogDirTextField.setBounds(163, 57, 357, 25);
        errorLogDirTextField.setColumns(10);
        frame.getContentPane().add(errorLogDirTextField);

        JButton browseErrorLogDirButton = new JButton("");
        browseErrorLogDirButton.setBounds(530, 54, 27, 23);
        browseErrorLogDirButton.setOpaque(false);
        browseErrorLogDirButton.setContentAreaFilled(false);
        browseErrorLogDirButton.setBorderPainted(false);
        browseErrorLogDirButton.setIcon(new ImageIcon(SettingsUI.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        browseErrorLogDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { browseFolderDir(errorLogDirTextField);}
        });
        frame.getContentPane().add(browseErrorLogDirButton);

        // download directory
        JLabel lblDownloadDir = new JLabel("Download Directory:");
        lblDownloadDir.setBounds(10, 85, 143, 14);
        frame.getContentPane().add(lblDownloadDir);

        downloadDirtextField = new JTextField();
        downloadDirtextField.setColumns(10);
        downloadDirtextField.setBounds(163, 82, 357, 25);
        frame.getContentPane().add(downloadDirtextField);

        JButton browseDownloadDirButton = new JButton("");
        browseDownloadDirButton.setBounds(530, 85, 27, 23);
        browseDownloadDirButton.setOpaque(false);
        browseDownloadDirButton.setContentAreaFilled(false);
        browseDownloadDirButton.setBorderPainted(false);
        browseDownloadDirButton.setIcon(new ImageIcon(SettingsUI.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        browseDownloadDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { browseFolderDir(downloadDirtextField);}
        });
        frame.getContentPane().add(browseDownloadDirButton);

        databaseUI();
        advanceUI();
    }

    // populate value from the configuration file
    private void initializeUIValue() {
        errorLogDirTextField.setText(Config.getInstance().getErrorLogDir());
        downloadDirtextField.setText(Config.getInstance().getDownloadDir());
        GlobalSchematextField.setText(Config.getInstance().getGlobalSchema());
        textFieldHostName.setText(Config.getInstance().getDatabaseHost());
        textFieldPort.setText(Config.getInstance().getPort().toString());
        textFieldDatabaseName.setText(Config.getInstance().getDatabaseName());
        textFieldUserName.setText(Config.getInstance().getDatabaseUsername());
        textFieldPassword.setText(Config.getInstance().getDatabasePassword());
        textFieldMaxNumOfConnectionsPerInstance.setText(Config.getInstance().getMaxNumOfConnectionsPerInstance().toString());
    }

    // set up database UI
    private void databaseUI() {
        JPanel databasePanel = new JPanel();
        databasePanel.setBorder(new TitledBorder(null, "Database Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        databasePanel.setBounds(10, 110, 839, 133);
        databasePanel.setLayout(null);
        frame.getContentPane().add(databasePanel);

        JLabel labelGlobalSchema = new JLabel("Global Schema");
        labelGlobalSchema.setBounds(10, 24, 153, 14);
        databasePanel.add(labelGlobalSchema);
        GlobalSchematextField = new JTextField();
        GlobalSchematextField.setBounds(173, 18, 238, 27);
        GlobalSchematextField.setColumns(10);
        databasePanel.add(GlobalSchematextField);

        JLabel labelHostName = new JLabel("Host Name");
        labelHostName.setBounds(10, 49, 153, 14);
        databasePanel.add(labelHostName);
        textFieldHostName = new JTextField();
        textFieldHostName.setBounds(173, 43, 238, 27);
        textFieldHostName.setColumns(10);
        databasePanel.add(textFieldHostName);

        JLabel labelPort = new JLabel("Port");
        labelPort.setBounds(10, 74, 153, 14);
        databasePanel.add(labelPort);
        textFieldPort = new JTextField();
        textFieldPort.setBounds(173, 68, 238, 27);
        textFieldPort.setColumns(10);
        databasePanel.add(textFieldPort);

        JLabel labelDatabaseName = new JLabel("Database Name");
        labelDatabaseName.setBounds(10, 99, 153, 14);
        databasePanel.add(labelDatabaseName);
        textFieldDatabaseName = new JTextField();
        textFieldDatabaseName.setBounds(173, 93, 238, 27);
        textFieldDatabaseName.setColumns(10);
        databasePanel.add(textFieldDatabaseName);

        JLabel labelUserName = new JLabel("User Name");
        labelUserName.setBounds(421, 24, 153, 14);
        databasePanel.add(labelUserName);
        textFieldUserName = new JTextField();
        textFieldUserName.setBounds(584, 18, 238, 27);
        textFieldUserName.setColumns(10);
        databasePanel.add(textFieldUserName);

        JLabel labelPassword = new JLabel("Password");
        labelPassword.setBounds(421, 49, 153, 14);
        databasePanel.add(labelPassword);
        textFieldPassword = new JTextField();
        textFieldPassword.setBounds(584, 43, 238, 27);
        textFieldPassword.setColumns(10);
        databasePanel.add(textFieldPassword);

        JLabel labelMaxNumOfConnectionsPerInstance = new JLabel("Connections Per Instance");
        labelMaxNumOfConnectionsPerInstance.setBounds(421, 74, 153, 14);
        databasePanel.add(labelMaxNumOfConnectionsPerInstance);
        textFieldMaxNumOfConnectionsPerInstance = new JTextField();
        textFieldMaxNumOfConnectionsPerInstance.setColumns(10);
        textFieldMaxNumOfConnectionsPerInstance.setBounds(584, 68, 238, 27);
        databasePanel.add(textFieldMaxNumOfConnectionsPerInstance);
    }

    // populate advance UI (for threading purposes)
    private void advanceUI() throws IOException {
        JButton btnAdvanceSettings = new JButton("");
        btnAdvanceSettings.setToolTipText("Advance Settings");
        btnAdvanceSettings.setBounds(750, 10, 45, 40);
        btnAdvanceSettings.setIcon(new ImageIcon(SettingsUI.class.getResource("/version2/prototype/Images/cog.png")));
        btnAdvanceSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { showAdvanceUI(); }
        });
        frame.getContentPane().add(btnAdvanceSettings);

        JLabel lblAdvanceSystemSettings = new JLabel("Advance System Settings");
        lblAdvanceSystemSettings.setFont(new Font("Monospaced", Font.BOLD, 25));
        lblAdvanceSystemSettings.setBounds(10, 288, 389, 32);
        frame.getContentPane().add(lblAdvanceSystemSettings);
        advancePanel = new JPanel();
        advancePanel.setBorder(new TitledBorder(null, "Advance Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        advancePanel.setBounds(10, 329, 839, 297);
        advancePanel.setLayout(null);
        frame.getContentPane().add(advancePanel);

        setModelForAdvanceConfig();

        JButton refreshButton = new JButton("");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advancePanel.remove(textAreaPane);
                advancePanel.remove(treePanel);
                setModelForAdvanceConfig();
            }
        });
        refreshButton.setIcon(new ImageIcon(SettingsUI.class.getResource("/version2/prototype/Images/Refresh.png")));
        refreshButton.setToolTipText("Refresh Model");
        refreshButton.setBounds(387, 125, 52, 40);
        advancePanel.add(refreshButton);
    }

    // model for the advance configuration UI
    private void setModelForAdvanceConfig() {
        textAreaPane = new JScrollPane();
        textAreaPane.setBounds(10, 28, 367, 255);
        advancePanel.add(textAreaPane);

        textArea = new JTextArea();
        textAreaPane.setViewportView(textArea);

        try {
            FileReader reader = new FileReader(System.getProperty("user.dir") + "\\config\\" + "c3p0-config.xml");
            BufferedReader br = new BufferedReader(reader);
            textArea.read(reader, null);
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        tree = new XMLTree(textArea.getText());
        treePanel = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treePanel.setBounds(449, 28, 375, 255);
        advancePanel.add(treePanel);
    }

    // browser file directory
    private void browseFolderDir(JTextField dirTextField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Browse the folder to process");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "+ chooser.getCurrentDirectory());
            dirTextField.setText(chooser.getSelectedFile().toString());
        } else {
            System.out.println("No Selection ");
        }
    }

    // show advance UI toggle
    private void showAdvanceUI() {
        isAdvance = !isAdvance;

        if(isAdvance){
            frame.setBounds(100, 100, 875, 675);
        }else {
            frame.setBounds(100, 100, 875, 326);
        }
    }

    // logic to get XML object
    public Document GetXMLObject() throws ParserConfigurationException {
        Document doc = DocumentBuilderInstance.Instance().GetDocument();
        Element config = doc.createElement("config");
        doc.appendChild(config);

        Element ErrorLogDir = doc.createElement("ErrorLogDir");
        ErrorLogDir.appendChild(doc.createTextNode(errorLogDirTextField.getText()));
        config.appendChild(ErrorLogDir);

        Element DownloadDir = doc.createElement("DownloadDir");
        DownloadDir.appendChild(doc.createTextNode(downloadDirtextField.getText()));
        config.appendChild(DownloadDir);

        Element Database = doc.createElement("Database");
        config.appendChild(Database);

        Element GlobalSchema = doc.createElement("GlobalSchema");
        GlobalSchema.appendChild(doc.createTextNode(GlobalSchematextField.getText()));
        Database.appendChild(GlobalSchema);

        Element HostName = doc.createElement("HostName");
        HostName.appendChild(doc.createTextNode(textFieldHostName.getText()));
        Database.appendChild(HostName);

        Element Port = doc.createElement("Port");
        Port.appendChild(doc.createTextNode(textFieldPort.getText()));
        Database.appendChild(Port);

        Element DatabaseName = doc.createElement("DatabaseName");
        DatabaseName.appendChild(doc.createTextNode(textFieldDatabaseName.getText()));
        Database.appendChild(DatabaseName);

        Element UserName = doc.createElement("UserName");
        UserName.appendChild(doc.createTextNode(textFieldUserName.getText()));
        Database.appendChild(UserName);

        Element PassWord = doc.createElement("PassWord");
        PassWord.appendChild(doc.createTextNode(textFieldPassword.getText()));
        Database.appendChild(PassWord);

        Element MaxNumOfConnectionsPerInstance = doc.createElement("MaxNumOfConnectionsPerInstance");
        MaxNumOfConnectionsPerInstance.appendChild(doc.createTextNode(textFieldMaxNumOfConnectionsPerInstance.getText()));
        Database.appendChild(MaxNumOfConnectionsPerInstance);

        Element Output = doc.createElement("Output");
        config.appendChild(Output);

        for(String ts : Config.getInstance().getSummaryTempCompStrategies()) {
            Element TemporalSummaryCompositionStrategy = doc.createElement("TemporalSummaryCompositionStrategy");
            TemporalSummaryCompositionStrategy.appendChild(doc.createTextNode(ts));
            Output.appendChild(TemporalSummaryCompositionStrategy);
        }

        for(String sc : Config.getInstance().getSummaryCalculations()) {
            Element SummaryCalculation = doc.createElement("SummaryCalculation");
            SummaryCalculation.appendChild(doc.createTextNode(sc));
            Output.appendChild(SummaryCalculation);
        }

        return doc;
    }

    // save logic
    private void saveConfig() {
        try {
            if(Config.getInstance().WriteConfigFile(GetXMLObject()) && Config.getInstance().Writec3p0File(textArea.getText())){
                JOptionPane.showMessageDialog(frame, "Settings was saved");
                System.out.println("File saved!");
            }else{
                System.out.println("Erorr in saving");
            }
        } catch (HeadlessException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        frame.dispose();
    }

    // update poll timer
    class UpdateModel extends TimerTask {
        String advanceConfig = "";

        public UpdateModel() {
            advanceConfig = textArea.getText();
        }

        @Override
        public void run() {
            // update only when there is something to update
            if(!advanceConfig.equals(textArea.getText())){
                advanceConfig = textArea.getText();
                advancePanel.remove(treePanel);

                tree = new XMLTree(advanceConfig);
                treePanel = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                treePanel.setBounds(460, 28, 375, 255);
                advancePanel.add(treePanel);
            }
        }
    }
}