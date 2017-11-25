package version2.prototype.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import version2.prototype.Config;
import version2.prototype.ErrorLog;

public class UnGunZip
{
    public UnGunZip(String gzFile, String outFile)
    {
        byte[] buffer = new byte[1024];

        try {
            FileInputStream fileIn = new FileInputStream(gzFile);
            GZIPInputStream gzInStream = new GZIPInputStream(fileIn);
            FileOutputStream outStream = new FileOutputStream(outFile);

            int bytes_read;

            while ((bytes_read = gzInStream.read(buffer)) > 0)
            {
                outStream.write(buffer, 0, bytes_read);
            }

            gzInStream.close();
            outStream.close();
        } catch (IOException ex) {
            ErrorLog.add(Config.getInstance(), "Cannot unGunzip file: " + gzFile, ex);
        }
    }

}