/**
 *
 */
package version2.prototype.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import version2.prototype.Config;
import version2.prototype.ErrorLog;

/**
 * @author michael.devos
 *
 */
public class DatabaseConnector {
    private static Integer connectionCount = 0;
    private static Config configInstance = null;
    private static Boolean configLock = true;   // Value doesn't matter.
    private static Map<Integer, DatabaseConnection> connections = new HashMap<Integer, DatabaseConnection>();
    private static Boolean DatabaseConnectorLock = true;  // Value doesn't matter.
    private static BitSet connectionIDs = null;
    private static Boolean connectionIDsLock = true;    // Value doesn't matter.
    private static DatabaseConnectionPoolA connectionPool = null;

    protected DatabaseConnector()
    {
        // avoid public instantiation
    }

    /**
     * Safely closes the connection pool and open resources if it has been instantiated.
     */
    public static void Close()
    {
        if(connectionPool != null) {
            connectionPool.close();
        }
    }

    /**
     * Creates a valid Connection object if a connection could be established with the PostgreSQL database using information from config.xml.
     * @return valid Connection object if successfully connected, otherwise null
     */
    public static DatabaseConnection getConnection()
    {
        synchronized(configLock)
        {
            if(configInstance == null)
            {
                configInstance = Config.getInstance();

                // Driver Connection Check
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    ErrorLog.add(Config.getInstance(), "Failed to find the PostgreSQL JDBC driver.", e);
                }
            }
        }

        return getConnection(configInstance, false, null);
    }

    /**
     * Creates a valid Connection object if a connection could be established with the PostgreSQL database using information from config.xml.
     * @param myConfigInstance  - Config reference. Setting this to null causes it to lookup the default config instance.
     * @return valid Connection object if successfully connected, otherwise null
     */
    public static DatabaseConnection getConnection(Config myConfigInstance)
    {
        if(myConfigInstance == null)
        {
            myConfigInstance = Config.getInstance();
        }

        return getConnection(myConfigInstance, false, null);
    }

    /**
     * Creates a valid Connection object if a connection could be established with the PostgreSQL database using information from config.xml.
     * @param myConfigInstance  - Config reference. Setting this to null causes it to lookup the default config instance.
     * @param failFast  - TRUE if this method is to return after failing on the first attempt with a null, FALSE if it is to loop continuously or attempt connecting the number of times specified if specified.
     * Each connection establish retry is done after putting the thread to sleep for 2 seconds.
     *
     * @return valid Connection object if successfully connected, otherwise null
     */
    public static DatabaseConnection getConnection(Config myConfigInstance, boolean failFast)
    {
        if(myConfigInstance == null)
        {
            myConfigInstance = Config.getInstance();
        }

        return getConnection(myConfigInstance, failFast, null);
    }

    /**
     * Creates a valid Connection object if a connection could be established with the PostgreSQL database using information from config.xml.
     * @param myConfigInstance
     * @param failFast  - TRUE if this method is to return after failing on the first attempt with a null, FALSE if it is to loop continuously or attempt connecting the number of times specified if specified.
     * Each connection establish retry is done after putting the thread to sleep for 2 seconds.
     * @param maxAttemptCount  - The maximum number of times to attempt connecting. Leave this blank or null if infinite is desired.
     *
     * @return valid Connection object if successfully connected, otherwise null
     */
    public static DatabaseConnection getConnection(Config myConfigInstance, boolean failFast, Integer maxAttemptCount)
    {
        DatabaseConnection con = null;

        if(myConfigInstance == null) {
            myConfigInstance = Config.getInstance();

            if(myConfigInstance == null) {
                return null;
            }
        }

        if(configInstance == null)
        {
            // Driver Connection Check
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                ErrorLog.add(Config.getInstance(), "Failed to find the PostgreSQL JDBC driver.", e);
            }
        }

        synchronized(connectionIDsLock) {
            if(connectionIDs == null) {
                connectionIDs = new BitSet(myConfigInstance.getMaxNumOfConnectionsPerInstance());
            }
        }

        synchronized(DatabaseConnectorLock) {
            if(connectionPool == null) {
                connectionPool = new C3P0ConnectionPool(myConfigInstance);
            }
        }

        con = connectionPool.getConnection();
        return con;
    }

    /**
     * Closes all currently opened connections and resets connection count.
     * @throws SQLException
     * @throws Exception
     */
    public static void closeAllConnections() throws SQLException, Exception{
        int connectionSize, count;

        synchronized(DatabaseConnectorLock)
        {
            ArrayList<Integer> activeIDs = new ArrayList<Integer>();
            for(Integer id : connections.keySet())
            {
                activeIDs.add(id);
            }
            for(Integer id : activeIDs)
            {
                if(!connections.get(id).isClosed()) {
                    connections.get(id).close();
                }
            }

            connectionSize = connections.size();
            count = connectionCount;
        }

        if(connectionSize != 0)
        {
            throw new Exception("Failed to update connections listing when closing all connections. Size of listing = " + connections.size() + ".");
        }

        if(count != 0)
        {
            throw new Exception("Failed to correctly update connection count when closing all connections. Connection count = " + connectionCount + ".");
        }
    }
}
