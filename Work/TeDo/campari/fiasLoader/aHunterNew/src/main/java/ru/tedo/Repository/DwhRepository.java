package ru.tedo.Repository;

import ru.tedo.Main;
import ru.tedo.Model.AddressEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DwhRepository {
    private int chankSize = 100;

    /**
     * Getting list of addresses to be verified by aHunter.
     * We have to verify all addresses with statusid = 2 and with empty or null fiascode.
     * @return
     * @throws SQLException
     */
    public List<AddressEntity> getAddressList() throws SQLException {
        List<AddressEntity> result = new ArrayList<>();
        String limit = Main.getProperty("processing.maxRecordSize");
        DbConnection pg = new DbConnection();
        String sql = "select * from target_edw.partnerstore \n" +
                " where statusid = 2 and \n" +
                " (fiascode = '' or fiascode is null) \n" +
                "limit " + limit;
        ResultSet rs = pg.executeSqlQuery(sql);
        while (rs.next()) {
            AddressEntity ae = new AddressEntity();
            ae.setAddressID(rs.getInt("id"));
            ae.setOriginString(rs.getString("address"));
            ae.setStatusID(rs.getInt("statusid"));
            result.add(ae);
        }
        rs.getStatement().close();
        rs.close();

        return result;
    }

    /**
     * Save aHunter data to database.
     * based on aHunter data several fields have to be updated
     * @param addressList - list of addresses for update data
     * @throws SQLException
     */
    public void saveAddressList(List<AddressEntity> addressList) throws SQLException {
          DbConnection pg = new DbConnection();
          StringBuilder sb = new StringBuilder("");
          String sql = "";
          int chankCount = 0;


          for (AddressEntity addr: addressList) {
              // Если произошла ошибка, то не записываем это в БД.
              if (addr.getStatusID() == -1)
                  continue;
              String regionCode = "";
              if (addr.getFiasCode().length()>2)
                  regionCode = addr.getFiasCode().substring(0,2);
              sb.append("update target_edw.partnerstore " +
                      "   set fiascode = '" + addr.getFiasCode() + "'," +
                      "   latitude = "+ addr.getLatitude() + "," +
                      "   longitude = "+ addr.getLongitude() + "," +
                      "   ahunter_precision = "+ addr.getQuality().intValue() + "," +
                      "   pretty_address = '"+ addr.getPreatyString().replaceAll("'","''") + "'," +
                      "   fiasobjectid = '"+ addr.getFiasObjectID() + "'," +
                      "   fiasobjectlevel = '"+ addr.getFiasObjectLevel() + "'," +
                      "   faishouseid = '"+ addr.getFiasHouseID() + "'," +
                      "   house = '"+ addr.getHouse() + "'," +
                      "   city = '"+ addr.getCity().replaceAll("'","''") + "'," +
                      "   regioncode = '"+ regionCode + "'," +
                      "   statusid = "+ addr.getStatusID() +
                      " where id = "+ addr.getAddressID() + ";\n");
              chankCount++;
              if (chankCount > chankSize) {
                  sql = sb.toString();
                  pg.executeSql(sql);
                  chankCount =0;
              }
          }
          if (chankCount > 0) {
              sql = sb.toString();
              pg.executeSql(sql);
          }
    }
}
