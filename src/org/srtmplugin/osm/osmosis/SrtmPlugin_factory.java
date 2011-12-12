package org.srtmplugin.osm.osmosis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class SrtmPlugin_factory extends TaskManagerFactory {

    //local directory for saving the downloaded *.hgt files
    private static final String ARG_LOCAL_DIR = "locDir";
    //default: tempdir
    private static final String DEFAULT_LOCAL_DIR = System.getProperty("java.io.tmpdir");
    
    //url for server basedir
    private static final String ARG_SERVER_BASE = "srvBase";
    //default: defined at srtmservers.properties
    
    private static String DEFAULT_SERVER_BASE = "";
    //subdirs for differenty countries, separated by semicolon
    private static final String ARG_SERVER_SUB_DIRS = "srvSubDirs";
    //default: defined at srtmservers.properties
    private static String DEFAULT_SERVER_SUB_DIRS = "";
    
    //local use of plugin (only available hgt files inside local directory)
    private static final String ARG_LOCAL_ONLY = "locOnly";
    //default: false
    private static final boolean DEFAULT_LOCAL_ONLY = false;
    
    //replace existing "height" tags of nodes
    private static final String ARG_REPLACE_EXISTING = "repExisting";
    //default: true
    private static final boolean DEFAULT_REPLACE_EXISTING = true;

    /**
     * method for reading the serverBaseDir and 
     * serverSubDirectories from the srtmservers.properties files
     * inside the jar
     */
    public void readSrvProperties() {
        Properties properties = new Properties();
        try (InputStream is = this.getClass().getResourceAsStream("/srtmservers.properties")) {
            if (is != null) {
                properties.load(is);
                is.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        DEFAULT_SERVER_BASE = checkForTrailingSlash(properties.getProperty("srtm.src.url.base"));

        for (Object o : properties.keySet()) {
            String s = (String) o;
            if (s.startsWith("srtm.src.url.subdir.")) {
                DEFAULT_SERVER_SUB_DIRS += checkForTrailingSlash(properties.getProperty(s)) + ";";
            }
        }
    }
    
    public List<String> getDefault_Server_Sub_Dirs() {
        String[] sss_split = DEFAULT_SERVER_SUB_DIRS.split(";");
        List<String> al_sss = new ArrayList<>();
        al_sss.addAll(Arrays.asList(sss_split));
        return al_sss;
    }
    
    public String getDefault_Server_Base() {
        return DEFAULT_SERVER_BASE;
    }
    
    
    /**
     * checks a string for an trailing slash
     * @param s input string w/o trailing slash
     * @return string s with added trailing slash
     */
    private String checkForTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s;
        } else {
            return s += "/";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        readSrvProperties();

        String localDir = getStringArgument(taskConfig, ARG_LOCAL_DIR, DEFAULT_LOCAL_DIR);
        String serverBase = getStringArgument(taskConfig, ARG_SERVER_BASE, DEFAULT_SERVER_BASE);
        String serverSubDirs = getStringArgument(taskConfig, ARG_SERVER_SUB_DIRS, DEFAULT_SERVER_SUB_DIRS);
        boolean localOnly = getBooleanArgument(taskConfig, ARG_LOCAL_ONLY, DEFAULT_LOCAL_ONLY);
        boolean repExist = getBooleanArgument(taskConfig, ARG_REPLACE_EXISTING, DEFAULT_REPLACE_EXISTING);

        boolean tmpDir = false;
        if (localDir.equals(System.getProperty("java.io.tmpdir"))) {
            tmpDir = true;
        }

        File lDir = new File(localDir);

        String[] sss_split = serverSubDirs.split(";");
        List<String> al_sss = new ArrayList<>();
        al_sss.addAll(Arrays.asList(sss_split));


        SinkSource task = new SrtmPlugin_task(
                serverBase,
                al_sss,
                lDir,
                tmpDir,
                localOnly,
                repExist);

        return new SinkSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}
