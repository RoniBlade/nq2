package ru.tedo.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbConnection extends PostgreSQLConnection {

    public static DbConnectionParams connParams = new DbConnectionParams();

    public DbConnection() {

    }


    public DbConnection(String dbHost, Integer dbPort, String database, String dbLogin, String dbPassword, String sshLogin, String sshPassword, Integer sshForwardPort, Boolean isSSH, Integer maxConnections) {
        connParams.dbHost = dbHost;
        connParams.dbPort = dbPort;
        connParams.database = database;
        connParams.dbLogin = dbLogin;
        connParams.dbPassword = dbPassword;
        connParams.sshLogin = sshLogin;
        connParams.sshPassword = sshPassword;
        connParams.sshForwardPort = sshForwardPort;
        connParams.isSSH = isSSH;
        connParams.maxConnections = maxConnections;

    }


    public Boolean executeSql(String queryStr) throws SQLException {
        return executeSql(queryStr,0);
    }


    public Boolean executeSql(String queryStr, Integer connectionNum) throws SQLException {
        return super.executeSql(connParams, queryStr, connectionNum);
    }


    public ResultSet executeSqlQuery(String queryStr) throws SQLException {
        return executeSqlQuery(queryStr, 0);
    }


    public ResultSet executeSqlQuery(String queryStr, Integer connectionNum) throws SQLException {
        return super.executeSqlQuery(connParams, queryStr, connectionNum);
    }



}

