package version2.prototype.processor;

import java.time.MonthDay;

import version2.prototype.DataDate;
import version2.prototype.Projection;

/* rewritten by Y.L. on May 31st */

public class ProcessData {

    private String[] inputFolders;
    private String outputFolder;
    private String qcLevel;
    private DataDate date;
    private String shapefile;
    private String maskfile;
    private int [] dataBands;
    private int [] qcBands;
    private Projection projection;
    private Integer maskResolution;
    private Integer dataResolution;
    private Boolean clipOrNot;
    private MonthDay freezingDate;
    private MonthDay heatingDate;
    private double freezingDegree;
    private double heatingDegree;
    private Integer noDataValue;

    public ProcessData() { }

    public ProcessData(String [] inputFolders, String outputFolder, DataDate date, String qcLevel, String shapefile,
            String maskfile, int dataBands[], int qcBands[], Projection projection,
            Integer maskResolution, Integer dataResolution, Boolean clipOrNot,
            MonthDay freezingDate, MonthDay heatingDate, double freezingDegree, double heatingDegree, Integer noDataValue)
    {
        this.inputFolders = inputFolders;
        this.outputFolder = outputFolder;
        this.date = date;
        this.qcLevel = qcLevel;
        this.shapefile = shapefile;
        this.maskfile = maskfile;
        this.dataBands = dataBands;
        this.qcBands = qcBands;
        this.projection = projection;
        this.maskResolution = maskResolution;
        this.dataResolution = dataResolution;
        this.clipOrNot = clipOrNot;
        this.freezingDate = freezingDate;
        this.heatingDate = heatingDate;
        this.freezingDegree = freezingDegree;
        this.heatingDegree = heatingDegree;
        this.noDataValue = noDataValue;
    }

    public String [] getInputFolders()
    {   return inputFolders;    }

    public String getOutputFolder()
    {   return outputFolder;    }

    public DataDate getDateDate()
    {   return date;        }

    public String getQCLevel()
    {   return qcLevel;     }

    public String getShapefile()
    {   return shapefile;   }

    public String getMaskfile()
    {   return maskfile;      }

    public int [] getDataBands()
    {   return dataBands;       }

    public int [] getQCBands()
    {   return qcBands;       }

    public Projection getProjection()
    {   return projection;  }

    public Integer getMaskResolution()
    {    return maskResolution; }

    public int getDataResolution()
    {    return dataResolution; }

    // return true for clipping and false for not clipping
    public Boolean getClipOrNot()
    {   return clipOrNot;       }

    public MonthDay getFreezingDate()
    {   return freezingDate;    }

    public MonthDay getHeatingDate()
    {   return heatingDate;     }

    public double getFreezingDegree()
    {   return freezingDegree;  }

    public double getHeatingDegree()
    {   return heatingDegree;   }

    public Integer getNoDataValue()
    {   return noDataValue;     }

}
