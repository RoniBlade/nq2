package ru.tedo.nifi.processors.processors.ru.tedo.nifi.processors.CreateTable;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Tags({"example"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "table.OrderBy", description = "Fields to order by"),
        @ReadsAttribute(attribute = "file.TableName", description = "Table name"),
        @ReadsAttribute(attribute = "file.ClusterName", description = "Cluster name")})
@WritesAttributes({@WritesAttribute(attribute = "", description = "")})
public class CreateTable extends AbstractProcessor {

    public static final PropertyDescriptor FILE_SEPARATOR = new PropertyDescriptor
            .Builder().name("file.Separator")
            .displayName("Separator")
            .description("File Separator")
            .required(true)
            .allowableValues(";", "\\|", "~", "✀", "\t")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor ORDER_BY_FILE_PATH = new PropertyDescriptor
            .Builder().name("orderByFilePath")
            .displayName("Order By File Path")
            .description("Path to the file containing order by columns")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TTL_INTERVAL = new PropertyDescriptor
            .Builder().name("ttl.Interval")
            .displayName("TTL Interval")
            .description("Interval in days for TTL")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor DISK_NAME = new PropertyDescriptor
            .Builder().name("disk.Name")
            .displayName("Disk Name")
            .description("Name of the disk for TTL storage")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("REL_FAILURE")
            .description("Failure")
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("REL_SUCCESS")
            .description("Success")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(FILE_SEPARATOR);
        descriptors.add(ORDER_BY_FILE_PATH);
        descriptors.add(TTL_INTERVAL);
        descriptors.add(DISK_NAME);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(REL_FAILURE);
        relationships.add(REL_SUCCESS);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        // Нет необходимости изменять метод onScheduled, он остаётся пустым.
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final String tableName = flowFile.getAttribute("file.TableName");
        final String clusterName = flowFile.getAttribute("file.ClusterName");
        final String schemaName = flowFile.getAttribute("tables.Schema");
        final String filePath = flowFile.getAttribute("file.Path");
        final String fileName = flowFile.getAttribute("filename");

        List<String> orderByFields = getOrderByFields(context.getProperty(ORDER_BY_FILE_PATH).getValue(), tableName);

        final ComponentLog logger = getLogger();
        StringBuilder createTableQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(schemaName).append(".").append("\"").append(tableName).append("\"")
                .append(" ON cluster ").append(clusterName)
                .append(" (TEDO_UPDATED DateTime Default now() ,");

//        List<String> primaryKeys = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath + fileName))) {
            String line;
            if ((line = reader.readLine()) == null) {
                logger.error("Input file is empty");
                session.transfer(flowFile, REL_FAILURE);
                return;
            }

            while ((line = reader.readLine()) != null) {
                if (StringUtils.isBlank(line)) continue;

                String[] fields = line.split(context.getProperty(FILE_SEPARATOR).getValue());
                if (fields.length < 12) {
                    logger.error("Incorrect format: {}", new Object[]{line});
                    session.transfer(flowFile, REL_FAILURE);
                    return;
                }
                String fieldName = fields[0].trim();
                String fieldType = mapFieldType(fields[7].trim());


                if (orderByFields.contains(fieldName)) {
                    createTableQuery.append("\"").append(fieldName).append("\" ").append(fieldType).append(" NOT NULL, ");
//                    primaryKeys.add(fieldName.trim());
                } else {
                    createTableQuery.append("\"").append(fieldName).append("\" ").append(fieldType).append(", ");
                }
            }

            if (createTableQuery.length() > 0) {
                createTableQuery.setLength(createTableQuery.length() - 2);
                createTableQuery.append(")\nENGINE = ReplicatedReplacingMergeTree(TEDO_UPDATED)");
            }

            if (!orderByFields.isEmpty()) {
                createTableQuery.append("\nORDER BY (\"").append(String.join("\", \"", orderByFields)).append("\")");
            } else {
                logger.error("No order by fields found for the table {}", new Object[]{tableName});
                session.transfer(flowFile, REL_FAILURE);
                return;
            }

//            if (!primaryKeys.isEmpty()) {
//                createTableQuery.append("\nPRIMARY KEY (\"").append(String.join("\", \"", orderByFields)).append("\")");
//            }

            TTLProperties ttlProperties = getTTLProperties(context.getProperty(ORDER_BY_FILE_PATH).getValue(), tableName);
            if (ttlProperties != null && context.getProperty(TTL_INTERVAL).isSet() && context.getProperty(DISK_NAME).isSet()) {
                String ttlInterval = context.getProperty(TTL_INTERVAL).getValue();
                String diskName = context.getProperty(DISK_NAME).getValue();
                createTableQuery.append("\npartition by (").append("TEDO_UPDATED, ").append(ttlProperties.getTtlValue()).append(")");
                createTableQuery.append("\nTTL makeDate(toUInt64(").append(ttlProperties.getTtlValue()).append("),1,1) + INTERVAL ").append(ttlInterval).append(" day TO DISK '").append(diskName).append("'");
            } else {
                createTableQuery.append("\npartition by TEDO_UPDATED");
            }
            createTableQuery.append(" FORMAT CSV");

            flowFile = session.putAttribute(flowFile, "sql.Query", createTableQuery.toString());
            session.transfer(flowFile, REL_SUCCESS);
        } catch (IOException e) {
            logger.error("Failed to process file due to {}", new Object[]{e.toString(), e});
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private List<String> getOrderByFields(String orderByFilePath, String tableName) {
        List<String> orderByFields = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(Files.readAllBytes(Paths.get(orderByFilePath)));
            for (JsonNode tableNode : rootNode) {
                if (tableNode.path("TABLE").asText().equals(tableName)) {
                    String[] fields = tableNode.path("ORDER BY").asText().split(", ");
                    Collections.addAll(orderByFields, fields);
                    break;
                }
            }
        } catch (IOException e) {
            getLogger().error("Failed to read order by fields from JSON file due to {}", new Object[]{e.toString(), e});
        }
        return orderByFields;
    }

    private TTLProperties getTTLProperties(String orderByFilePath, String tableName) {
        TTLProperties ttlProperties = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(Files.readAllBytes(Paths.get(orderByFilePath)));
            for (JsonNode tableNode : rootNode) {
                if (tableNode.path("TABLE").asText().equals(tableName)) {
                    if (tableNode.has("TTL")) {
                        ttlProperties = new TTLProperties(tableNode.path("TTL").asText());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            getLogger().error("Failed to read TTL properties from JSON file due to {}", new Object[]{e.toString(), e});
        }
        return ttlProperties;
    }

    private String mapFieldType(String abapType) {
        switch (abapType) {
            case "D":
                return "Date32";
            case "F":
            case "P":
                return "Float64";
            case ("T"):
                return "DateTime";
            default:
                return "String";
        }
    }

    private static class TTLProperties {
        private final String ttlValue;

        public TTLProperties(String ttlValue) {
            this.ttlValue = ttlValue;
        }

        public String getTtlValue() {
            return ttlValue;
        }
    }
}
