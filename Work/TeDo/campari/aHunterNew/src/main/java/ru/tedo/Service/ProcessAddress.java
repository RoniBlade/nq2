package ru.tedo.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Repository.DbConnection;
import ru.tedo.Main;
import ru.tedo.Model.AddressEntity;
import ru.tedo.Model.AddressFieldEntity;
import ru.tedo.Model.CommonHttpResponseEntity;
import ru.tedo.Model.HttpProxyEntity;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class ProcessAddress {
    final static Logger logger = LoggerFactory.getLogger(Main.class);
    private HttpProxyEntity proxy = null;


    public ProcessAddress() {
        String isProxy = Main.getProperty("proxy.isProxy");
        if (isProxy.equals("true")) {
            proxy = new HttpProxyEntity();
            proxy.setProxyHost(Main.getProperty("proxy.url"));
            proxy.setProxyPort(Integer.valueOf(Main.getProperty("proxy.port")));
            proxy.setProxyUser(Main.getProperty("proxy.userName"));
            proxy.setProxyPassword(Main.getProperty("proxy.userPassword"));
        }

    }

    /**
     * Process address function
     * @param address - initial address which should be processed
     * @throws Exception
     */
    public void processAddressString(AddressEntity address) throws Exception {
       // System.out.println("Process address");
        if (address.getOriginString().length()<10) {
            address.setStatusID(4);
            return;
        }
        String url = Main.getProperty("aHunter.url");
        String key = Main.getProperty("aHunter.key");
        String mode = Main.getProperty("aHunter.searchMode");
        String request = url+"?user=" + key + ";mode="+mode+";output="+URLEncoder.encode("json|afiasall")+";query=" + URLEncoder.encode(address.getOriginString(), Charset.forName("UTF-8"));
        CommonHTTPrequestService req = new CommonHTTPrequestService();
        CommonHttpResponseEntity resp = req.processHttpGetRequest(request,300,proxy);
        if (resp.getResponseCode() != 200) {
            logger.error("HTTP request to aHunter returns error code = " + resp.getResponseCode());
            address.setStatusID(-1);
            return;
        }

        JSONObject jsonResponse = new JSONObject(resp.getBody());
       // Ошибка часто еще и с кодом 200 приходит.
       // но вот так хитро в Json
        if (jsonResponse.has("errors")) {
            String error = "";
            JSONArray array = jsonResponse.getJSONArray("errors");
            for (int i = 0; i < array.length(); i++) {
                error += "Code " + array.getJSONObject(i).getString("code") + "   "+
                         "Description " + array.getJSONObject(i).getString("description") + "\n";
            }
            error = error.replaceAll("<h1>","");
            error = error.replaceAll("</h1>","");
            error = error.replaceAll("<p>","");
            error = error.replaceAll("</p>","");
            logger.error(error);
            address.setStatusID(-1);
            return;
        }

        if (jsonResponse.has("check_info")) {
            JSONObject info = jsonResponse.getJSONObject("check_info");
            if (info.has("status") && info.getInt("alts") == 0) {
                logger.info("No found options for provided address string.");
                address.setStatusID(5);
                return;
            }

        }

        if (jsonResponse.has("addresses") && jsonResponse.getJSONArray("addresses").length() > 0) {
            JSONObject jsonObject = jsonResponse.getJSONArray("addresses").getJSONObject(0);
            processAddressJson(jsonObject, address);
        }

    }

    /**
     * Process received JSON for address and fill processing address fields
     * @param jsonObject - JSON for process
     * @param address - Address which should be enhanced
     * @throws JSONException
     */
    private void processAddressJson(JSONObject jsonObject, AddressEntity address) throws JSONException {
        if (jsonObject.has("fields")) {
            JSONArray ml = jsonObject.getJSONArray("fields");
            if (ml != null) {
                for (int i = 0; i < ml.length(); i++) {
                    AddressFieldEntity field = fillAddressField(ml.getJSONObject(i));
                    address.getAddressFields().put(field.getLevel(),field);
                }
            }
        }
        AddressFieldEntity city = address.getAddressFields().get("City");
        if (city != null) {
            if (city.getName().isEmpty())
                city = address.getAddressFields().get("District");
            if (city.getName().isEmpty())
                city = address.getAddressFields().get("Region");
            address.setCity(city.getName());
        }

        AddressFieldEntity house = address.getAddressFields().get("House");
        if (house != null)
            address.setHouse(house.getName());


        if (jsonObject.has("codes") &&
            jsonObject.getJSONObject("codes").has("fias_actual_code")) {
            address.setFiasCode(jsonObject.getJSONObject("codes").getString("fias_actual_code"));
            if (jsonObject.getJSONObject("codes").has("fias_house"))
                address.setFiasHouseID(jsonObject.getJSONObject("codes").getString("fias_house"));
            if (jsonObject.getJSONObject("codes").has("fias_object"))
                address.setFiasObjectID(jsonObject.getJSONObject("codes").getString("fias_object"));
            if (jsonObject.getJSONObject("codes").has("fias_object_level"))
                address.setFiasObjectLevel(jsonObject.getJSONObject("codes").getString("fias_object_level"));
        }

        if (jsonObject.has("geo_data")) {
            address.setLatitude(jsonObject.getJSONObject("geo_data").getJSONObject("mid").getDouble("lat"));
            address.setLongitude(jsonObject.getJSONObject("geo_data").getJSONObject("mid").getDouble("lon"));
        }

        if (jsonObject.has("quality"))
            address.setQuality(jsonObject.getJSONObject("quality").getDouble("precision"));
        if (jsonObject.has("pretty"))
            address.setPreatyString(jsonObject.getString("pretty"));



    }

    private AddressFieldEntity fillAddressField(JSONObject json) throws JSONException {
        AddressFieldEntity result = new AddressFieldEntity();
        if (json.has("c"))
            result.setC(json.getString("c"));
        if (json.has("cover"))
            result.setCover(json.getString("cover"));
        if (json.has("level"))
            result.setLevel(json.getString("level"));
        if (json.has("name"))
            result.setName(json.getString("name"));
        if (json.has("ns"))
            result.setNs(json.getDouble("ns"));
        if (json.has("ts"))
            result.setTs(json.getDouble("ts"));
        if (json.has("type"))
            result.setType(json.getString("type"));
        return result;
    }
}
