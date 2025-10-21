    package ru.tedo.Repository;

    import com.jcraft.jsch.JSch;
    import com.jcraft.jsch.Session;
    import com.zaxxer.hikari.HikariConfig;
    import com.zaxxer.hikari.HikariDataSource;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.sql.Connection;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Statement;
    import java.util.ArrayList;
    import java.util.List;

    public class PostgreSQLConnection {

        final static Logger logger = LoggerFactory.getLogger(PostgreSQLConnection.class);
        /*
        private static String dbHost = "";

        private static Integer dbPort = 0;
        private static String database = "";
        private static String dbLogin = "";
        private static String dbPassword = "";
        private static String sshLogin = "";
        private static String sshPassword = "";
        private static Integer sshForwardPort = 0;
        private static Boolean isSSH = false;
        private static Integer maxConnections = 0;

        private static Boolean dbLock = false;
        private static Session sshSession = null;
        private static HikariDataSource dataSource = null;
        private static List<Connection> connectionList = new ArrayList<>();

    */

    /*
        public PostgreSQLConnection() {

        }
    */

        protected void  OpenSQLConnection(DbConnectionParams params) {

            String dbConnection = "jdbc:postgresql://";

            logger.info("Try to close existing connection");
            CloseSQLConnection(params);
            logger.info("Create new connection");



            try {

                logger.info("Database connection SSH=" + params.isSSH + " Max connections = " + params.maxConnections);

                synchronized (params.dbLock) {
                    if (params.isSSH) {
                        JSch jsch = new JSch();
                        params.sshSession = jsch.getSession(params.sshLogin, params.dbHost, 22);
                        params.sshSession.setPassword(params.sshPassword);
                        params.sshSession.setConfig("StrictHostKeyChecking", "no");
                        params.sshSession.connect();
                        params.sshSession.setPortForwardingL(params.sshForwardPort, params.dbHost, params.dbPort);


                        logger.info("SSH tunnel success.");
                    }


                    if (params.isSSH)
                        dbConnection = dbConnection + "localhost:" + params.sshForwardPort + "/" + params.database;
                    else
                        dbConnection = dbConnection + params.dbHost + ":" + params.dbPort + "/" + params.database;
                    logger.info("Try to open new DataSource to database " + dbConnection);
                    Class.forName("org.postgresql.Driver");
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl(dbConnection);
                    cfg.setUsername(params.dbLogin);
                    cfg.setPassword(params.dbPassword);
                    cfg.setMaximumPoolSize(params.maxConnections);
                    //cfg.setConnectionTimeout(3600000);
                    // cfg.setAutoCommit(false);
                    // cfg.addDataSourceProperty("cachePrepStmts", "true");
                    // cfg.addDataSourceProperty("prepStmtCacheSize", "250");
                    // cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    params.dataSource = new HikariDataSource(cfg);
                    logger.info("Try to open pool of connection to database " + dbConnection);
                    params.connectionList = new ArrayList<>();
                    for (int i = 0; i < params.maxConnections; i++) {
                        Connection cc = params.dataSource.getConnection();
                        params.connectionList.add(cc);
                    }
                    logger.info("Success connect to database " + dbConnection);
                }

            } catch (Exception e) {
                logger.error("Database connection ERROR " + e.getMessage());
                logger.error("Database connection " + dbConnection);
                CloseSQLConnection(params);
            }
        }

        private Connection getSQLConnection(DbConnectionParams connect,Integer threadNum) {

            logger.info("getSQLConnection: get connection from app settings");
            // DatabaseConnectionEntity connection = null;
            Connection result = null;


            synchronized (connect.dbLock)
            {
                logger.info("getSQLConnection: dbLock setted");
                if (connect.connectionList == null || connect.dataSource == null)
                    OpenSQLConnection(connect);

                if (connect.dataSource.isClosed())
                    OpenSQLConnection(connect);


                if (connect.connectionList == null) {
                    logger.error("Error open new DataSource and creation connectionLIst");
                    return null;
                }
                logger.info("getSQLConnection: get connection from DataSource");
                result = connect.connectionList.get(threadNum);
                if (result == null) {
                    logger.error("Error get connection with thread number " + threadNum);
                    return null;
                }
                logger.info("getSQLConnection: test 'IS VALID ' connection with thread number " + threadNum);


                try {
                    if (!result.isValid(5)) {
                        // Если что-то протухло, то протухли скорее всего все коннекты.
                        // Ну не протухают они по одному.... Поэтому просто закрываем и зановоо открываем...
                        logger.error("getSQLConnection: connection is not valid. RECONNECT! " + threadNum);
                        CloseSQLConnection(connect);
                        logger.error("getSQLConnection: try to open new connections. " + threadNum);
                        OpenSQLConnection(connect);
                        result = connect.dataSource.getConnection();
                    }
                } catch (Exception e) {
                    logger.error("Establish new SQL connection ERROR: " + e.getMessage());
                    return null;
                }
            }
            logger.info("getSQLConnection: dbLock UN-setted");

            return result;
        }

    /*
        public ResultSet executeSqlQuery(DbConnectionParams connect,String queryStr) throws SQLException {
            return executeSqlQuery(connect,queryStr,0);
        }
    */

        protected ResultSet executeSqlQuery(DbConnectionParams connect, String queryStr, Integer connectionNum) throws SQLException {
            logger.info("Try execute SQL query... by connection = "+ connectionNum);
            ResultSet result = null;
            int attempt = 0;
            do {
                synchronized (connect.dbLock) {
                    logger.info("DB locked...");
                    logger.info("Execute query getting connection...");
                    Connection connection = getSQLConnection(connect, connectionNum);
                    if (connection == null) {
                        logger.error("DB connection not defined...");
                        result = null;
                    }

                    logger.info("Execute query creating statement...");
                    logger.info("Exceute query " + queryStr);
                    Statement stmt = null;
                    if (connection != null) {
                        stmt = connection.createStatement();
                        stmt.setQueryTimeout(180);
                    }
                    try {
                        long start = System.currentTimeMillis();
                        if (stmt != null)
                            result = stmt.executeQuery(queryStr);
                        long end = System.currentTimeMillis();
                        long s = end - start;
                        if (s > 1000) {
                            logger.error("Too slow SQL with duration " +
                                    s + "\n" + queryStr);
                        }

                    } catch (SQLException e) {
                        logger.error("Exceute query ERROR " + e.getMessage());
                        logger.error("sql string:" + queryStr);

                    }
                }
                attempt++;
            } while(attempt < 5 && result == null);
            logger.info("DB UNlocked...");
            logger.info("Executed SQL query...");
            return result;
        }

        /*
            public  Boolean  executeSql(DbConnectionParams connect, String queryStr) throws SQLException {

                return executeSql(connect, queryStr, 0);
            }
        */
        protected Boolean  executeSql(DbConnectionParams connect, String queryStr, Integer connectionNum) throws SQLException {

            logger.info("Try execute SQL statement by connection = " + connectionNum );
            Boolean result = false;

            synchronized(connect.dbLock)
            {
                logger.info("Exceute sql " + queryStr);
                Connection  connection = getSQLConnection(connect,connectionNum);
                if (connection == null) {
                    logger.error("DB connection not defined...");
                    return false;
                }

                Statement stmt = connection.createStatement();
                try {
                    stmt.executeUpdate(queryStr + " ; COMMIT;");
                    result = true;
                } catch (SQLException e) {
                    logger.error("Execute query ERROR " + e.getMessage());
                    logger.error("sql string:" + queryStr);
                }
                stmt.close();
            }
            logger.info("DB UNlocked...");
            logger.info("Executed SQL...");
            return result;
        }

        protected void CloseSQLConnection(DbConnectionParams connect){
            // synchronized(dbLock)
            {
                if (connect.dataSource != null) {
                    try {
                        connect.dataSource.close();
                        connect.dataSource = null;
                        connect.connectionList = new ArrayList<>();
                    } catch (Exception e) {
                        logger.error("ERROR close database connection " + e.getMessage());
                        connect.dataSource = null;
                        connect.connectionList = new ArrayList<>();
                    }
                }
                if (connect.sshSession != null) {
                    try {
                        connect.sshSession.disconnect();
                        connect.sshSession = null;
                    } catch (Exception e) {
                        logger.error("ERROR close ssh tunnel " + e.getMessage());
                        connect.sshSession = null;
                    }
                }
            }
        }

        public static class DbConnectionParams {
            public  String dbHost = "";

            public  Integer dbPort = 0;
            public  String database = "";
            public  String dbLogin = "";
            public  String dbPassword = "";
            public  String sshLogin = "";
            public  String sshPassword = "";
            public  Integer sshForwardPort = 0;
            public  Boolean isSSH = false;
            public  Integer maxConnections = 0;

            public  Boolean dbLock = false;
            public Session sshSession = null;
            public  HikariDataSource dataSource = null;
            public List<Connection> connectionList = new ArrayList<>();

        }
    /* Это просто как пример записи в файл... на случай просто скопировать код..

        private void SaveResultToFile(String exMessage){

            try {
                Charset charset = Charset.forName("Windows-1251");
                String userDirectory = new File("").getAbsolutePath();
                Files.deleteIfExists(Path.of(userDirectory +"loging.txt"));
                BufferedWriter writer = Files.newBufferedWriter(Path.of(userDirectory +"/loging.txt"), charset, StandardOpenOption.CREATE);
                writer.write(exMessage);
                writer.close();
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }

        }
    */
    }
