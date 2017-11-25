package version2.prototype.EastWebUI.SettingsUI;


import javax.swing.*;
import javax.swing.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.*;

import version2.prototype.EastWebUI.DocumentBuilderInstance;


/** Given a filename or a name and an input stream,
 *  this class generates a JTree representing the
 *  XML structure contained in the file or stream.
 *  Parses with DOM then copies the tree structure
 *  (minus text and comment nodes).
 */

@SuppressWarnings("serial")
public class XMLTree extends JTree {
    public XMLTree(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public XMLTree(InputStream in) {
        super(makeRootNode(in));
    }

    public XMLTree(String text){
        super(makeRootNode(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
    }

    // This method needs to be static so that it can be called
    // from the call to the parent constructor (super), which
    // occurs before the object is really built.
    private static DefaultMutableTreeNode makeRootNode(InputStream in) {
        try {
            // Standard DOM code from hereon. The "parse"
            // method invokes the parser and returns a fully parsed
            // Document object. We'll then recursively descend the
            // tree and copy non-text nodes into JTree nodes.
            DocumentBuilderInstance.ClearInstance();
            Document document = DocumentBuilderInstance.Instance().GetDocumentBuilder().parse(in);
            document.getDocumentElement().normalize();
            Element rootElement = document.getDocumentElement();
            DefaultMutableTreeNode rootTreeNode = buildTree(rootElement);

            return(rootTreeNode);
        } catch(Exception e) {
            String errorMessage = "Error making root node: " + e;
            System.err.println(errorMessage);
            e.printStackTrace();

            return(new DefaultMutableTreeNode(errorMessage));
        }
    }

    private static DefaultMutableTreeNode buildTree(Element rootElement) {
        // Make a JTree node for the root, then make JTree
        // nodes for each child and add them to the root node.
        // The addChildren method is recursive.
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(treeNodeLabel(rootElement));
        addChildren(rootTreeNode, rootElement);

        return(rootTreeNode);
    }
    private static void addChildren (DefaultMutableTreeNode parentTreeNode, Node parentXMLElement) {
        // Recursive method that finds all the child elements
        // and adds them to the parent node. We have two types
        // of nodes here: the ones corresponding to the actual
        // XML structure and the entries of the graphical JTree.
        // The convention is that nodes corresponding to the
        // graphical JTree will have the word "tree" in the
        // variable name. Thus, "childElement" is the child XML
        // element whereas "childTreeNode" is the JTree element.
        // This method just copies the non-text and non-comment
        // nodes from the XML structure to the JTree structure.

        NodeList childElements = parentXMLElement.getChildNodes();

        for(int i=0; i<childElements.getLength(); i++) {
            Node childElement = childElements.item(i);

            if (!(childElement instanceof Text || childElement instanceof Comment)) {
                DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(treeNodeLabel(childElement));
                parentTreeNode.add(childTreeNode);
                addChildren(childTreeNode, childElement);
            }
        }
    }

    // If the XML element has no attributes, the JTree node
    // will just have the name of the XML element. If the
    // XML element has attributes, the names and values of the
    // attributes will be listed in parens after the XML
    // element name. For example:
    // XML Element: <blah>
    // JTree Node:  blah
    // XML Element: <blah foo="bar" baz="quux">
    // JTree Node:  blah (foo=bar, baz=quux)

    private static String treeNodeLabel(Node childElement) {
        NamedNodeMap elementAttributes = childElement.getAttributes();
        String treeNodeLabel = childElement.getNodeName();

        if (elementAttributes != null && elementAttributes.getLength() > 0) {
            treeNodeLabel = treeNodeLabel + " (";
            int numAttributes = elementAttributes.getLength();

            for(int i=0; i<numAttributes; i++) {
                Node attribute = elementAttributes.item(i);

                if (i > 0) {
                    treeNodeLabel = treeNodeLabel + ", ";
                }
                treeNodeLabel = treeNodeLabel + attribute.getNodeName() + "=" + attribute.getNodeValue();
            }
            treeNodeLabel = treeNodeLabel + ")" + String.format("; value = %s", childElement.getTextContent());
        }
        return(treeNodeLabel);
    }
}