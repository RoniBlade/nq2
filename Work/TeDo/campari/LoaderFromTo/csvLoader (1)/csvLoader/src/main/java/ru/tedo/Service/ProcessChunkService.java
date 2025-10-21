package ru.tedo.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessChunkService extends Thread {
    final static Logger logger = LoggerFactory.getLogger(ProcessChunkService.class);
    List<String> chunk = new ArrayList<>();
    Map<Integer,String > mapping = new HashMap<>();
    Integer connectionNum = 0;

    public ProcessChunkService (List<String> processChunk, Map<Integer,String > mapping, Integer connectionNum) {
        this.chunk = processChunk;
        this.connectionNum = connectionNum;
        this.mapping = mapping;
    }
    @Override
    public void run() {
        ProcessCsvStringService ps = new ProcessCsvStringService();
        try {
            ps.processChunk(this.chunk, mapping, connectionNum);
        } catch (Exception e) {
            logger.error("Exception in process chunk " + e.getMessage());
        }
    }

}
