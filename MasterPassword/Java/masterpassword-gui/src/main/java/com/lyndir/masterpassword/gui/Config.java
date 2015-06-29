package com.lyndir.masterpassword.gui;

/**
 * @author lhunath, 2014-08-31
 */
public class Config {

    private static final Config instance = new Config();

    public static Config get() {
        return instance;
    }

    public boolean checkForUpdates() {
        return Boolean.getBoolean("mp.update.check");
    }
}
