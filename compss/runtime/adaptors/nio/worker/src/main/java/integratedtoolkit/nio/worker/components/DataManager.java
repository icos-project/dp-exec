package integratedtoolkit.nio.worker.components;

import integratedtoolkit.log.Loggers;
import integratedtoolkit.nio.worker.exceptions.InitializationException;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DataManager {

    // Logger
    private static final Logger wLogger = LogManager.getLogger(Loggers.WORKER);
    // Cache
    private final HashMap<String, Object> objectCache;


    public DataManager() {
        objectCache = new HashMap<String, Object>();
    }

    public void init() throws InitializationException {
        // All structures are already defined
    }

    public void stop() {

    }

    /*
     * ************************************ 
     * STORE METHODS
     ************************************/
    public synchronized void storeObject(String name, Object value) {
        try {
            objectCache.put(name, value);
        } catch (NullPointerException e) {
            wLogger.error("Object Cache " + objectCache + " dataId " + name + " object " + value);
        }
    }

    /*
     * ************************************ 
     * GET METHODS
     ************************************/
    public synchronized Object getObject(String name) {
        return objectCache.get(name);
    }

    /*
     * ************************************ 
     * REMOVE METHODS
     ************************************/
    public synchronized void remove(String name) {
        objectCache.remove(name);
    }

    /*
     * ************************************ 
     * CHECKER METHODS
     ************************************/
    public synchronized boolean checkPresence(String name) {
        return objectCache.containsKey(name);
    }

}
