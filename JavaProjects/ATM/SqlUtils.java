import java.lang.StringBuilder;

public class SqlUtils {

    private final String table = "d_get_m_user";
    private final String baseSelect = "select * from public." + table;

    private String getSqlQuery(String where, String orderBy, Long limit, Long offset) {
        StringBuilder stringBuilder = new StringBuilder(baseSelect);

        return stringBuilder
                .append(baseSelect)
                .append(" ( \np_limit => ").append(limit)
                .append(", \np_offset => ").append(offset)
                .append(", \np_where => ").append(where)
                .append(", \np_order_by => ").append(orderBy)
                .append("\n);")
                .toString();
    }

    public static void main(String[] args) {
        SqlUtils utils = new SqlUtils();

        System.out.println(utils.getSqlQuery("where nameorig like '%loc_user%", "nameorig ASC", 100l, 0l));
    }

}
