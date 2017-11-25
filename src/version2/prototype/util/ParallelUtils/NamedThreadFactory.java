package version2.prototype.util.ParallelUtils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import version2.prototype.Config;
import version2.prototype.ErrorLog;

/**
 * Copyright (c) 2011.  Peter Lawrey
 *
 * "THE BEER-WARE LICENSE" (Revision 128)
 * As long as you retain this notice you can do whatever you want with this stuff.
 * If we meet some day, and you think this stuff is worth it, you can buy me a beer in return
 * There is no warranty.
 *
 * ------------------
 *
 * Modifications have been made from the source.
 *
 */
public class NamedThreadFactory implements ThreadFactory {
    private final String name;
    private final boolean daemon;
    private final Config configInstance;

    /**
     * Sets up defaults for a new NamedThreadFactory.
     *
     * @param configInstance  - Config instance to use for error logging
     * @param name  - default name to use for threads
     * @param daemon  - set to true if threads are to be made as daemons
     */
    public NamedThreadFactory(Config configInstance, String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
        this.configInstance = configInstance;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, name);
        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                ErrorLog.add(configInstance, "Uncaught Exception", e);
            }
        });
        t.setDaemon(daemon);
        return t;
    }
}
