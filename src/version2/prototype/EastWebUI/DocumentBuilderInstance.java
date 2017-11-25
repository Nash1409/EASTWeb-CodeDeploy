package version2.prototype.EastWebUI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

public class DocumentBuilderInstance {

    private DocumentBuilderInstance ()
    {

    }

    DocumentBuilderFactory docFactory;
    DocumentBuilder docBuilder;
    Document doc;

    public DocumentBuilderFactory GetDocumentBuilderFactory()
    {
        if(docFactory == null) {
            docFactory = DocumentBuilderFactory.newInstance();
        }
        return docFactory;
    }

    public DocumentBuilder GetDocumentBuilder() throws ParserConfigurationException
    {
        if(docBuilder == null) {
            docBuilder = GetDocumentBuilderFactory().newDocumentBuilder();
        }
        return docBuilder;
    }

    public Document GetDocument() throws ParserConfigurationException
    {
        if(doc == null) {
            doc = GetDocumentBuilder().newDocument();
        }
        return doc;
    }

    public static DocumentBuilderInstance Instance()
    {
        if(instance == null) {
            instance = new DocumentBuilderInstance ();
        }
        return instance;
    }
    private static DocumentBuilderInstance instance;

    public static void ClearInstance()
    {
        instance = null;
    }
}
