package version2.prototype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import version2.prototype.Scheduler.ProcessName;
import version2.prototype.Scheduler.Scheduler;
import version2.prototype.util.FileSystem;
import version2.prototype.util.GeneralUIEventObject;

/**
 * @author michael.devos
 *
 */
public final class ErrorLog {
    private ErrorLog() {
        // Don't allow instantiation
    }

    private static final Object sErrorLogLock = new Object();


    /**Reports an error to the path of the class loader or where the program was executed from. Used mainly to handle Config issues as the others require a Config instance.
     * @param message
     * @param e
     */
    public static void add(String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = null;
        String logFileName = getLogFileName();
        try {
            logPath = ClassLoader.getSystemClassLoader().getResource(".").toURI().getPath();
            while(logPath.startsWith("\\") || logPath.startsWith("/")) {
                logPath = logPath.substring(1);
            }
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        processError(message, e, logPath, logFileName);
    }

    /**
     * Reports an error to the error log in the error log directory root.
     * @param configInstance
     * @param message Error message, suitable for presentation to the user
     * @param e Cause of the error, may be null
     */
    public static void add(Config configInstance, String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = configInstance.getErrorLogDir();
        String logFileName = getLogFileName();
        processError(message, e, logPath, logFileName);
    }

    /**
     * Reports an error to the error log for the specified GlobalDownloader.
     * @param configInstance
     * @param pluginName
     * @param dataName  - name of the data files
     * @param message Error message, suitable for presentation to the user
     * @param e Cause of the error, may be null
     */
    public static void add(Config configInstance, String pluginName, String dataName, String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = configInstance.getErrorLogDir();
        String logFileName = getLogFileName();
        try {
            logPath = FileSystem.GetGlobalDownloadDirectory(configInstance, pluginName, dataName);
        } catch (ConfigReadException cause) {
            add(configInstance, "Problem logging error.", cause);
        }
        processError(message, e, logPath, logFileName);
    }

    /**
     * Reports an error to the error log for the specified project. Sends the custom message as a new log message to the UI.
     * @param scheduler
     * @param message Error message, suitable for presentation to the user
     * @param e Cause of the error, may be null
     */
    public static void add(Scheduler scheduler, String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = FileSystem.GetProjectDirectoryPath(scheduler.projectInfoFile.GetFullPath());
        String logFileName = getLogFileName();
        String finalPath = processError(message, e, logPath, logFileName);
        scheduler.NotifyUI(new GeneralUIEventObject(e.getCause() != null ? e.getCause() : e, message + " [Error Logged: " + finalPath + "]"));
    }

    /**
     * Reports an error to the error log for the specified project. Sends the custom message as a new log message to the UI.
     * @param process
     * @param message Error message, suitable for presentation to the user
     * @param e Cause of the error, may be null
     */
    public static void add(Process process, String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = FileSystem.GetProjectDirectoryPath(process.projectInfoFile.GetFullPath());
        String logFileName = getLogFileName();
        String finalPath = processError(message, e, logPath, logFileName);
        process.NotifyUI(new GeneralUIEventObject(e.getCause() != null ? e.getCause() : e, message + " [Error Logged: " + finalPath + "]"));
    }

    /**
     * Reports an error to the error log for the specified process and project. Sends the custom message as a new log message to the UI.
     * @param processName
     * @param scheduler
     * @param message Error message, suitable for presentation to the user
     * @param e Cause of the error, may be null
     */
    public static void add(ProcessName processName, Scheduler scheduler, String message, Throwable e)
    {
        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        String logPath = FileSystem.GetProjectDirectoryPath(scheduler.projectInfoFile.GetFullPath());
        String logFileName = processName + "_" + getLogFileName();
        String finalPath = processError(message, e, logPath, logFileName);
        scheduler.NotifyUI(new GeneralUIEventObject(e.getCause() != null ? e.getCause() : e, message + " [Error Logged: " + finalPath + "]"));
    }

    private static String processError(String message, Throwable e, String logPath, String logFileName)
    {
        String finalPath;
        message = message + " [Thread: " + Thread.currentThread().getName() + "]";
        synchronized (sErrorLogLock)
        {
            finalPath = handleLogFileExtensions(logFileName, logPath);
            printToStderr(message, e);
            printToLogFile(finalPath, message, e);
        }
        return finalPath;
    }

    private static void printToLogFile(String logPath, String message, Throwable e)
    {
        PrintStream sErrorLogPrintStream = null;
        try {
            System.out.println(logPath);
            final FileOutputStream fos = new FileOutputStream(logPath);
            sErrorLogPrintStream = new PrintStream(fos);

            sErrorLogPrintStream.println(message);
            Throwable tempError = e;
            int exceptionNum = 1;
            while(tempError != null)
            {
                sErrorLogPrintStream.println("Exception " + exceptionNum + ":");

                if(tempError.getMessage() != null) {
                    sErrorLogPrintStream.println(tempError.getMessage());
                }

                if(tempError.getLocalizedMessage() != null) {
                    sErrorLogPrintStream.println(tempError.getLocalizedMessage());
                }

                if(tempError.getCause() != null) {
                    tempError.getCause().printStackTrace(sErrorLogPrintStream);
                }
                else {
                    tempError.printStackTrace(sErrorLogPrintStream);
                }

                if(tempError instanceof SQLException)
                {
                    SQLException sqlError = (SQLException) tempError;
                    tempError = sqlError.getNextException();
                }
                else
                {
                    tempError = null;
                }
            }
            sErrorLogPrintStream.println();
        } catch (Exception cause) {
            System.err.println("Failed to write to the error log");
        }

        if(sErrorLogPrintStream != null)
        {
            sErrorLogPrintStream.flush();
            sErrorLogPrintStream.close();
        }
    }

    private static void printToStderr(String message, Throwable r) {
        System.err.println("ERROR: " + message);
        if (r != null) {
            if(r.getCause() != null)
            {
                r.getCause().printStackTrace(System.err);
            }
            else
            {
                r.printStackTrace(System.err);
            }
        }
    }

    private static String getLogFileName() {
        LocalDateTime temp = LocalDateTime.now();
        return "Error_Log_" + LocalDate.now().getYear() + "_" + LocalDate.now().getMonthValue() + "_" + LocalDate.now().getDayOfMonth() + "_" + String.format("%02d", temp.getHour())
        + String.format("%02d", temp.getMinute()) + String.format("%02d", temp.getSecond());
    }

    private static String handleLogFileExtensions(String logFileName, String logPath) {
        final String ext =  ".log";
        String finalPath = logPath + logFileName;
        File tempFile = new File(finalPath + ext);
        if(tempFile.exists())
        {
            Integer id = 0;
            do {
                id++;
                finalPath = logPath + logFileName + "_" + id.toString();
                tempFile = new File(finalPath + ext);
            } while(tempFile.exists());
        }
        return finalPath + ext;
    }
}