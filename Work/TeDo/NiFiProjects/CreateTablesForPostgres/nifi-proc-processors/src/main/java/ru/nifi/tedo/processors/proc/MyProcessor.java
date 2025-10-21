/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.nifi.tedo.processors.proc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"example"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class MyProcessor extends AbstractProcessor {
    public static final PropertyDescriptor FILE_SEPARATOR = new PropertyDescriptor
            .Builder().name("file.Separator")
            .displayName("Separator")
            .description("File Separator")
            .required(true)
            .allowableValues(";", "\\|", "~", "✀")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor ORDER_BY_FILE_PATH = new PropertyDescriptor
            .Builder().name("orderByFilePath")
            .displayName("Order By File Path")
            .description("Path to the file containing order by columns")
            .required(true)
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
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final String tableName = flowFile.getAttribute("file.TableName");
        final String schemaName = flowFile.getAttribute("tables.Schema");
        final String filePath = flowFile.getAttribute("file.Path");
        final String fileName = flowFile.getAttribute("filename");

        final ComponentLog logger = getLogger();
        StringBuilder createTableQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(schemaName).append(".").append("\"").append(tableName).append("\"")
                .append(" (TEDO_UPDATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP ,");

        List<String> primaryKeys = new ArrayList<>();

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

                if (fields[9] != null && !fields[9].trim().isEmpty()) {
                    createTableQuery.append("\"").append(fieldName).append("\" ").append(fieldType).append(" NOT NULL, ");
                    primaryKeys.add(fieldName.trim());
                } else {
                    createTableQuery.append("\"").append(fieldName).append("\" ").append(fieldType).append(", ");
                }
            }

            if (createTableQuery.length() > 0) {
                createTableQuery.setLength(createTableQuery.length() - 2);
                createTableQuery.append(")");
            }

            if (!primaryKeys.isEmpty()) {
                createTableQuery.append(" PRIMARY KEY (\"").append(String.join("\", \"", primaryKeys)).append("\")");
            }

            createTableQuery.append(";");

            flowFile = session.putAttribute(flowFile, "sql.Query", createTableQuery.toString());
            session.transfer(flowFile, REL_SUCCESS);
        } catch (IOException e) {
            logger.error("Failed to process file due to {}", new Object[]{e.toString(), e});
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private String mapFieldType(String abapType) {
        switch (abapType) {
            case "D":
                return "DATE";
            case "F":
            case "P":
                return "FLOAT";
            case ("T"):
                return "TIMESTAMP";
            default:
                return "TEXT";
        }
    }
}
