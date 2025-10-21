package ru.tedo;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Model.RegionEntity;
import ru.tedo.Repository.DbConnection;
import ru.tedo.Repository.DwhRepository;
import ru.tedo.Service.ProcessRegion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class Main {
    final static Logger logger = LoggerFactory.getLogger(Main.class);
    private static Properties prop = null;
    private static int minQuality = 0;

    public static String getProperty(String propertyName) {
        String result = "";
        result = prop.getProperty(propertyName);
        return result;
    }

    public static void main(String[] args) {

        String fileConfig = getApplicationPath() + File.separator + "log4j.properties";
        System.out.println("Config file: " + fileConfig);
        PropertyConfigurator.configure(fileConfig);

        logger.info("Start application");
        int httpError = 0;
        try {
            loadProperties();
            minQuality = Integer.valueOf(getProperty("fias.minQuality"));
            ProcessRegion pa = new ProcessRegion();
            openDBConnection();
            DwhRepository dr = new DwhRepository();
            List<RegionEntity> regionList = pa.getRegionList();
            logger.info("Start process " + regionList.size() + " Regions");

            dr.saveRegionList(regionList);
            logger.info("Region info saved for " + (regionList.size() - httpError) + " Regions. " +
                         ". Errors " + httpError);
        } catch (Exception e) {
            logger.error("Error in application : " + e.getMessage());
            System.exit(2);
        }
        logger.info("Finish Region processing");
       // System.out.println("Finish");
        if ( httpError == 0) {
            logger.info("System ends with code 0");
            System.exit(0);
        }
        else {
            logger.info("System ends with code 1");
            System.exit(1);
        }
    }

    private static void openDBConnection() {
        DbConnection conn = new DbConnection(
                getProperty("postgresql.DBaddress"),
                Integer.valueOf(getProperty("postgresql.Port")),
                getProperty("postgresql.DBname"),
                getProperty("postgresql.Login"),
                getProperty("postgresql.Password"),
                getProperty("postgresql.ssh.Login"),
                getProperty("postgresql.ssh.Password"),
                Integer.valueOf(getProperty("postgresql.ssh.ForwardPort")),
                Boolean.valueOf(getProperty("postgresql.ssh.UseSSH")),
                Integer.valueOf(getProperty("postgresql.maxConnection")));
    }

    /**
     * Upload properties from file fiasLoader.properties
     * This file sould be in the same directory as JAR file
     * @throws IOException
     */
    private static void loadProperties() throws IOException {
        String jarDir = getApplicationPath();
        logger.info("Path = " + jarDir);
        String propFile = jarDir + File.separator + "fiasLoader.properties";
        Properties appProps = new Properties();
        appProps.load(new FileInputStream(propFile));
        Enumeration keys = appProps.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String val = appProps.getProperty(key);
            logger.info(key + " : " + val);
        }
        prop = appProps;
    }

    /**
     * Get path of JAR file.
     * @return path of JAR file
     */
    private static String getApplicationPath() {
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(path);
        String jarDir = jarFile.getParentFile().getAbsolutePath();
        return jarDir;
    }

}