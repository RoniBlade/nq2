package lct.tomorrowgen.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lct.tomorrowgen.config.SendersInterface;
import lct.tomorrowgen.mapper.XlsxKafkaMapper;
import lct.tomorrowgen.model.xlsx.XlsxInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

@Component
@Slf4j
public class XlsxProcessor<T extends XlsxInterface> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private XlsxKafkaMapper xlsxKafkaMapper;

    public void process(String filePath, Class<T> modelClass, SendersInterface sendersInterface) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = new XSSFWorkbook(fis);

            Sheet sheet = workbook.getSheetAt(0);

            List<T> result = new LinkedList<>();

            for (Row row : sheet) {
                if(row.getRowNum() == 0) {
                    continue;
                }

                T instance = modelClass.newInstance();

                int cellCount = Math.min(row.getLastCellNum(), modelClass.getDeclaredFields().length);

                for (int i = 0; i < cellCount; i++) {
                    Cell cell = row.getCell(i);

                    Object cellValue = null;

                    if(cell != null) {
                        cellValue = switch (cell.getCellType()) {
                            case STRING -> cell.getStringCellValue();
                            case NUMERIC -> cell.getNumericCellValue();
                            case BOOLEAN -> cell.getBooleanCellValue();
                            default -> "";
                        };
                    }

                    instance.setField(i, cellValue);
                }
                sendersInterface.send(objectMapper.writeValueAsString(xlsxKafkaMapper.toKafkaMessage(instance)));
            }
        }
    }
}