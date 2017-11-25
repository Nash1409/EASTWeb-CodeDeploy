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
public class HTTP {
    public final String url;

    public HTTP(Node e){
        url=((Element)e).getElementsByTagName("URL").item(0).getTextContent();
    }

    /**
     * Provides a means to create a custom HTTP object mainly for testing purposes.
     * @param url
     */
    public HTTP(String url)
    {
        this.url = url;
    }
}
