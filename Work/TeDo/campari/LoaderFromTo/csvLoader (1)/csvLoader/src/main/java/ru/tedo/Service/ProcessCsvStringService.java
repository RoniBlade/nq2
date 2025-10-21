package ru.tedo.Service;

import ru.tedo.Main;
import ru.tedo.Repository.DwhRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessCsvStringService {

    Character maskSymbol = null;
    Character startSymbol = null;
    Character separator = ';';

    public ProcessCsvStringService() {
        if (!Main.getProperty("csv.maskStartSymbol").isEmpty())
            maskSymbol = Main.getProperty("csv.maskStartSymbol").charAt(0);
        if (!Main.getProperty("csv.startValueSymbol").isEmpty())
            startSymbol = Main.getProperty("csv.startValueSymbol").charAt(0);
        if (!Main.getProperty("csv.separator").isEmpty())
            separator = Main.getProperty("csv.separator").charAt(0);
    }

    public void processChunk(List<String> chunk,Map<Integer,String> mapping, Integer connectionNum) throws Exception {
        List<Map<String,String>> fields = new ArrayList<>();

        int size = 0;
        for (Map.Entry<Integer,String> ent : mapping.entrySet()) {
            if (ent.getKey() > size)
                size = ent.getKey();
        }
        for (String str : chunk) {
            String[] values = parseString(str, size);
            fields.add(prepareSqlData(values, mapping));
        }

        DwhRepository dr = new DwhRepository();
        dr.saveRows(fields, connectionNum);


    }

    private Map<String,String> prepareSqlData(String[] values, Map<Integer, String> mapping) {
        Map<String,String> result = new HashMap<>();
        for (Map.Entry<Integer,String> ent : mapping.entrySet())
            result.put(ent.getValue(),values[ent.getKey()-1]);

        return result;
    }



    private String[] parseString(String sourceStr, Integer size) {
        String[] result = new String[size];
        String value = "";
        Boolean isStart = false;
        int i = 0;
        int fieldNum = 0;
        while ( i< sourceStr.length()) {
            if (((startSymbol != null && sourceStr.charAt(i) == startSymbol) ||
                 (startSymbol == null && sourceStr.charAt(i) != separator))
                    && !isStart) {
                i++;
                isStart = true;
                if (startSymbol == null)
                    value += sourceStr.charAt(i);
                else
                    value = "";
                continue;
            }
            if ((maskSymbol != null && sourceStr.charAt(i) == maskSymbol)
                    && isStart
                    && (startSymbol != null && sourceStr.charAt(i+1) == startSymbol)) {
                value = value + startSymbol;
                i+=2;
                continue;
            }

            if ((   (startSymbol != null && sourceStr.charAt(i) == startSymbol) ||
                    (startSymbol == null && sourceStr.charAt(i) == separator) ||
                    (i == sourceStr.length()) )
                    && isStart) {
                if (startSymbol != null)
                    i++;
                else
                    if (sourceStr.charAt(i) != separator)
                       value += sourceStr.charAt(i);
                i++;
                isStart = false;
                result[fieldNum] = value;
                fieldNum++;
                value = "";
                // И не читаем хвост если все нужные поля считаны
                if (fieldNum >= size)
                    break;

                continue;
            }
            value += sourceStr.charAt(i);
            i++;

        }

        return result;
    }
}
