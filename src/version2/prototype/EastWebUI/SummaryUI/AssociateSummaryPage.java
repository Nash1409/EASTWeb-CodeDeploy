package version2.prototype.EastWebUI.SummaryUI;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;

import version2.prototype.Config;
import version2.prototype.EASTWebManager;
import version2.prototype.ErrorLog;
import version2.prototype.util.ReadShapefile;
import version2.prototype.util.ShapefileException;

public class AssociateSummaryPage {
    private JComboBox<String> areaNameFieldComboBox;
    private JComboBox<String> areaCodeFieldComboBox;
    private JComboBox<String> temporalComboBox;
    private JTextField filePathText;
    private JButton browseButton;
    private JFrame frame;

    private SummaryEvent summaryEvent;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new AssociateSummaryPage(null,null);
                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "AssociateSummaryPage.main problem running an AssociateSummaryPage window.", e);
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public AssociateSummaryPage(SummaryListener l,JFrame parent){
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        summaryEvent = new SummaryEvent();
        summaryEvent.addListener(l);

        frame = new JFrame();
        frame.setVisible(true);
        frame.setBounds(100, 100, 401, 300);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(parent);

        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        JPanel myPanel = new JPanel();
        myPanel.setLayout(null);
        myPanel.setBorder(new TitledBorder(null, "Summary Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        myPanel.setBounds(547, 420, 383, 275);

        // shape file
        final JLabel filePathLabel = new JLabel("ShapeFile Path");
        filePathLabel.setBounds(10, 27, 152, 14);
        filePathLabel.setEnabled(true);
        myPanel.add(filePathLabel);
        filePathText =  new JTextField();
        filePathText.setBounds(172, 24, 150, 27);
        filePathText.setEnabled(true);
        filePathText.setColumns(10);
        myPanel.add(filePathText);

        // browse button
        browseButton = new JButton();
        browseButton.setToolTipText("browse file");
        browseButton.setBounds(344, 27, 41, 24);
        browseButton.setEnabled(true);
        browseButton.setOpaque(false);
        browseButton.setContentAreaFilled(false);
        browseButton.setBorderPainted(false);
        browseButton.setIcon(new ImageIcon(AssociateSummaryPage.class.getResource("/version2/prototype/Images/folder-explore-icon.png")));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {browserShapeFile();}
        });
        myPanel.add(browseButton);

        // area code
        final JLabel areaCodeFieldLabel = new JLabel("Area Code Field");
        areaCodeFieldLabel.setBounds(10, 58, 152, 14);
        myPanel.add(areaCodeFieldLabel);
        areaCodeFieldComboBox = new JComboBox<String>();
        areaCodeFieldComboBox.setEnabled(false);
        areaCodeFieldComboBox.setToolTipText("Area Code is a numeric value");
        areaCodeFieldComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {setTemporalView(areaCodeFieldComboBox.getSelectedItem());}
        });
        areaCodeFieldComboBox.setBounds(172, 55, 150, 20);
        myPanel.add(areaCodeFieldComboBox);

        // area name
        final JLabel areaNameFieldLabel = new JLabel("Area Name Field");
        areaNameFieldLabel.setBounds(10, 89, 152, 14);
        myPanel.add(areaNameFieldLabel);
        areaNameFieldComboBox = new JComboBox<String>();
        areaNameFieldComboBox.setEnabled(false);
        areaNameFieldComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {setTemporalView(areaNameFieldComboBox.getSelectedItem());}
        });
        areaNameFieldComboBox.setBounds(172, 86, 150, 20);
        myPanel.add(areaNameFieldComboBox);

        // temporal summary
        JLabel temporalLbl = new JLabel("Temporal Summary");
        temporalLbl.setBounds(10, 119, 152, 14);
        myPanel.add(temporalLbl);
        temporalComboBox = new JComboBox<String>();
        temporalComboBox.setBounds(172, 116, 150, 20);
        temporalComboBox.setEnabled(false);
        for(String strategy : EASTWebManager.GetRegisteredTemporalSummaryCompositionStrategies()){
            temporalComboBox.addItem(strategy);
        }
        myPanel.add(temporalComboBox);

        // save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {saveSummary();}
        });
        saveButton.setBounds(82, 237, 89, 23);
        myPanel.add(saveButton);

        // cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {frame.dispose();}
        });
        cancelButton.setBounds(233, 237, 89, 23);
        myPanel.add(cancelButton);

        frame.getContentPane().add(myPanel);
    }

    // browse file list
    private void browserShapeFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Browse the folder to process");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "+ chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "+ chooser.getSelectedFiles());
            filePathText.setText(chooser.getSelectedFile().toString());

            try {
                ReadShapefile shapfile = new ReadShapefile(filePathText.getText());
                populateShapeFiles(areaNameFieldComboBox, shapfile.getNameFeatureList());
                populateShapeFiles(areaCodeFieldComboBox, shapfile.getNumericFeatureList());
            } catch (ShapefileException e) {
                ErrorLog.add(Config.getInstance(), "AssociateSummaryPage.initialize problem with populating shape files.", e);
            }
        } else {
            System.out.println("No Selection ");
        }
    }

    // save summary selected
    private void saveSummary() {
        String temporal = String.valueOf(temporalComboBox.getSelectedItem());
        String summary = String.format("AreaNameField: %s; Shape File Path: %s; AreaCodeField: %s;",
                String.valueOf(areaNameFieldComboBox.getSelectedItem()),
                filePathText.getText(),
                String.valueOf(areaCodeFieldComboBox.getSelectedItem()));

        if(temporal != null && !temporal.isEmpty()) {
            summary = String.format("%s Temporal Summary: %s", summary, String.valueOf(temporalComboBox.getSelectedItem()));
        }

        summaryEvent.fire(summary);
        frame.dispose();
    }

    // enables temporal if selected item is set
    private void setTemporalView(Object selectedItem) {
        String temporal = String.valueOf(selectedItem);

        if(temporal != null & !temporal.isEmpty()) {
            temporalComboBox.setEnabled(true);
        }
    }

    // populate shape file combo box
    private void populateShapeFiles(JComboBox<String> shapeFileComboBox, ArrayList<String[]> featureList) throws ShapefileException{
        for (int i = 0; i < featureList.size(); i++) {
            for(String feature: featureList.get(i)){
                shapeFileComboBox.addItem(feature);
            }
        }
        shapeFileComboBox.setEnabled(true);
    }
}
