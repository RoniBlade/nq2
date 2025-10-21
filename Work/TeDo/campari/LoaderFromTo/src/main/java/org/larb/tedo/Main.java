package org.larb.tedo;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Repository.DbConnection;
import ru.tedo.Service.UploadCsvFileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
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

//        BasicConfigurator.configure();
        String fileConfig = getApplicationPath() + File.separator + "log4j.properties";
        //System.out.println("Config file: " + fileConfig);
        PropertyConfigurator.configure(fileConfig);
        Boolean isCopy = false;
        logger.info("Start application");
        try {
            loadProperties();
            openDBConnection();
            isCopy = Boolean.valueOf(getProperty("copy.copyAfterProcess"));
            UploadCsvFileService exl = new UploadCsvFileService();

            if (Files.notExists(Path.of(getProperty("csv.fileName")))) {
                logger.info("File " + getProperty("csv.fileName") + " not exists.");
                System.exit(0);
            }
            exl.uploadCsvFile();

            if (isCopy) {
                logger.info("Copy file to processed directory");
                File file = new File(getProperty("csv.fileName"));
                moveFile(file,getProperty("copy.ProcessedDir"));
            }

        } catch (Exception e) {
            logger.error("Error in application : " + e.getMessage());
            if (isCopy) {
                logger.info("Copy file to error directory");
                File file = new File(getProperty("csv.fileName"));
                moveFile(file,getProperty("copy.ErrorDir"));
            }
            System.exit(2);
        }


        logger.info("Finish address processing");
       // System.out.println("Finish");
        System.exit(0);
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
     * Upload properties from file excelLoader.properties
     * This file sould be in the same directory as JAR file
     * @throws IOException
     */

    protected static void moveFile(File file, String targetDir) {
        try {
            Files.move(file.toPath(), Path.of(targetDir + file.getName().trim()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ee) {
            logger.error("Error moving file " + file.getName() + " to error directory " + ee.getMessage());
        }
    }
    private static void loadProperties() throws IOException {
        String jarDir = getApplicationPath();
        logger.info("Path = " + jarDir);
        String propFile = jarDir + File.separator + "csvLoader.properties";
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

    /*
    Работа идет по токену. Токен есть в личном кабинете.
    Т.е. надо войти в личный кабинет по логину/паролю и взять там токен

    Логин: Campari
Пароль: NJC0z4ALES(il(__
Токен: 	Campari4GzhLxb6l087QaSvOeH6E6

https://ahunter.ru/
     */

}