/**
 *
 */
package version2.prototype.PluginMetaData;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author michael.devos
 *
 */
public class FTP {
    public final String hostName;
    public final String rootDir;
    public final String userName;
    public final String password;

    public FTP(Node e){
        hostName=((Element)e).getElementsByTagName("HostName").item(0).getTextContent();
        rootDir=((Element)e).getElementsByTagName("RootDir").item(0).getTextContent();
        userName=((Element)e).getElementsByTagName("UserName").item(0).getTextContent();
        password=((Element)e).getElementsByTagName("PassWord").item(0).getTextContent();
    }

    /**
     * Provides a means to create a custom FTP object mainly for testing purposes.
     * @param hostName
     * @param rootDir
     * @param userName
     * @param password
     */
    public FTP(String hostName, String rootDir, String userName, String password)
    {
        this.hostName = hostName;
        this.rootDir = rootDir;
        this.userName = userName;
        this.password = password;
    }
}
