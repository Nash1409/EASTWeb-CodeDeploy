/**
 *
 */
package version2.prototype.util;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import version2.prototype.Config;
import version2.prototype.ErrorLog;

/**
 * @author michael.devos
 *
 * http://www.mchange.com/projects/c3p0/
 *
 */
public class C3P0ConnectionPool extends DatabaseConnectionPoolA {
    private final ComboPooledDataSource comboPDS;
    private HashMap<Integer, String> connectionDescriptions;
    private HashMap<Integer, String> pastConnectionDescriptions;
    private HashMap<Integer, LocalDateTime> connectionPullTimes;

    /**
     * @param configInstance
     */
    public C3P0ConnectionPool(Config configInstance) {
        super(configInstance);

        if(configInstance.getMaxNumOfConnectionsPerInstance() <= 1) {
            ErrorLog.add(configInstance, "Maximum number of allowed database connections must exceed 1. Please increase this value in config.xml.",
                    new Exception("Maximum number of allowed database connections must exceed 1."));
        }

        // Setup connection pool
        comboPDS = new ComboPooledDataSource();     // C3P0 Connection Pool object
        try {
            comboPDS.setDriverClass("org.postgresql.Driver");
        } catch (PropertyVetoException e) {
            ErrorLog.add(Config.getInstance(), "Failed to find the PostgreSQL JDBC driver.", e);
        }

        comboPDS.setJdbcUrl(configInstance.getDatabaseHost() + ":" + configInstance.getPort() + "/" + configInstance.getDatabaseName());
        comboPDS.setUser(configInstance.getDatabaseUsername());
        comboPDS.setPassword(configInstance.getDatabasePassword());
        comboPDS.setMaxPoolSize(configInstance.getMaxNumOfConnectionsPerInstance());

        connectionDescriptions = new HashMap<Integer, String>();
        pastConnectionDescriptions = new HashMap<Integer, String>();
        connectionPullTimes = new HashMap<Integer, LocalDateTime>();
    }

    /* (non-Javadoc)
     * @see version2.prototype.util.DatabaseConnectionPoolA#close()
     */
    @Override
    public void handleClosing() {
        synchronized(super.DatabaseConnectorLock) {
            comboPDS.close();
        }
    }

    /* (non-Javadoc)
     * @see version2.prototype.util.DatabaseConnectionPoolA#getConnection(boolean, java.lang.Integer)
     */
    @SuppressWarnings("resource")
    @Override
    public DatabaseConnection handleGettingConnection() {
        DatabaseConnection dbCon = null;
        Integer connectionID = null;
        StackTraceElement tempTrace = new Exception().getStackTrace()[1];
        String descriptor = "Thread(" + Thread.currentThread().getId() + ") - " + tempTrace.getClassName().substring(tempTrace.getClassName().lastIndexOf(".") + 1)
                + "(" + tempTrace.getLineNumber() + ")";

        synchronized(super.DatabaseConnectorLock) {
            connectionID = getLowestAvailableConnectionID();
            Connection con = null;
            if(connectionID > -1)
            {
                try {
                    //                    System.out.println("\nBefore getting connection: " + descriptor +
                    //                            "\n  connectionID: " + connectionID +
                    //                            "\n  num_connections: " + comboPDS.getNumConnections() +
                    //                            "\n  num_busy_connections: " + comboPDS.getNumBusyConnections() +
                    //                            "\n  num_idle_connections: " + comboPDS.getNumIdleConnections());

                    con = comboPDS.getConnection();

                    pastConnectionDescriptions.put(connectionID, connectionDescriptions.get(connectionID));
                    connectionDescriptions.put(connectionID, descriptor);
                    connectionPullTimes.put(connectionID, LocalDateTime.now());

                    //                    System.out.println("\nAfter getting connection: " + descriptor +
                    //                            "\n  connectionID: " + connectionID +
                    //                            "\n  num_connections: " + comboPDS.getNumConnections() +
                    //                            "\n  num_busy_connections: " + comboPDS.getNumBusyConnections() +
                    //                            "\n  num_idle_connections: " + comboPDS.getNumIdleConnections());
                } catch (SQLException e) {
                    if(!Thread.currentThread().isInterrupted())
                    {
                        String message = "Problem getting new connection from connection pool." +
                                "\n  connectionID: " + connectionID +
                                "\n  connectionDescriptions: " + connectionDescriptions +
                                "\n  connectionPullTimes: " + connectionPullTimes +
                                "\n  pastConnectionDescriptions: " + pastConnectionDescriptions;
                        ErrorLog.add(configInstance, message, e);
                    } else {
                        con = null;
                    }
                }

                if(con != null) {
                    dbCon = new DatabaseConnection(configInstance, this, con, connectionID);
                } else {
                    if(connectionID > -1) {
                        releaseConnectionID(connectionID);
                    }
                }
            }
            //            }
        }

        return dbCon;
    }

    /* (non-Javadoc)
     * @see version2.prototype.util.DatabaseConnectionPoolA#getConnectionCount()
     */
    @Override
    public int getConnectionCount() {
        int count = -1;

        synchronized(super.DatabaseConnectorLock) {
            try {
                count = comboPDS.getNumConnections();
            } catch(SQLException e) {
                ErrorLog.add(configInstance, "Problem while checking actve database connection count.", e);
            }
        }

        return count;
    }

    /* (non-Javadoc)
     * @see version2.prototype.util.DatabaseConnectionPoolA#endConnection(java.lang.Integer)
     */
    @Override
    public void endConnection(Integer ID) {
        synchronized(super.DatabaseConnectorLock) {
            releaseConnectionID(ID);
        }
    }

}
