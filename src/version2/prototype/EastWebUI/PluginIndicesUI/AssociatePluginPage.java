package version2.prototype.EastWebUI.PluginIndicesUI;

import java.awt.EventQueue;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.ModisTile;
import version2.prototype.EastWebUI.ProjectInformationUI.ProjectInformationPage;
import version2.prototype.PluginMetaData.PluginMetaDataCollection;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import java.awt.Font;

public class AssociatePluginPage {
    public JFrame frame;
    private IndicesEvent indicesEvent;
    private PluginMetaDataCollection pluginMetaDataCollection;
    private JComboBox<String> pluginComboBox ;
    private JComboBox<String> indicesComboBox;
    private JComboBox<String> qcComboBox;
    private JButton addNewModisButton;
    private JButton deleteSelectedModisButton;

    private ArrayList<String> globalModisTiles;
    private DefaultListModel<String> modisListModel;

    private static JFrame parent = null;
    @SuppressWarnings("rawtypes")
    private DefaultListModel indicesListModel;

    /**
     * Launch application for debug.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    AssociatePluginPage window = new AssociatePluginPage(null, new ArrayList<String>(),parent);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "AssociatePluginPage.main problem with running a AssociatePluginPage window.", e);
                }
            }
        });
    }

    /**
     * Create the application.
     * @throws Exception
     */
    public AssociatePluginPage(IndicesListener l, ArrayList<String> globalModisTiles, JFrame parent) throws Exception {
        AssociatePluginPage.parent = parent;
        this.globalModisTiles = new ArrayList<String>();

        for(String tile :globalModisTiles) {
            this.globalModisTiles.add(tile);
        }

        indicesEvent = new IndicesEvent();
        indicesEvent.addListener(l);
        initialize();
        frame.setVisible(true);
        frame.setLocationRelativeTo(AssociatePluginPage.parent);
    }

    /**
     * Initialize the contents of the frame.
     * @throws Exception
     */
    private void initialize() throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

        frame = new JFrame();
        frame.setBounds(100, 100, 603, 400);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel pluginPanel = new JPanel();
        pluginMetaDataCollection = PluginMetaDataCollection.getInstance();
        pluginInformation(pluginPanel);
        ModisInformation(pluginPanel);

        JLabel lblModisTiles = new JLabel("Modis Tiles");
        lblModisTiles.setFont(new Font("Times New Roman", Font.PLAIN, 15));
        lblModisTiles.setBounds(420, 55, 90, 15);
        pluginPanel.add(lblModisTiles);
    }

    /**
     * populate plugin information UI
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void pluginInformation(JPanel pluginPanel) {
        pluginPanel.setLayout(null);
        pluginPanel.setBorder(new TitledBorder(null, "Plugin Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        pluginPanel.setBounds(547, 420, 383, 275);
        frame.getContentPane().add(pluginPanel);

        // list of indices to be added
        indicesListModel = new DefaultListModel();

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 110, 320, 215);
        pluginPanel.add(scrollPane);
        final JList<DefaultListModel> listOfInndices = new JList<DefaultListModel>(indicesListModel);
        scrollPane.setViewportView(listOfInndices);

        JLabel qcLabel = new JLabel("Quality Control");
        qcLabel.setBounds(10, 45, 80, 15);
        pluginPanel.add(qcLabel);
        qcComboBox = new JComboBox<String>();
        qcComboBox.setBounds(95, 45, 150, 25);
        pluginPanel.add(qcComboBox);

        // set indices UI
        JLabel indicesLabel = new JLabel("Indices");
        indicesLabel.setBounds(10, 75, 80, 15);
        pluginPanel.add(indicesLabel);
        indicesComboBox = new JComboBox<String>();
        indicesComboBox.setBounds(95, 75, 150, 25);
        pluginPanel.add(indicesComboBox);

        // set plug-in data
        JLabel pluginLabel = new JLabel("Plugin");
        pluginLabel.setBounds(10, 15, 80, 15);
        pluginPanel.add(pluginLabel);
        pluginComboBox = new JComboBox<String>();
        pluginComboBox.setBounds(95, 15, 150, 25);
        pluginComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { setAdditionalInfomationUI(); }
        });
        for(String plugin: pluginMetaDataCollection.pluginList){
            pluginComboBox.addItem(plugin);
        }
        pluginPanel.add(pluginComboBox);

        // add plugin to list
        final JButton btnSave = new JButton("Save");
        btnSave.setBounds(10, 330, 90, 25);
        btnSave.setEnabled(!indicesListModel.isEmpty());
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { saveAction(listOfInndices);}
        });
        pluginPanel.add(btnSave);

        // cancel button
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBounds(240, 330, 90, 25);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { frame.dispose(); }
        });
        pluginPanel.add(btnCancel);

        // add indices button
        final JButton btnAddIndices = new JButton("");
        btnAddIndices.setToolTipText("add indices ");
        btnAddIndices.setBounds(250, 75, 35, 25);
        btnAddIndices.setIcon(new ImageIcon(AssociatePluginPage.class.getResource("/version2/prototype/Images/action_add_16xLG.png")));
        btnAddIndices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { addIndicesAction(btnSave);}
        });
        pluginPanel.add(btnAddIndices);

        // delete selected indices
        JButton btnDeleteIndices = new JButton("");
        btnDeleteIndices.setBounds(290, 75, 35, 25);
        btnDeleteIndices.setToolTipText("delete selected indices");
        btnDeleteIndices.setIcon(new ImageIcon(AssociatePluginPage.class.getResource("/version2/prototype/Images/ChangeQueryType_deletequery_274.png")));
        btnDeleteIndices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { deleteIndicesAction(listOfInndices, btnSave); }
        });

        pluginPanel.add(btnDeleteIndices);
    }

    // generate format from list model to string
    private String getModisTilesFormat(DefaultListModel<String> modisListModel) {
        String formatString = "Modis Tiles: ";

        if(modisListModel.isEmpty() || !String.valueOf(pluginComboBox.getSelectedItem()).toUpperCase().contains("MODIS")) {
            return "";
        } else {
            for(Object tile : modisListModel.toArray()) {
                formatString += tile.toString() + "; ";
            }

            return String.format("<br>%s</span>",formatString);
        }
    }

    // generate modis information UI
    private void ModisInformation(JPanel modisInformationPanel) {
        modisInformationPanel.setLayout(null);
        modisInformationPanel.setBounds(359, 420, 275, 390);
        frame.getContentPane().add(modisInformationPanel);

        modisListModel = new DefaultListModel<String>();

        addNewModisButton = new JButton("");
        addNewModisButton.setBounds(420, 330, 35, 25);
        addNewModisButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/action_add_16xLG.png")));
        addNewModisButton.setToolTipText("Add modis");
        addNewModisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { validateModisInput(); }
        });
        modisInformationPanel.add(addNewModisButton);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(340, 110, 245, 215);
        modisInformationPanel.add(scrollPane);

        final JList<String> modisList = new JList<String>(modisListModel);
        scrollPane.setViewportView(modisList);

        deleteSelectedModisButton = new JButton("");
        deleteSelectedModisButton.setBounds(475, 330, 35, 25);
        deleteSelectedModisButton.setIcon(new ImageIcon(ProjectInformationPage.class.getResource("/version2/prototype/Images/ChangeQueryType_deletequery_274.png")));
        deleteSelectedModisButton.setToolTipText("Delete Selected Modis");
        deleteSelectedModisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) { deleteSelectedModisTile(modisList); }
        });
        modisInformationPanel.add(deleteSelectedModisButton);

        for(String tiles : globalModisTiles) {
            modisListModel.addElement(tiles);
        }
    }

    // save plug-in back to the project information
    @SuppressWarnings("rawtypes")
    private void saveAction(final JList<DefaultListModel> listOfInndices) {
        String formatString = String.format("<html>Plugin: %s;<br>Indices: %s</span> %s <br>Quality: %s;</span></html>",
                String.valueOf(pluginComboBox.getSelectedItem()),
                getIndicesFormat(listOfInndices.getModel()),
                getModisTilesFormat(modisListModel),
                String.valueOf(qcComboBox.getSelectedItem()));

        if(!modisListModel.isEmpty() && String.valueOf(pluginComboBox.getSelectedItem()).toUpperCase().contains("MODIS")) {
            globalModisTiles.clear();
            for(Object tile : modisListModel.toArray()) {
                globalModisTiles.add(tile.toString());
            }
        }

        indicesEvent.fire(formatString, globalModisTiles);
        frame.dispose();
    }

    // add indices
    @SuppressWarnings("unchecked")
    private void addIndicesAction(final JButton btnSave) {
        if(indicesComboBox.getSelectedItem() == null) {
            return ;
        }

        indicesListModel.addElement(String.valueOf(indicesComboBox.getSelectedItem()));
        indicesComboBox.removeItem(indicesComboBox.getSelectedItem());
        btnSave.setEnabled(!indicesListModel.isEmpty());
    }

    // delete selected indices
    @SuppressWarnings("rawtypes")
    private void deleteIndicesAction(final JList<DefaultListModel> listOfInndices, final JButton btnSave) {
        indicesComboBox.addItem(indicesListModel.getElementAt(listOfInndices.getSelectedIndex()).toString());

        DefaultListModel<DefaultListModel> model = (DefaultListModel<DefaultListModel>) listOfInndices.getModel();
        model.getElementAt(listOfInndices.getSelectedIndex());
        int selectedIndex = listOfInndices.getSelectedIndex();

        if (selectedIndex != -1) {
            model.remove(selectedIndex);
        }

        btnSave.setEnabled(!indicesListModel.isEmpty());
    }

    // populate additional information UI for a plug-in
    private void setAdditionalInfomationUI() {
        indicesListModel.removeAllElements();
        PluginMetaData plugin = pluginMetaDataCollection.pluginMetaDataMap.get(String.valueOf(pluginComboBox.getSelectedItem()));

        if(plugin.Title.toUpperCase().contains("MODIS")){
            if(frame.getBounds().width == 347) {
                frame.setBounds(frame.getBounds().x-128, frame.getBounds().y, 603, 400);
            }
        }
        else {
            if(frame.getBounds().width == 603) {
                frame.setBounds(frame.getBounds().x+128, frame.getBounds().y, 347, 400);
            }
        }

        indicesComboBox.removeAllItems();
        for(String indices : plugin.Indices.indicesNames) {
            indicesComboBox.addItem(indices);
        }

        qcComboBox.removeAllItems();
        for(String qc:plugin.QualityControlMetaData) {
            qcComboBox.addItem(qc);
        }
    }

    // validate modis input
    private void validateModisInput() {
        String tile = JOptionPane.showInputDialog(frame,"Enter Modis Tile", null);

        if(tile.toUpperCase().charAt(0) != 'H' || tile.toUpperCase().charAt(3) != 'V' || tile.length() > 6) {
            JOptionPane.showMessageDialog(frame, "Modis format: hddvdd  d=> digit");
            return;
        } else{
            int horizontal = Integer.parseInt(String.format("%s%s", tile.toUpperCase().charAt(1), tile.toUpperCase().charAt(2)));
            int vertical = Integer.parseInt(String.format("%c%c", tile.toUpperCase().charAt(4), tile.toUpperCase().charAt(5)));

            if(horizontal < ModisTile.HORZ_MIN || horizontal > ModisTile.HORZ_MAX || vertical < ModisTile.VERT_MIN || vertical > ModisTile.VERT_MAX){
                JOptionPane.showMessageDialog(frame, String.format("Horizontal has be to within %d-%d and Vertical has to be within %d-%d",
                        ModisTile.HORZ_MIN , ModisTile.HORZ_MAX , ModisTile.VERT_MIN, ModisTile.VERT_MAX ));
                return;
            }

        }

        for(Object item:modisListModel.toArray()){
            if(tile.contentEquals(item.toString())) {
                JOptionPane.showMessageDialog(frame, "Modis tile already exist");
                return;
            }
        }

        modisListModel.addElement(tile);
    }

    // delete selected modis
    @SuppressWarnings("rawtypes")
    private void deleteSelectedModisTile(final JList<String> modisList) {
        DefaultListModel model = (DefaultListModel) modisList.getModel();
        int selectedIndex = modisList.getSelectedIndex();

        if (selectedIndex != -1) {
            model.remove(selectedIndex);
        }
    }

    /**
     * format indices to show in UI
     * @param m
     * @return
     */
    @SuppressWarnings("rawtypes")
    private String getIndicesFormat(ListModel m){
        String formatString = "";
        ListModel model = m;

        for(int i=0; i < model.getSize(); i++){
            formatString += String.format("<span>%s;</span>",   model.getElementAt(i).toString());
        }

        return formatString;
    }
}
