package ru.tedo.Repository;

import com.jcraft.jsch.JSchException;
import org.apache.poi.hpsf.Decimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Main;
import ru.tedo.Model.TableStructureEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DwhRepository {
    final static Logger logger = LoggerFactory.getLogger(DwhRepository.class);
    private int chankSize = 100;

    public void truncateTable() throws SQLException {
        String tableName = Main.getProperty("csv.uploadTable");
        String scheme = Main.getProperty("csv.uploadScheme");
        String sql = "truncate table " + scheme + "." + tableName;
        DbConnection pg = new DbConnection();
        pg.executeSql(sql);
    }

    public List<TableStructureEntity> getTableStructureList(String scheme, String tableName) throws JSchException, SQLException, ClassNotFoundException {
        logger.info("Getting table structure " + scheme + "." +  tableName);
        List<TableStructureEntity> result = new ArrayList<>();
        DbConnection pg = new DbConnection();
        String sql = "SELECT\n" +
                "    column_name,\n" +
                "    udt_name,\n" +
                "    character_maximum_length,\n" +
                "    numeric_precision,\n" +
                "    numeric_precision_radix,\n" +
                "    numeric_scale ,\n" +
                "    is_nullable \n" +
                "FROM\n" +
                "    information_schema.columns\n" +
                "where\n" +
                "    table_schema = '"+ scheme.trim() +"' and\n" +
                "    table_name = '"+tableName.trim()+"';";
        ResultSet rs = pg.executeSqlQuery(sql);
        while(rs.next()) {
            TableStructureEntity tbl = new TableStructureEntity();
            tbl.setColumnName(rs.getString("column_name"));
            tbl.setColumnType(rs.getString("udt_name"));
            tbl.setCharMaxLength(rs.getInt("character_maximum_length"));
            tbl.setNumericPrecision(rs.getInt("numeric_precision"));
            tbl.setNumericRadix(rs.getInt("numeric_precision_radix"));
            tbl.setNumericScale(rs.getInt("numeric_scale"));
            if (rs.getString("is_nullable").trim().equals("NO"))
                tbl.setIsNullable(false);
            else
                tbl.setIsNullable(true);
            result.add(tbl);
        }
        rs.getStatement().close();
        rs.close();
        return result;
    }

    public void saveRows(List<Map<String, String>> rowsList, Integer connectionNum) throws Exception {

        String tableName = Main.getProperty("csv.uploadTable");
        String scheme = Main.getProperty("csv.uploadScheme");
        StringBuilder sb = new StringBuilder();
        List<TableStructureEntity> ts = getTableStructureList(scheme,tableName);
        DbConnection pg = new DbConnection();
        int count = 0;

        for (Map<String,String> row : rowsList) {
            String fields = "insert into " + scheme + "." + tableName + "(";
            String values = "values (";
            for (Map.Entry<String,String> entry : row.entrySet()) {
                fields += entry.getKey() + ",";
                values += prepareFieldValue(entry.getKey().replaceAll("\"",""),entry.getValue(),ts);
            }
            fields = fields.substring(0,fields.length()-1) + ")";
            values = values.substring(0,values.length()-1) + ") on conflict do nothing;\n";
            sb.append(fields);
            sb.append(values);

            count++;
            if (count > chankSize) {
                String sql = sb.toString();
                pg.executeSql(sql, connectionNum);
                sb = new StringBuilder();
                count = 0;
            }
        }
        String sql = sb.toString();
        if (sql.length() > 0)
           pg.executeSql(sql, connectionNum);

    }

    private String prepareFieldValue(String field, String value, List<TableStructureEntity> ts) throws Exception {
        String result = "";

        TableStructureEntity ent = ts.stream().filter(f-> f.getColumnName().equals(field))
                .findFirst()
                .orElse(null);
        if (ent == null)
            throw new Exception("Field " + field + " not present in table.");

        if (value == null) {
            result = "null,";
            return result;
        }
        switch (ent.getColumnType()) {
            case "varchar":
            case "text":{
                result = "'" + value.replaceAll("'","''") +"'";
                break;
            }
            case "timestamp": {
                try {
                    DateFormat df = new SimpleDateFormat(Main.getProperty("csv.dateTimeFormat"));
                    result = "'" + df.parse(value) +"'";
                } catch (Exception e) {
                    result = "null";
                }
                break;
            }
            case "date":{
                try {
                    DateFormat df = new SimpleDateFormat(Main.getProperty("csv.dateFormat"));
                    result = "'" + df.parse(value) +"'";
                } catch (Exception e) {
                    result = "null";
                }
                break;
            }
            case "int8":
            case "int4":{
                result = value.trim();
                if (result.contains("."))
                    result = result.substring(0, result.indexOf("."));
                if (result.isEmpty())
                    result = "null";
                break;
            }
            case "boolean": {
                result = value.trim();
                if (result.isEmpty())
                    result = "false";
                else
                   try {
                        result = Boolean.valueOf(value).toString();
                   } catch (Exception e) {
                         result = "null";
                   }
                break;
            }
            case "numeric": {
                result = value.trim();
                if (result.isEmpty())
                    result = "null";
                else
                    try {
                        result = Double.valueOf(value).toString();
                    } catch (Exception e) {
                        result = "null";
                    }
                break;
            }
            default: {
                result = value;
                break;
            }

        }
        result += ",";
        return result;
    }
}
