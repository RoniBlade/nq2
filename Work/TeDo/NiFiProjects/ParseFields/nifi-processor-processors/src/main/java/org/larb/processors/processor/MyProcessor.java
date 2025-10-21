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
package org.larb.processors.processor;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SupportsBatching
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"example", "csv", "process"})
@CapabilityDescription("Example processor that reads a CSV structure file and updates the content of a FlowFile.")
public class MyProcessor extends AbstractProcessor {

    public static final PropertyDescriptor STRUCTURE_FILE_PATH = new PropertyDescriptor
            .Builder().name("Structure File Path")
            .description("Path to the structure file.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully processed FlowFile")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to process FlowFile")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(STRUCTURE_FILE_PATH);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return this.descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        final String structureFilePath = context.getProperty(STRUCTURE_FILE_PATH).getValue();
        final String tableName = flowFile.getAttribute("file.TableName");
        final String schema = flowFile.getAttribute("tables.Schema");
        final String fileOwner = flowFile.getAttribute("file.owner");

        String lvStructureFilePath = structureFilePath + fileOwner + "/" + tableName + ".csv";
        File lvFile = new File(lvStructureFilePath);
        StringBuilder lvFields = new StringBuilder();
        StringBuilder lvKeys = new StringBuilder();

        try (BufferedReader lvReader = new BufferedReader(new FileReader(lvFile))) {
            boolean lvIsFirst = true;
            String lvLine;

            while ((lvLine = lvReader.readLine()) != null) {
                if (lvIsFirst) {
                    lvIsFirst = false;
                    continue;
                }

                if (lvLine.trim().isEmpty()) {
                    continue;
                }

                String[] lvParams = lvLine.split(";");
                if (lvParams.length != 12) {
                    continue;
                }

                lvFields.append(" \"").append(lvParams[0]).append("\" ");
                switch (lvParams[7].toUpperCase()) {
                    case "T":
                        lvFields.append("time without time zone");
                        break;
                    case "D":
                        lvFields.append("date");
                        break;
                    case "F":
                    case "P":
                        lvFields.append("double precision");
                        break;
                    default:
                        lvFields.append("character(").append(Integer.parseInt(lvParams[4])).append(")");
                        break;
                }

                if ("X".equalsIgnoreCase(lvParams[9])) {
                    lvKeys.append("\"").append(lvParams[0]).append("\",");
                    lvFields.append(" NOT NULL");
                }
                lvFields.append(",");
            }

        } catch (FileNotFoundException e) {
            logger.error("The file with structure '{}' was not found at path: {}", new Object[]{tableName, lvStructureFilePath});
            flowFile = session.putAttribute(flowFile, "file.Message", "The file with structure '" + tableName + "' was not found");
            session.transfer(flowFile, REL_FAILURE);
            return;
        } catch (IOException e) {
            logger.error("Error reading the structure file.", e);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        if (lvKeys.length() > 0) {
            lvKeys.setLength(lvKeys.length() - 1);
        }

        final StringBuilder finalLvFields = new StringBuilder(lvFields.toString());
        final StringBuilder finalLvKeys = new StringBuilder(lvKeys.toString());

        flowFile = session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream in, OutputStream out) throws IOException {
                String lvFileContent = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                lvFileContent = lvFileContent.replace("&1", schema)
                        .replace("&2", tableName)
                        .replace("&3", finalLvFields.toString())
                        .replace("&4", finalLvKeys.toString());

                out.write(lvFileContent.getBytes(StandardCharsets.UTF_8));
            }
        });

        flowFile = session.putAttribute(flowFile, "table.Keys", lvKeys.toString());
        session.transfer(flowFile, REL_SUCCESS);
    }
}
