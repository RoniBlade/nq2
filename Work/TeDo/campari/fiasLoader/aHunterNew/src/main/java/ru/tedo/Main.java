package ru.tedo;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Model.AddressEntity;
import ru.tedo.Repository.DbConnection;
import ru.tedo.Repository.DwhRepository;
import ru.tedo.Service.ProcessAddress;

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

//        BasicConfigurator.configure();
        String fileConfig = getApplicationPath() + File.separator + "log4j.properties";
        //System.out.println("Config file: " + fileConfig);
        PropertyConfigurator.configure(fileConfig);

        logger.info("Start application");
        int httpError = 0;
        try {
            loadProperties();
            minQuality = Integer.valueOf(getProperty("aHunter.minQuality"));
            ProcessAddress pa = new ProcessAddress();
            // pa.processAddressStr("Москва, Большая Полянка 13/15");
           // pa.processAddressString(0,"Раменский район, пос.Удельная, Карпинского 13");
            openDBConnection();
            DwhRepository dr = new DwhRepository();
            List<AddressEntity> addrList = dr.getAddressList();
            logger.info("Start process " + addrList.size() + " addresses");

            int badQuality = 0;
            for (AddressEntity addr : addrList) {
                pa.processAddressString(addr);
                if (addr.getQuality() < minQuality && addr.getStatusID() == 2) {
                    addr.setStatusID(4);
                    badQuality++;
                }
                if (addr.getStatusID() == -1)
                    httpError ++;
            }
            dr.saveAddressList(addrList);
            logger.info("Address info saved for " + (addrList.size() - httpError) + " addresses. "+
                        "Bad quality addresses " + badQuality + ". Errors " + httpError);
        } catch (Exception e) {
            logger.error("Error in application : " + e.getMessage());
            System.exit(2);
        }
        logger.info("Finish address processing");
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
     * Upload properties from file ahunter.properties
     * This file sould be in the same directory as JAR file
     * @throws IOException
     */
    private static void loadProperties() throws IOException {
        String jarDir = getApplicationPath();
        logger.info("Path = " + jarDir);
        String propFile = jarDir + File.separator + "ahunter.properties";
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