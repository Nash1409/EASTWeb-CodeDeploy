package version2.prototype.EastWebUI.QueryUI;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import version2.prototype.Config;
import version2.prototype.ErrorLog;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class QueryResultWindow {

    private JFrame frame;
    private JList<String> list;

    private DefaultListModel<String> files ;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    List<File> results = new ArrayList<File>();
                    File temp = new File("C:\\Users\\sufi\\Desktop\\Clean.bat");

                    results.add(temp);
                    results.add(temp);
                    results.add(temp);

                    @SuppressWarnings("unused")
                    QueryResultWindow window = new QueryResultWindow(results);

                } catch (Exception e) {
                    ErrorLog.add(Config.getInstance(), "QueryResultWindow.main problem with running a QueryResultWindow window.", e);
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public QueryResultWindow(List<File> listFile) {
        files = new DefaultListModel<String>();
        initialize();

        for(File f:listFile) {
            files.addElement(f.getPath());
        }

    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 400, 325);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setVisible(true);

        list = new JList<String>(files);
        list.setBounds(10, 10, 364, 240);
        frame.getContentPane().add(list);

        JButton OpenBtn = new JButton("Open In Notepad ++");
        OpenBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    for(String v:list.getSelectedValuesList()) {
                        Runtime.getRuntime().exec(String.format("notepad %s",v));
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, "File not found");
                    ErrorLog.add(Config.getInstance(), "QueryResultWindow.initialize problem with openning query in notepad.", e);
                }
            }
        });
        OpenBtn.setBounds(20, 261, 149, 23);
        frame.getContentPane().add(OpenBtn);

        JButton btnSaveFilesTo = new JButton("Export Files to Folder");
        btnSaveFilesTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String message = "Enter Directory";
                String directory = JOptionPane.showInputDialog(frame, message);

                if (directory == null) {
                    // User clicked cancel
                }else{
                    File dest = new File(directory);
                    try {
                        for(String v:list.getSelectedValuesList()) {
                            File source = new File(v);
                            FileUtils.copyFileToDirectory(source, dest);
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(frame, "Folder not found");
                        ErrorLog.add(Config.getInstance(), "QueryResultWindow.initialize problem with copying files to different directory.", e);
                    }
                }
            }
        });
        btnSaveFilesTo.setBounds(214, 261, 149, 23);
        frame.getContentPane().add(btnSaveFilesTo);
    }
}
