package com.glt.idm.rest.utils;

import java.io.*;
import java.util.ArrayList;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glt.idm.rest.controllers.GltRestController;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;



public class GltUtilites {

    private static final Trace LOGGER = TraceManager.getTrace(GltRestController.class);

    public static String csvToxls(String filePath) throws IOException {
        ArrayList<String> oneRowData = null;
        ArrayList<ArrayList<String>> allRowAndColumnData = null;
        String currentLine;
        String fullNameAndPathXlsFile = filePath.replaceAll(".csv",".xls");
        FileInputStream fileInStr = new FileInputStream(filePath);
        DataInputStream fileStream = new DataInputStream(fileInStr);

        int inumber = 0;
        int icoll =0;
        allRowAndColumnData = new ArrayList<ArrayList<String>>();
        while ((currentLine = fileStream.readLine()) != null) {
            oneRowData = new ArrayList<String>();
            currentLine=currentLine.replaceAll("\"","");

            String[] oneRowArray = currentLine.split(";");
            for (int j = 0; j < oneRowArray.length; j++) {
                oneRowData.add(oneRowArray[j]);
            }
            allRowAndColumnData.add(oneRowData);

            inumber++;
        }

        try {

            HSSFWorkbook workBook = new HSSFWorkbook();

            HSSFSheet sheet = workBook.createSheet("Report");

            for (int i = 0; i < allRowAndColumnData.size(); i++) {
                ArrayList<?> arowdata = (ArrayList<?>) allRowAndColumnData.get(i);
                HSSFRow row = sheet.createRow((short) 0 + i);
                icoll=arowdata.size();
                for (int k = 0; k < arowdata.size(); k++) {

                    String cellData= arowdata.get(k).toString();
                    byte[] btext = cellData.getBytes(ISO_8859_1);
                    String utf8text = new String(btext, UTF_8);



                    HSSFCell cell = row.createCell((short) k);
                    cell.setCellValue(utf8text);

                }

            }
            CellRangeAddress region = new CellRangeAddress(0, inumber+1, 0, icoll+1);
            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);




            for (int bi = 1; bi <= icoll; bi++) {
                sheet.autoSizeColumn(bi);
            }


            FileOutputStream fileOutputStream =  new FileOutputStream(fullNameAndPathXlsFile);
            workBook.write(fileOutputStream);
            fileOutputStream.close();
        } catch (Exception ex) {
        }
        return fullNameAndPathXlsFile;
    }
    public static String getPolyStringToLang (String json,String lang){
        String value="";
        boolean isPoly=json.contains("orig");
    if (isPoly) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        // Получаем узел "lang"
        JsonNode langNode = rootNode.get("lang");
        if (langNode != null) {
            // Извлекаем значение "ru"
            value = langNode.get(lang).asText();
        } else {
            System.out.println("Узел 'lang' не найден");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
} else {
    value=json;
}
    return value;
}

    private static int count(String str, String target) {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }

    private static byte[] resizeImage(byte[] imageData, int width, int height) throws Exception {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));

        // Изменение размера с сохранением пропорций и обрезкой
        BufferedImage resizedImage = Thumbnails.of(originalImage)
                .size(width, height)
                .keepAspectRatio(false) // true - сохранить пропорции (с добавлением полей)
                .outputQuality(0.9)
                .asBufferedImage();

        // Конвертация обратно в byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = detectFormat(imageData);
        ImageIO.write(resizedImage, format, baos);
        return baos.toByteArray();
    }

    private static String detectFormat(byte[] imageData) {
        // Определение формата по сигнатуре
        if ((imageData[0] & 0xFF) == 0xFF && (imageData[1] & 0xFF) == 0xD8) return "jpg";
        if ((imageData[0] & 0xFF) == 0x89 && imageData[1] == 0x50) return "png";
        return "jpg"; // Дефолтный формат
    }

    public static String detectMimeType(byte[] imageBytes) {
        if (imageBytes.length >= 4) {
            // Пример для PNG: сигнатура 89 50 4E 47
            if ((imageBytes[0] & 0xFF) == 0x89 && imageBytes[1] == 0x50 &&
                    imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
                return "image/png";
            }
            // JPEG: FF D8 FF
            if ((imageBytes[0] & 0xFF) == 0xFF && (imageBytes[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
        }
        return "application/octet-stream"; // Дефолтный тип
    }



    public  HashMap<String, String> getAttributesUserTypeWithJavaTypes(PrismContext prismContext) {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        HashMap<String, String> attributeMap = new HashMap();
        List<String> allAttributes = new ArrayList<>();
        userDef.getDefinitions().forEach(itemDef ->
                attributeMap.put(itemDef.getItemName().getLocalPart(),itemDef.getTypeName().getLocalPart())

        );
        // Атрибуты из расширений (кастомные)
        PrismContainerDefinition<?> extensionDef = userDef.findContainerDefinition(UserType.F_EXTENSION);
        if (extensionDef != null) {
                 //   extensionDef.getDefinitions().forEach(itemDef -> attributeMap.put(itemDef.getItemName().getLocalPart(),itemDef.getTypeName().getLocalPart()));

            String[] array = {};
            for (var itemDef : extensionDef.getDefinitions()) {
                attributeMap.put(
                        itemDef.getItemName().getLocalPart(),
                        itemDef.getTypeName().getLocalPart()
                );




            }
        }

        return attributeMap;
    }


    private static String[] getEnumValuev(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", ");
    }



}


