/**
 *
 */
package version2.prototype.PluginMetaData;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import version2.prototype.util.FileSystem;

/**
 * @author michael.devos
 *
 */
public class DownloadMetaData extends ProcessMetaData {
    private NodeList nList;

    public final String name;   // Attribute defined
    public final String mode;   // the protocol type: ftp or http
    public final FTP myFtp;
    public final HTTP myHttp;
    public final String downloadFactoryClassName;
    public final String timeZone;
    public final int filesPerDay;
    public final Pattern datePattern;
    public final Pattern fileNamePattern;
    public final ArrayList<DownloadMetaData> extraDownloads;
    public final LocalDate originDate;

    public DownloadMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, NodeList n) throws DOMException,
    PatternSyntaxException
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        String tempName = null;
        String tempMode = null;
        FTP tempFtp = null;
        HTTP tempHttp = null;
        String tempDownloadFactoryClassName = null;
        String tempTimeZone = null;
        int tempFilesPerDay = -1;
        Pattern tempDatePattern = null;
        Pattern tempFileNamePattern = null;
        Node dataNode = null;
        int dataNodeIdx = -1;

        nList = n;

        // If there are multiple Download elements then find the Download element with attribute Name="Data"
        Element temp;
        if(nList.getLength() > 1)
        {
            for(int i=0; i < nList.getLength(); i++)
            {
                temp = (Element) nList.item(i);
                if(temp.getNodeName().equals("Download") && temp.hasAttribute("Name") && temp.getAttribute("Name").toLowerCase().equals("data"))
                {
                    dataNode = nList.item(i);
                    dataNodeIdx = i;
                    tempName = "data";
                    break;
                }
                else if(!temp.hasAttribute("Name"))
                {
                    throw new DOMException((short) 0, "A Download element is missing the attribute \"Name\".");
                }
            }

            if(tempName != null && dataNode != null) {
                name = FileSystem.StandardizeName(tempName);
            }
            else {
                throw new DOMException((short) 0, "Missing Download element with attribute Name=\"Date\".");
            }
        }
        else
        {
            dataNode = nList.item(0);
            temp = (Element) dataNode;
            if((temp.hasAttribute("Name") && temp.getAttribute("Name").toLowerCase().equals("data")) || !temp.hasAttribute("Name"))
            {
                name = "data";
            } else {
                throw new DOMException((short) 0, "Missing Download element with attribute Name=\"Date\".");
            }
        }

        // Set properties
        Element dataElement = (Element) dataNode;
        tempTimeZone = dataElement.getElementsByTagName("TimeZone").item(0).getTextContent();
        tempDownloadFactoryClassName = dataElement.getElementsByTagName("DownloadFactoryClassName").item(0).getTextContent();
        tempMode = dataElement.getElementsByTagName("Mode").item(0).getTextContent();
        tempMode = tempMode.toUpperCase();

        if(tempMode.equalsIgnoreCase("FTP")) {
            tempFtp = new FTP(dataElement.getElementsByTagName(tempMode).item(0));
        } else {
            tempHttp = new HTTP(dataElement.getElementsByTagName(tempMode).item(0));
        }

        tempFilesPerDay = Integer.parseInt(dataElement.getElementsByTagName("FilesPerDay").item(0).getTextContent());
        tempDatePattern = Pattern.compile(dataElement.getElementsByTagName("DatePattern").item(0).getTextContent());
        tempFileNamePattern = Pattern.compile(dataElement.getElementsByTagName("FileNamePattern").item(0).getTextContent());

        mode = tempMode;
        myFtp = tempFtp;
        myHttp = tempHttp;
        downloadFactoryClassName = tempDownloadFactoryClassName;
        timeZone = tempTimeZone;
        filesPerDay = tempFilesPerDay;
        datePattern = tempDatePattern;
        fileNamePattern = tempFileNamePattern;

        Element originDateElement = (Element) dataElement.getElementsByTagName("OriginDate").item(0);
        int dayOfMonth = Integer.parseInt(originDateElement.getElementsByTagName("DayOfMonth").item(0).getTextContent());
        String month = originDateElement.getElementsByTagName("Month").item(0).getTextContent();
        int year = Integer.parseInt(originDateElement.getElementsByTagName("Year").item(0).getTextContent());
        originDate = LocalDate.of(year, Month.valueOf(month.toUpperCase()), dayOfMonth);

        if(nList.getLength() > 1)
        {
            extraDownloads = new ArrayList<DownloadMetaData>();
            for(int i=0; i < nList.getLength(); i++)
            {
                if(i != dataNodeIdx)
                {
                    extraDownloads.add(new DownloadMetaData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, nList.item(i), timeZone, filesPerDay, originDate));
                }
            }
        } else {
            extraDownloads = new ArrayList<DownloadMetaData>();
        }
    }

    /**
     * Provides a means to create a custom DownloadMetaData object mainly for testing purposes. Used if no extra downloads are specified (only one Download section in meta data). Name field will is
     * defaulted to "Data".
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param mode
     * @param myFtp
     * @param myHttp
     * @param downloadFactoryClassName
     * @param timeZone
     * @param filesPerDay
     * @param datePatternStr
     * @param fileNamePatternStr
     * @param originDate
     * @throws PatternSyntaxException
     */
    public DownloadMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, String mode, FTP myFtp, HTTP myHttp,
            String downloadFactoryClassName, String timeZone, int filesPerDay, String datePatternStr, String fileNamePatternStr, LocalDate originDate) throws PatternSyntaxException
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        name = "data";
        this.mode = mode;
        this.myFtp = myFtp;
        this.myHttp = myHttp;
        this.downloadFactoryClassName = downloadFactoryClassName;
        this.timeZone = timeZone;
        this.filesPerDay = filesPerDay;
        if(datePatternStr != null) {
            datePattern = Pattern.compile(datePatternStr);
        } else {
            datePattern = null;
        }
        if(fileNamePatternStr != null) {
            fileNamePattern = Pattern.compile(fileNamePatternStr);
        } else {
            fileNamePattern = null;
        }
        extraDownloads = null;
        this.originDate = originDate;
    }

    /**
     * Provides a means to create a custom DownloadMetaData object mainly for testing purposes. Used if extra downloads are specified. Will need to create those extras using a different method (to be
     * accurate the CreateDownloadMetaData(String name, ...) should be used.
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param mode
     * @param myFtp
     * @param myHttp
     * @param downloadFactoryClassName
     * @param timeZone
     * @param filesPerDay
     * @param datePatternStr
     * @param fileNamePatternStr
     * @param extraDownloads
     * @param originDate
     * @throws PatternSyntaxException
     */
    public DownloadMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, String mode, FTP myFtp, HTTP myHttp,
            String downloadFactoryClassName, String timeZone, int filesPerDay, String datePatternStr, String fileNamePatternStr, ArrayList<DownloadMetaData> extraDownloads, LocalDate originDate)
                    throws PatternSyntaxException
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        name = "data";
        this.mode = mode;
        this.myFtp = myFtp;
        this.myHttp = myHttp;
        this.downloadFactoryClassName = downloadFactoryClassName;
        this.timeZone = timeZone;
        this.filesPerDay = filesPerDay;
        if(datePatternStr != null) {
            datePattern = Pattern.compile(datePatternStr);
        } else {
            datePattern = null;
        }
        if(fileNamePatternStr != null) {
            fileNamePattern = Pattern.compile(fileNamePatternStr);
        } else {
            fileNamePattern = null;
        }
        this.extraDownloads = extraDownloads;
        this.originDate = originDate;
    }

    /**
     * Provides a means to create a custom DownloadMetaData object mainly for testing purposes. Creates a DownloadMetaData object to represent one of the extra downloads.
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param name
     * @param mode
     * @param myFtp
     * @param myHttp
     * @param downloadFactoryClassName
     * @param timeZone
     * @param filesPerDay
     * @param datePatternStr
     * @param fileNamePatternStr
     * @param originDate
     * @throws PatternSyntaxException
     */
    public DownloadMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, String name, String mode, FTP myFtp,
            HTTP myHttp, String downloadFactoryClassName, String timeZone, int filesPerDay, String datePatternStr, String fileNamePatternStr, LocalDate originDate) throws PatternSyntaxException
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        this.name = name.toLowerCase();
        this.mode = mode;
        this.myFtp = myFtp;
        this.myHttp = myHttp;
        this.downloadFactoryClassName = downloadFactoryClassName;
        this.timeZone = timeZone;
        this.filesPerDay = filesPerDay;
        if(datePatternStr != null) {
            datePattern = Pattern.compile(datePatternStr);
        } else {
            datePattern = null;
        }
        if(fileNamePatternStr != null) {
            fileNamePattern = Pattern.compile(fileNamePatternStr);
        } else {
            fileNamePattern = null;
        }
        extraDownloads = new ArrayList<DownloadMetaData>();
        this.originDate = originDate;
    }

    private DownloadMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, Node extraDownloadNode, String defaultTimeZone,
            int defaultFilesPerDay, LocalDate dataOriginDate) throws PatternSyntaxException, DOMException
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        String tempMode = null;
        FTP tempFtp = null;
        HTTP tempHttp = null;
        String tempDownloadFactoryClassName = null;
        String tempTimeZone = null;
        int tempFilesPerDay = -1;
        Pattern tempDatePattern = null;
        Pattern tempFileNamePattern = null;
        nList = null;
        extraDownloads = new ArrayList<DownloadMetaData>();

        // Set properties
        if(((Element) extraDownloadNode).hasAttribute("Name")) {
            name = FileSystem.StandardizeName(((Element) extraDownloadNode).getAttribute("Name")).toLowerCase();
        } else {
            throw new DOMException((short) 0, "A Download element is missing the attribute \"Name\".");
        }

        if(((Element) extraDownloadNode).getElementsByTagName("TimeZone").getLength() > 0) {
            tempTimeZone = ((Element) extraDownloadNode).getElementsByTagName("TimeZone").item(0).getTextContent();
        } else {
            tempTimeZone = defaultTimeZone;
        }
        tempDownloadFactoryClassName = ((Element) extraDownloadNode).getElementsByTagName("DownloadFactoryClassName").item(0).getTextContent();
        tempMode = ((Element) extraDownloadNode).getElementsByTagName("Mode").item(0).getTextContent();
        tempMode = tempMode.toUpperCase();

        if(tempMode.equalsIgnoreCase("FTP")) {
            tempFtp = new FTP(((Element)extraDownloadNode).getElementsByTagName(tempMode).item(0));
        } else {
            tempHttp = new HTTP(((Element)extraDownloadNode).getElementsByTagName(tempMode).item(0));
        }

        if(((Element) extraDownloadNode).getElementsByTagName("FilesPerDay").getLength() > 0) {
            tempFilesPerDay = Integer.parseInt(((Element) extraDownloadNode).getElementsByTagName("FilesPerDay").item(0).getTextContent());
        } else {
            tempFilesPerDay = defaultFilesPerDay;
        }
        tempDatePattern = Pattern.compile(((Element) extraDownloadNode).getElementsByTagName("DatePattern").item(0).getTextContent());
        tempFileNamePattern = Pattern.compile(((Element) extraDownloadNode).getElementsByTagName("FileNamePattern").item(0).getTextContent());

        mode = tempMode;
        myFtp = tempFtp;
        myHttp = tempHttp;
        downloadFactoryClassName = tempDownloadFactoryClassName;
        timeZone = tempTimeZone;
        filesPerDay = tempFilesPerDay;
        datePattern = tempDatePattern;
        fileNamePattern = tempFileNamePattern;
        originDate = dataOriginDate;
    }

}
