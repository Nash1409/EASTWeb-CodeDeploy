/**
 *
 */
package version2.prototype.util;

import java.util.BitSet;

import version2.prototype.Config;

/**
 * @author michael.devos
 *
 */
public abstract class DatabaseConnectionPoolA {
    protected final Config configInstance;
    protected Boolean DatabaseConnectorLock;  // Value doesn't matter.
    protected BitSet connectionIDs;
    protected Boolean hasBeenClosed;

    protected DatabaseConnectionPoolA(Config configInstance)
    {
        if(configInstance == null)
        {
            configInstance = Config.getInstance();
        }
        this.configInstance = configInstance;
        DatabaseConnectorLock = new Boolean(true);
        connectionIDs = new BitSet(configInstance.getMaxNumOfConnectionsPerInstance());
        hasBeenClosed = new Boolean(false);
    }

    /**
     * Safely closes the connection pool and managed resources if it has been instantiated.
     */
    public final void close()
    {
        handleClosing();
        synchronized(hasBeenClosed) {
            hasBeenClosed = true;
        }
    }

    /**
     * Creates a valid Connection object if a connection could be established with the PostgreSQL database using information from config.xml.
     * @return If not already closed, then a valid DatabaseConnection object if successfully connected, otherwise null
     */
    public final DatabaseConnection getConnection()
    {
        synchronized(hasBeenClosed) {
            if(!hasBeenClosed)
            {
                return handleGettingConnection();
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the current number of active database connections.
     * @return  int - number of active database connections.
     */
    public abstract int getConnectionCount();

    /**
     * Ends the connection and updates the state of the DatabaseConnector.
     * @param ID  - DatabaseConnection ID
     */
    public abstract void endConnection(Integer ID);

    /**
     * Called from close().
     */
    protected abstract void handleClosing();

    /**
     * Called from getConnection().
     * @return valid Connection object if successfully connected, otherwise null
     */
    protected abstract DatabaseConnection handleGettingConnection();

    protected int getLowestAvailableConnectionID()
    {
        int id = -1;

        synchronized(DatabaseConnectorLock)
        {
            id = connectionIDs.nextClearBit(0);

            if(IsIDValid(id, connectionIDs)) {
                connectionIDs.set(id);
            }
            else {
                id = -1;
            }
        }

        return id;
    }

    protected void releaseConnectionID(Integer id)
    {
        if(IsIDValid(id, connectionIDs)) {
            connectionIDs.clear(id);
        }
    }

    protected boolean IsIDValid(Integer id, BitSet set)
    {
        if((id != null) && (id >= 0) && (id < set.size())) {
            return true;
        } else {
            return false;
        }
    }
}
