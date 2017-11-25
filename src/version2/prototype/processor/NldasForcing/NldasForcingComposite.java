package version2.prototype.processor.NldasForcing;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.processor.Composite;
import version2.prototype.processor.ProcessData;
import version2.prototype.util.GdalUtils;

public class NldasForcingComposite extends Composite
{
    private static MonthDay hDate;
    private static double hDegree;

    private static MonthDay fDate;
    private static double fDegree;

    private int[] mBands;

    private Integer noDataValue;

    public NldasForcingComposite(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);

        mBands = data.getDataBands();

        hDate = data.getHeatingDate();
        hDegree = data.getHeatingDegree();

        fDate = data.getFreezingDate();
        fDegree = data.getFreezingDegree();

        noDataValue = data.getNoDataValue();
    }

    @Override
    public void composeFiles()
    {
        GdalUtils.register();
        synchronized (GdalUtils.lockObject) {

            if(!(new File(outputFolder).exists())){
                new File(outputFolder).mkdirs();
                //                try { FileUtils.forceMkdir(new File(outputFolder)); }
                //                catch (IOException e) { ErrorLog.add(Config.getInstance(), "NldasForcingComposite.composeFiles error", e); }
            }

            List<Dataset> inputDSs = new ArrayList<Dataset>();
            for (File input : inputFiles) {
                inputDSs.add(gdal.Open(input.getPath()));
            }

            for(int band : mBands)
            {
                int rasterX = inputDSs.get(0).GetRasterXSize();
                int rasterY = inputDSs.get(0).GetRasterYSize();

                int outputs = 1;

                if(band == 1) {
                    // Used to differentiate Min--Mean/DegreeDays--Max Air Temp--other
                    outputs = 3;
                }
                else if (band == 2)
                {
                    outputs = 2;
                }

                for(int output = 0; output < outputs; output++)
                {
                    ArrayList<String> prefixList = GetFilePrefix(band, output);

                    for(String prefix : prefixList)
                    {
                        File temp = new File(outputFolder + "\\" + prefix + ".tif");
                        try {
                            temp.createNewFile();
                        } catch (IOException e) {
                            ErrorLog.add(Config.getInstance(), "NldasForcingComposite.composeFiles error while creating new file.", e);
                        }

                        Dataset outputDS = gdal.GetDriverByName("GTiff").Create(
                                temp.getAbsolutePath(),
                                rasterX, rasterY,
                                1,
                                gdalconstConstants.GDT_Float32
                                );

                        outputDS.SetGeoTransform(inputDSs.get(0).GetGeoTransform());
                        outputDS.SetProjection(inputDSs.get(0).GetProjection());
                        outputDS.SetMetadata(inputDSs.get(0).GetMetadata_Dict());
                        outputDS.GetRasterBand(1).WriteRaster(0, 0, rasterX, rasterY, GetOutputArray(band, output, inputDSs, rasterX, rasterY, prefix));
                        outputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
                        outputDS.GetRasterBand(1).ComputeStatistics(true);

                        outputDS.delete();
                    }
                }
            }
            for (Dataset inputDS : inputDSs) {
                inputDS.delete();
            }
        }
    }

    private ArrayList<String> GetFilePrefix(int band, int output)
    {
        ArrayList<String> prefixList = new ArrayList<String>();
        String prefix = "";

        if(band == 1)
        {
            if(output == 0) {
                prefix = "AirTemp_Min";
            }
            else if(output == 1) {
                prefixList.add("HeatingDegreeDays");
                prefixList.add("FreezingDegreeDays");
                prefixList.add("WNVAmplificationIndex");
                prefixList.add("LymeDiseaseIndex");
                prefixList.add("OverwinteringIndex");
                prefix = "AirTemp_Mean";
            }
            else if(output == 2) {
                prefix = "AirTemp_Max";
            }
        }
        else if (band == 2) {
            if(output == 0)
            {
                prefix = "Humidity_Mean";
            }
            else if(output == 1)
            {
                prefixList.add("Max_Heat_Index");
                prefixList.add("Mean_Heat_Index");
                prefix = "Relative_Humidity_Mean";
            }
        }
        else if (band == 4)
        {
            prefixList.add("Max_Windspeed");
            prefix = "Mean_Windspeed";

        }
        else if (band == 10) {
            prefix = "Precip_Total";
        }
        prefixList.add(prefix);

        return prefixList;
    }

    private double[] GetOutputArray(int band, int output, List<Dataset> inputDSs, int rasterX, int rasterY, String prefix)
    {
        int size = inputDSs.size();
        double[][] inputArrays = new double[size][rasterX * rasterY];
        double[] outputArray = new double[rasterX * rasterY];
        int length = outputArray.length;
        double sum;

        Arrays.fill(outputArray, 9999.0);

        // AirTemp_Mean, Humidity Mean
        //HeatingDegreeDays, FreezingDegreeDays, WNVAmplificationIndex, LymeDiseaseIndex, OverwinteringIndex
        if ((band == 1 && output == 1) || (band == 2 && output == 0))
        {
            for(int index = 0; index < size; index++)
            {
                inputDSs.get(index).GetRasterBand(band).ReadRaster(0, 0, rasterX, rasterY, inputArrays[index]);
            }

            // Always take the average
            for(int pos = 0; pos < length; pos++)
            {
                sum = 0.0;
                for (int i = 0; i < size; i++)
                {
                    // Get the proportional average for each input.
                    if(inputArrays[i][pos] != 9999.0)
                    {
                        outputArray[pos] = sum + (inputArrays[i][pos]/size);
                        sum = outputArray[pos];
                    }
                }
            }


            if(band == 1)
            {
                // Convert the values from Kelvin to Celsius
                //for(int i = 0; i < outputArray.length; i++) {
                // Tc = Tk - 273.15
                //    outputArray[i] = outputArray[i] - 273.15;
                //}

                if(prefix.equalsIgnoreCase("HeatingDegreeDays")) {
                    outputArray = GetCumulativeHeatingDegreeDays(outputArray, prefix);
                }
                else if (prefix.equalsIgnoreCase("FreezingDegreeDays")) {
                    outputArray = GetCumulativeFreezingDegreeDays(outputArray, prefix);
                }
                else if (prefix.equalsIgnoreCase("WNVAmplificationIndex"))
                {
                    outputArray = GetCumulativeWNVAmpDays(outputArray, prefix);
                }
                else if (prefix.equalsIgnoreCase("LymeDiseaseIndex"))
                {
                    outputArray = GetCumulativeLymeDiseaseDays(outputArray, prefix);
                }
                else if (prefix.equalsIgnoreCase("OverwinteringIndex"))
                {
                    outputArray = GetCumulativeOverwinteringDays(outputArray, prefix);
                }
            }
        }
        //AirTemp_Min, AirTemp_Max
        else if(band == 1)
        {
            if(output == 0) {
                outputArray = FindMinValues(inputDSs, rasterX, rasterY);
            }
            else if(output == 2) {
                outputArray = FindMaxValues(inputDSs, rasterX, rasterY);
            }
        }
        //Max_Heat_Index, Mean_Heat_Index, Relative_Humidity_Mean
        else if(band == 2)
        {
            outputArray = FindRelativeHumidity(inputDSs, rasterX, rasterY, prefix);
        }
        //Max_Windspeedd, Mean_Windspeed
        else if (band == 4)
        {
            outputArray = FindWindspeed(inputDSs, rasterX, rasterY, prefix);
        }
        //Precip_Total
        else if(band == 10)
        {
            for(int index = 0; index < size; index++)
            {
                inputDSs.get(index).GetRasterBand(band).ReadRaster(0, 0, rasterX, rasterY, inputArrays[index]);
            }

            for(int pos = 0; pos < length; pos++)
            {
                // band 10 == precipitation hourly total, so we don't want the average but rather the total
                // No conversion necessary because the
                // density of water is approximately 1000 kg/m^3,
                // so the total mass of a 1-mm layer of water covering an area of 1 m^2 is 1 kg.
                sum = 0.0;
                for (int i = 0; i < size; i++)
                {
                    if(inputArrays[i][pos] != 9999.0)
                    {
                        outputArray[pos] = sum + inputArrays[i][pos];
                        sum = outputArray[pos];
                    }
                }
            }
        }

        return outputArray;
    }

    private double[] FindMinValues(List<Dataset> inputDSs, int rasterX, int rasterY)
    {
        int size = inputDSs.size();
        double[][] inputArrays = new double[size][rasterX * rasterY];
        double[] outputArray = new double[rasterX * rasterY];
        int length = outputArray.length;

        Arrays.fill(outputArray, 9999.0);

        for(int index = 0; index < size; index++) {
            inputDSs.get(index).GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, inputArrays[index]);
        }

        for(int pos = 0; pos < length; pos++)
        {
            double minVal = inputArrays[0][pos];
            for (int i = 0; i < size; i++) {
                // Fill value is 9999.0
                if(inputArrays[i][pos] != 9999.0 && inputArrays[i][pos] < minVal) {
                    minVal = inputArrays[i][pos];
                }
            }
            outputArray[pos] = minVal;
        }

        //for(int i = 0; i < outputArray.length; i++) {
        // Tc = Tk - 273.15
        //outputArray[i] = outputArray[i] - 273.15;
        //}

        return outputArray;
    }

    private double[] FindMaxValues(List<Dataset> inputDSs, int rasterX, int rasterY)
    {
        int size = inputDSs.size();
        double[][] inputArrays = new double[size][rasterX * rasterY];
        double[] outputArray = new double[rasterX * rasterY];
        int length = outputArray.length;

        Arrays.fill(outputArray, 9999.0);

        for(int index = 0; index < size; index++) {
            inputDSs.get(index).GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, inputArrays[index]);
        }

        for(int pos = 0; pos < length; pos++)
        {
            double maxVal = inputArrays[0][pos];
            for (int i = 0; i < size; i++) {
                // Fill value is 9999.
                if(inputArrays[i][pos] != 9999.0 && inputArrays[i][pos] > maxVal) {
                    maxVal = inputArrays[i][pos];
                }
            }
            outputArray[pos] = maxVal;
        }

        //for(int i = 0; i < outputArray.length; i++) {
        //    // Tc = Tk - 273.15
        //outputArray[i] = outputArray[i] - 273.15;
        //}

        return outputArray;
    }

    private double [] FindRelativeHumidity(List<Dataset> inputDSs, int rasterX, int rasterY, String prefix)
    {
        int size = inputDSs.size();
        double[][] PArrays = new double[size][rasterX * rasterY];
        double[][] TArrays = new double[size][rasterX * rasterY];
        double[][] TArrays2 = new double[size][rasterX * rasterY];
        double[][] SHArrays = new double[size][rasterX * rasterY];
        double[][] RHArray = new double[size][rasterX * rasterY];
        double[][] HIArray = new double[size][rasterX * rasterY];
        double[] outputArray = new double[rasterX * rasterY];
        int length = outputArray.length;
        double sum;

        Arrays.fill(outputArray, 9999.0);

        for(int index = 0; index < size; index++) {
            inputDSs.get(index).GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, TArrays[index]);
            inputDSs.get(index).GetRasterBand(2).ReadRaster(0, 0, rasterX, rasterY, SHArrays[index]);
            inputDSs.get(index).GetRasterBand(3).ReadRaster(0, 0, rasterX, rasterY, PArrays[index]);
        }


        for(int pos = 0; pos < length; pos++)
        {
            for (int i = 0; i < size; i++) {
                if(TArrays2[i][pos] != 9999.0) {
                    TArrays2[i][pos] = TArrays2[i][pos] + 273.15;
                }
                else
                {
                    TArrays2[i][pos] = TArrays2[i][pos];
                }
            }
        }


        for(int pos = 0; pos < length; pos++)
        {
            for (int i = 0; i < size; i++) {
                if(PArrays[i][pos] != 9999.0 && TArrays2[i][pos] != 9999.0 && SHArrays[i][pos] != 9999.0) {
                    RHArray[i][pos] =0.263 * PArrays[i][pos] * SHArrays[i][pos]
                            * (1 / (Math.exp((17.67 * (TArrays2[i][pos] - 273.15)) / (TArrays2[i][pos] - 29.75))));
                }
                else
                {
                    RHArray[i][pos] = 9999.0;
                }
            }
        }

        if(prefix.equalsIgnoreCase("Max_Heat_Index") | prefix.equalsIgnoreCase("Mean_Heat_Index"))
        {
            for(int pos = 0; pos < length; pos++)
            {
                for (int i = 0; i < size; i++) {
                    if(RHArray[i][pos] != 9999.0) {
                        RHArray[i][pos] = RHArray[i][pos] / 100;
                    }
                    else
                    {
                        RHArray[i][pos] = RHArray[i][pos];
                    }
                }
            }

            for(int pos = 0; pos < length; pos++)
            {
                for (int i = 0; i < size; i++) {
                    if(RHArray[i][pos] != 9999.0 && TArrays[i][pos] != 9999.0 && TArrays[i][pos] > 20) {
                        HIArray[i][pos] = -8.784695 + 1.61139411 * TArrays[i][pos] + 2.338549 * RHArray[i][pos]
                                - 0.14611605 * TArrays[i][pos] * RHArray[i][pos] - 0.012308094 * (TArrays[i][pos] * TArrays[i][pos])
                                - 0.016424828 * (RHArray[i][pos] * RHArray[i][pos]) + 0.002211732 * (TArrays[i][pos] * TArrays[i][pos])
                                * RHArray[i][pos] + 0.00072546 * TArrays[i][pos] * (RHArray[i][pos] * RHArray[i][pos]) - 0.000003582
                                * (TArrays[i][pos] * TArrays[i][pos]) * (RHArray[i][pos] * RHArray[i][pos]);
                    }
                    else
                    {
                        HIArray[i][pos] = TArrays[i][pos];
                    }
                }
            }
        }


        if(prefix.equalsIgnoreCase("Max_Heat_Index"))
        {
            for(int pos = 0; pos < length; pos++)
            {
                double maxVal = HIArray[0][pos];
                for (int i = 0; i < size; i++) {
                    if(HIArray[i][pos] != 9999.0 && HIArray[i][pos] > maxVal) {
                        maxVal = HIArray[i][pos];
                    }
                }
                outputArray[pos] = maxVal;
            }
        }
        else if(prefix.equalsIgnoreCase("Mean_Heat_Index"))
        {
            for(int pos = 0; pos < length; pos++)
            {
                sum = 0.0;
                for (int i = 0; i < size; i++) {
                    if(HIArray[i][pos] != 9999.0) {

                        outputArray[pos] = sum + (HIArray[i][pos] /size);
                        sum = outputArray[pos];
                    }
                }
            }

        }
        else
        {
            for(int pos = 0; pos < length; pos++)
            {
                sum = 0.0;
                for (int i = 0; i < size; i++) {
                    if(RHArray[i][pos] != 9999.0) {

                        outputArray[pos] = sum + (RHArray[i][pos] /size);
                        sum = outputArray[pos];
                    }
                }
            }
        }


        return outputArray;
    }

    private double[] FindWindspeed(List<Dataset> inputDSs, int rasterX, int rasterY, String prefix)
    {
        int size = inputDSs.size();
        double[][] UArrays = new double[size][rasterX * rasterY];
        double[][] VArrays = new double[size][rasterX * rasterY];
        double[][] calcArray = new double[size][rasterX * rasterY];
        double[] outputArray = new double[rasterX * rasterY];
        int length = outputArray.length;
        double sum;

        Arrays.fill(outputArray, 9999.0);

        for(int index = 0; index < size; index++) {
            inputDSs.get(index).GetRasterBand(4).ReadRaster(0, 0, rasterX, rasterY, UArrays[index]);
            inputDSs.get(index).GetRasterBand(5).ReadRaster(0, 0, rasterX, rasterY, VArrays[index]);
        }

        for(int pos = 0; pos < length; pos++)
        {
            for (int i = 0; i < size; i++) {
                if(UArrays[i][pos] != 9999.0 && VArrays[i][pos] != 9999.0) {
                    calcArray[i][pos] = Math.sqrt((UArrays[i][pos] * UArrays[i][pos]) + (VArrays[i][pos] * VArrays[i][pos]));
                }
                else
                {
                    calcArray[i][pos] = 9999.0;
                }
            }
        }

        if(prefix.equalsIgnoreCase("Max_Windspeed"))
        {
            for(int pos = 0; pos < length; pos++)
            {
                double maxVal = calcArray[0][pos];
                for (int i = 0; i < size; i++) {
                    if(calcArray[i][pos] != 9999.0 && calcArray[i][pos] > maxVal) {
                        maxVal = calcArray[i][pos];
                    }
                }
                outputArray[pos] = maxVal;
            }
        }
        else
        {
            for(int pos = 0; pos < length; pos++)
            {
                sum = 0.0;

                for (int i = 0; i < size; i++) {
                    if(calcArray[i][pos] != 9999.0) {

                        outputArray[pos] = sum + (calcArray[i][pos] /size);
                        sum = outputArray[pos];
                    }
                }
            }
        }


        return outputArray;
    }

    private double[] GetCumulativeHeatingDegreeDays(double[] meanValues, String prefix)
    {
        int length = meanValues.length;
        double[] cumulative = null; //GetPreviousValues(prefix, hDate);

        for(int i = 0; i < length; i++)
        {
            double previousVal = 0.0;
            if(cumulative != null) {
                previousVal = cumulative[i];
            }
            else if(cumulative == null && meanValues[i] == 9999.0)
            {
                previousVal = meanValues[i];
            }

            // Fill value is 9999.0
            if(meanValues[i] != 9999.0 && meanValues[i] > hDegree) {
                meanValues[i] = previousVal + (meanValues[i] - hDegree);
            } else {
                meanValues[i] = previousVal;
            }
        }

        return meanValues;
    }

    private double[] GetCumulativeFreezingDegreeDays(double[] meanValues, String prefix)
    {
        int length = meanValues.length;
        double[] cumulative = null; //GetPreviousValues(prefix, fDate);

        for(int i = 0; i < length; i++)
        {
            double previousVal = 0.0;
            if(cumulative != null) {
                previousVal = cumulative[i];
            }
            else if(cumulative == null && meanValues[i] == 9999.0)
            {
                previousVal = meanValues[i];
            }

            // Fill value is 9999.0
            if(meanValues[i] != 9999.0 && meanValues[i] < fDegree) {
                meanValues[i] = previousVal + (fDegree - meanValues[i]);
            } else {
                meanValues[i] = previousVal;
            }
        }

        return meanValues;
    }

    private double[] GetCumulativeWNVAmpDays(double[] meanValues, String prefix)
    {
        int length = meanValues.length;
        double[] cumulative = null; //GetPreviousValues(prefix, MonthDay.of(1,1));
        double degree = 14.3;

        for(int i = 0; i < length; i++)
        {
            double previousVal = 0.0;
            if(cumulative != null) {
                previousVal = cumulative[i];
            }
            else if(cumulative == null && meanValues[i] == 9999.0)
            {
                previousVal = meanValues[i];
            }

            // Fill value is 9999.0
            if(meanValues[i] != 9999.0 && meanValues[i] > degree) {
                meanValues[i] = previousVal + (meanValues[i] - degree);
            } else {
                meanValues[i] = previousVal;
            }
        }

        return meanValues;
    }

    private double[] GetCumulativeLymeDiseaseDays(double[] meanValues, String prefix)
    {
        int length = meanValues.length;
        double[] cumulative = null; //GetPreviousValues(prefix, MonthDay.of(1, 1));
        double degree = 0.0;

        for(int i = 0; i < length; i++)
        {
            double previousVal = 0.0;
            if(cumulative != null) {
                previousVal = cumulative[i];
            }
            else if(cumulative == null && meanValues[i] == 9999.0)
            {
                previousVal = meanValues[i];
            }

            // Fill value is 9999.0
            if(meanValues[i] != 9999.0 && meanValues[i] > degree) {
                meanValues[i] = previousVal + (meanValues[i] - degree);
            } else {
                meanValues[i] = previousVal;
            }
        }

        return meanValues;
    }

    private double[] GetCumulativeOverwinteringDays(double[] meanValues, String prefix)
    {
        int length = meanValues.length;
        double[] cumulative = null; //GetPreviousValues(prefix, MonthDay.of(7, 1));
        double degree = 0.0;

        for(int i = 0; i < length; i++)
        {
            double previousVal = 0.0;
            if(cumulative != null) {
                previousVal = cumulative[i];
            }
            else if(cumulative == null && meanValues[i] == 9999.0)
            {
                previousVal = meanValues[i];
            }

            // Fill value is 9999.0
            if(meanValues[i] != 9999.0 && meanValues[i] < degree) {
                meanValues[i] = previousVal + (degree - meanValues[i]);
            } else {
                meanValues[i] = previousVal;
            }
        }

        return meanValues;
    }

    private double[] GetPreviousValues(String prefix, MonthDay start)
    {
        double[] cumulative = null;
        File yesterdayFile = null;

        try
        {
            int year = Integer.parseInt(inputFolder.getParentFile().getName());
            DataDate startDate = new DataDate(start.getDayOfMonth(), start.getMonthValue(), year);

            // if we're currently processing the start date,
            // we don't want to continue with the previous run of heating degree days
            if(!(inputFolder.getName().equalsIgnoreCase(String.format("%03d", startDate.getDayOfYear()))))
            {
                //Get the day before's values
                File yesterdayFolder = null;
                int currentDayOfYear = Integer.parseInt(inputFolder.getName());
                LocalDate leap = LocalDate.ofYearDay(year-1, 1);

                if(currentDayOfYear > 1) {
                    yesterdayFolder = new File(new File(outputFolder).getParentFile().getAbsolutePath() + File.separator
                            + String.format("%03d", (currentDayOfYear-1)));
                }
                else {
                    if(leap.isLeapYear())
                    {
                        yesterdayFolder = new File(new File(outputFolder).getParentFile().getParentFile().getAbsolutePath() + File.separator
                                + String.format("%04d", (year-1)) + File.separator + "366");
                    }
                    else
                    {
                        // Previous day would be last year and day of year = 365
                        yesterdayFolder = new File(new File(outputFolder).getParentFile().getParentFile().getAbsolutePath() + File.separator
                                + String.format("%04d", (year-1)) + File.separator + "365");
                    }
                }

                if(yesterdayFolder != null && yesterdayFolder.exists())
                {
                    for(File file : yesterdayFolder.listFiles())
                    {
                        if(file.getName().contains(prefix)) {
                            yesterdayFile = file;
                            break;
                        }
                    }
                }
            }
        }
        catch(NumberFormatException e)
        {
            ErrorLog.add(Config.getInstance(), "NldasForcingComposite.GetPreviousValues error.", e);
        }

        if(yesterdayFile != null) {
            Dataset ds = gdal.Open(yesterdayFile.getPath());
            int rasterX = ds.GetRasterXSize();
            int rasterY = ds.GetRasterYSize();

            cumulative = new double[rasterX * rasterY];
            ds.GetRasterBand(1).ReadRaster(0, 0, rasterX, rasterY, cumulative);
        }

        return cumulative;
    }
}
