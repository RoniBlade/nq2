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
	package ru.nifi.tedo.processors.comparestructure;
	
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
	import org.apache.nifi.processor.AbstractProcessor;
	import org.apache.nifi.processor.ProcessContext;
	import org.apache.nifi.processor.ProcessSession;
	import org.apache.nifi.processor.ProcessorInitializationContext;
	import org.apache.nifi.processor.Relationship;
	import org.apache.nifi.processor.util.StandardValidators;
	
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.HashSet;
	import java.util.List;
	import java.util.Set;
	
	import java.io.BufferedReader;
	import java.io.ByteArrayOutputStream;
	import java.io.FileReader;
	import java.io.IOException;
	import java.nio.file.Files;
	import java.nio.file.Paths;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.HashSet;
	import java.util.LinkedHashMap;
	import java.util.List;
	import java.util.Map;
	import java.util.Set;
	import com.fasterxml.jackson.databind.JsonNode;
	import com.fasterxml.jackson.databind.ObjectMapper;
	
	@Tags({ "Compare, Structure, TeDo" })
	@CapabilityDescription("Provide a description")
	@SeeAlso({})
	@ReadsAttributes({ @ReadsAttribute(attribute = "", description = "") })
	@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })
	public class CompareStructureClick extends AbstractProcessor {
	
		private static LinkedHashMap<String, String> mvFlowLines = new LinkedHashMap<>();
		private static LinkedHashMap<String, String> mvServerLines = new LinkedHashMap<>();
	
		public static final PropertyDescriptor FILE_SEPARATOR = new PropertyDescriptor.Builder().name("file.Separator")
				.displayName("Separator").description("Value to split a string").required(true)
				.allowableValues(";", "\\|", "~", "✀").addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
	
		public static final PropertyDescriptor DATA_DIR = new PropertyDescriptor.Builder().name("file.DataPath")
				.displayName("Data Path").description("The path of data directory").required(true)
				.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
	
		public static final PropertyDescriptor ORDER_DIR = new PropertyDescriptor.Builder().name("file.OrderByPath")
				.displayName("Data Path").description("The path of order by file").required(true)
				.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
	
		public static final Relationship REL_FAILURE = new Relationship.Builder().name("Failure")
				.description("Fail operation").build();
	
		public static final Relationship REL_NEW_COLUMN = new Relationship.Builder().name("Create Column")
				.description("Create column operation").build();
	
		public static final Relationship REL_SUCCESS = new Relationship.Builder().name("Success")
				.description("Success operation").build();
	
		private List<PropertyDescriptor> descriptors;
		private Set<Relationship> relationships;
	
		@Override
		protected void init(final ProcessorInitializationContext context) {
			descriptors = new ArrayList<>();
			descriptors.add(FILE_SEPARATOR);
			descriptors.add(DATA_DIR);
			descriptors.add(ORDER_DIR);
			descriptors = Collections.unmodifiableList(descriptors);
	
			relationships = new HashSet<>();
			relationships.add(REL_FAILURE);
			relationships.add(REL_SUCCESS);
			relationships.add(REL_NEW_COLUMN);
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
			FlowFile lvFlowFile = session.get();
			if (lvFlowFile == null) {
				return;
			}
			getStructureFromFlowFile(context, session, lvFlowFile);
			getStructureFromServer(context, session, lvFlowFile);
	
			try {
				for (Map.Entry<String, String> _Entry : mvFlowLines.entrySet()) {
					if (mvServerLines.containsKey(_Entry.getKey())) {
						if (!mvServerLines.get(_Entry.getKey()).equals(_Entry.getValue())) {
							String[] lvServerLineParam = mvServerLines.get(_Entry.getKey()).split(";");
							String[] lvFlowLineParam = _Entry.getValue().split(";");
							if (!lvServerLineParam[0].equals(lvFlowLineParam[0])) {
								// Сообщение об изменении типов данных
								createFailureRelation(session, lvFlowFile,
										"The database table structure does not match the table structure in the file. The data type for column "
												+ _Entry.getKey() + " has changed from " + lvServerLineParam[0] + " to "
												+ lvFlowLineParam[0]);
								clean();
								return;
							} else {
								// Сообщение об изменении ключевых полей
								createFailureRelation(session, lvFlowFile,
										"The database table structure does not match the table structure in the file. The key field setting for column "
												+ _Entry.getKey() + " has changed.");
								clean();
								return;
							}
						}
					} else if (!_Entry.getKey().equals("TEDO_UPDATED")) {
						createFailureRelation(session, lvFlowFile,
								"The database table structure does not match the table structure in the file. There is no column with the given name: "
										+ _Entry.getKey());
						clean();
						return;
					}
				}
	
				if (mvServerLines.size() > (mvFlowLines.size() - 1)) {
					for (Map.Entry<String, String> _Entry : mvServerLines.entrySet()) {
						if (!mvFlowLines.containsKey(_Entry.getKey())) {
							FlowFile lvNewFlowFile = session.create();
							session.putAllAttributes(lvNewFlowFile, lvFlowFile.getAttributes());
							session.putAttribute(lvNewFlowFile, "table.NewColumn", "yes");
							session.putAttribute(lvNewFlowFile, "table.NewColumnName", _Entry.getKey());
							session.putAttribute(lvNewFlowFile, "table.NewColumnType",
									_Entry.getValue().substring(0, _Entry.getValue().indexOf(";")));
							session.transfer(lvNewFlowFile, REL_NEW_COLUMN);
						}
					}
					session.remove(lvFlowFile);
				} else {
					session.putAttribute(lvFlowFile, "Structure", getStucture(mvFlowLines));
					session.transfer(lvFlowFile, REL_SUCCESS);
				}
				clean();
			} catch (Exception _Exception) {
				createFailureRelation(session, lvFlowFile, "Processor: CompareStructure. " + _Exception.getMessage());
				clean();
				return;
			}
		}
	
		private String getStucture(LinkedHashMap<String, String> mvFlowLines) {
			StringBuilder structureBuilder = new StringBuilder();
	
			for (Map.Entry<String, String> entry : mvFlowLines.entrySet()) {
				String columnName = entry.getKey();
				String columnDetails = entry.getValue();
	
				String[] detailsParts = columnDetails.split(";");
				String columnType = detailsParts[0];
	
				structureBuilder.append(columnName).append(" ").append(columnType).append(", ");
			}
	
			// Удаление последней запятой и пробела
			if (structureBuilder.length() > 0) {
				structureBuilder.setLength(structureBuilder.length() - 2);
			}
	
			return structureBuilder.toString();
		}
	
		private void getStructureFromServer(final ProcessContext avContext, final ProcessSession avSession,
											FlowFile avFlowFile) {
			try {
				BufferedReader lvReader = new BufferedReader(new FileReader(
						avContext.getProperty(DATA_DIR).getValue() + avFlowFile.getAttribute("file.TransferedTime") + "/"
								+ avFlowFile.getAttribute("file.TableName") + ".csv"));
				String _Line = lvReader.readLine();
				_Line = lvReader.readLine();
				while (_Line != null) {
					String[] lvParameters = _Line.split(avContext.getProperty(FILE_SEPARATOR).getValue());
					String lvParameterType = getDataType(lvParameters[7]);
					String mappedKeyField = mapKeyField(lvParameters[0], avContext, avSession, avFlowFile);
					mvServerLines.put(lvParameters[0], lvParameterType + ";" + mappedKeyField);
					_Line = lvReader.readLine();
				}
				lvReader.close();
			} catch (Exception _Exception) {
				createFailureRelation(avSession, avFlowFile,
						"Processor: CompareStructure. Error while working with a file from the server. "
								+ _Exception.getMessage());
				clean();
				return;
			}
		}
	
	
		private void getStructureFromFlowFile(final ProcessContext avContext, final ProcessSession avSession,
				FlowFile avFlowFile) {
			try {
				ByteArrayOutputStream lvBytes = new ByteArrayOutputStream();
				avSession.exportTo(avFlowFile, lvBytes);
				String lvContent = new String(lvBytes.toByteArray());
				String[] lvNames = new String[0];
				if (lvContent.contains("\r\n")) {
					lvNames = lvContent.split("\r\n");
				} else {
					lvNames = lvContent.split("\n");
				}
				lvBytes.close();
				for (int i = 1; i < lvNames.length; ++i) {
					String lvColumnName = lvNames[i];
					mvFlowLines.put(lvColumnName.substring(0, lvColumnName.indexOf(";")),
							lvColumnName.substring(lvColumnName.indexOf(";") + 1));
				}
			} catch (Exception _Exception) {
				createFailureRelation(avSession, avFlowFile,
						"Processor: CompareStructure. Error while working with a flowfile. " + _Exception.getMessage());
				clean();
				return;
			}
		}
	
		private String getDataType(String avParameter) {
			switch (avParameter.toUpperCase()) {
			case ("T"):
				return "DateTime";
			case ("D"):
				return "Date32";
			case ("F"):
			case ("P"):
				return "Float64";
			default:
				return "String";
			}
		}
	
		private String mapKeyField(String keyField, final ProcessContext avContext, final ProcessSession avSession,
				FlowFile avFlowFile) {
			List<String> orderByFields = getOrderByFields(avContext, avFlowFile.getAttribute("file.TableName"));
			if (orderByFields.contains(keyField)) {
				return "1";
			}
			return "0";
		}
	
		private List<String> getOrderByFields(final ProcessContext avContext, String tableName) {
			List<String> orderByFields = new ArrayList<>();
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				JsonNode rootNode = objectMapper
						.readTree(Files.readAllBytes(Paths.get(avContext.getProperty(ORDER_DIR).getValue())));
				for (JsonNode tableNode : rootNode) {
					if (tableNode.path("TABLE").asText().equals(tableName)) {
						String[] fields = tableNode.path("ORDER BY").asText().split(", ");
						Collections.addAll(orderByFields, fields);
						break;
					}
				}
			} catch (IOException e) {
				getLogger().error("Failed to read order by fields from JSON file due to {}",
						new Object[] { e.toString(), e });
			}
			return orderByFields;
		}
	
		private static void createFailureRelation(final ProcessSession avSession, FlowFile avFlowFile, String avMessage) {
			avFlowFile = avSession.putAttribute(avFlowFile, "file.Message", avMessage);
			avSession.transfer(avFlowFile, REL_FAILURE);
		}
	
		public static void clean() {
			mvFlowLines.clear();
			mvServerLines.clear();
		}
	}