package ru.tedo.Service;

import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Main;
import ru.tedo.Repository.DwhRepository;

import java.io.*;
import java.util.*;

public class UploadCsvFileService {
    final static Logger logger = LoggerFactory.getLogger(UploadCsvFileService.class);
    public void uploadCsvFile() throws Exception {
        logger.info("Start upload csv file");
        DwhRepository dr = new DwhRepository();

        Boolean isTruncate = Boolean.valueOf(Main.getProperty("database.truncateBeforeUpload"));
        if (isTruncate)
            dr.truncateTable();


        int firstRow = Integer.valueOf(Main.getProperty("csv.firstDataRow"));
        int rowsChunk = Integer.valueOf(Main.getProperty("csv.maxRowsChunk"));
        int threads = Integer.valueOf(Main.getProperty("database.maxThreadNum"));

        Map<Integer,String> mapping = getTableFieldMapping();
        List<Map<String,String>> rowsList = new ArrayList<>();
        File file = new File( Main.getProperty("csv.fileName"));
        int rowNum = 0;
        int chunk = 0;
        List<String> rows = new ArrayList<>();
        Object[] chunkArray = new Object[threads+1];
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        try {
            String line;
            while((line = br.readLine()) != null){
                // обработка строки
                rowNum ++;
                if (rowNum < firstRow )
                    continue;
                rows.add(line);
                if (rows.size() >= rowsChunk) {
                    chunkArray[chunk] = rows;
                    chunk++;
    //                ps.processChunk(rows,mapping,0);
                    rows = new ArrayList<>();
                }
                if (chunk >= threads) {
                    startThreads(chunkArray, mapping);
                    chunkArray = new Object[threads + 1];
                    chunk = 0;
                }
            }
        } catch (IOException e) {
           logger.error("File read exception " + e.getMessage());
           throw e;
        }
        finally {
            br.close();
            fr.close();
        }

        chunkArray[chunk] = rows;
        startThreads(chunkArray,mapping);
    }

    private void startThreads(Object[] chunkArray, Map<Integer,String> mapping) {
        List<ProcessChunkService> threadList = new ArrayList<>();
        logger.info("Start database upload threads.");
        for (int i = 0; i < chunkArray.length; i++) {
            if (chunkArray[i] != null) {
                List<String> rows = (List<String>) chunkArray[i];
                ProcessChunkService cs = new ProcessChunkService(rows,mapping,i);
                threadList.add(cs);
                cs.start();
            }
        }
        logger.info("All database upload threads have been started.");
        for (ProcessChunkService cs: threadList) {
            try {
                if (cs != null)
                    cs.join();
            } catch (Exception e) {
                logger.error("Error join thread");
            }
        }
        logger.info("All database upload threads have been finished.");

    }


    private Map<Integer,String> getTableFieldMapping() throws Exception {
    Map<Integer,String> result = new HashMap<>();
    String settings = Main.getProperty("csv.columnMapping");
    for (String str : settings.split(";")) {
        if (!str.contains("="))
            throw new Exception("Field mapping string incorrect.");
        Integer key = Integer.valueOf(str.substring(0,str.indexOf("=")));
        String value = str.substring(str.indexOf("=")+1);
        result.put(key,value);

    }

    return result;

}
}
