package com.glt.idm.rest.controllers;/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import static com.evolveum.midpoint.security.api.RestAuthorizationAction.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

import static org.springframework.http.ResponseEntity.status;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import javax.xml.namespace.QName;

import java.io.IOException;

import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.glt.idm.rest.utils.GltUtilites;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.springframework.core.io.UrlResource;


import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;

import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.RestHandlerMethod;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;

import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin
@RestController
@RequestMapping( "/api/glt")
public class GltRestController extends GltAbstractRestController {
    private RepositoryService repositoryService;


    public static final String GET_OBJECT_PATH = "/{type}/{id}";

    private static final String CURRENT = "current";
    private static final long WAIT_FOR_TASK_STOP = 2000L;

        @Autowired AuditService auditService;
    @Autowired private ModelCrudService model;
    @Autowired private ModelDiagnosticService modelDiagnosticService;
    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private ModelService modelService;
    @Autowired private BulkActionsService bulkActionsService;
    @Autowired private TaskService taskService;
    @Autowired private CaseService caseService;
    @Autowired protected AccessCertificationService certificationService;

    @Autowired private ModelAuditService modelAuditService;

    @Autowired protected CertificationManagerImpl certificationManager;

    @Autowired protected SchemaService schemaService;

    private static final int MAX_DEPTH = 2;


    @RestHandlerMethod(authorization = GENERATE_VALUE)
    @PostMapping("/{type}/{oid}/generate")
    public ResponseEntity<?> generateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "generateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = generateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RPC_GENERATE_VALUE)
    @PostMapping("/rpc/generate")
    public ResponseEntity<?> generateValueRpc(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("generateValueRpc");

        ResponseEntity<?> response = generateValue(null, policyItemsDefinition, task, result);
        finishRequest(task, result);

        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> generateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult parentResult) {

        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
        } else {
            try {
                modelInteraction.generateValue(object, policyItemsDefinition, task, parentResult);
                parentResult.computeStatusIfUnknown();
                if (parentResult.isSuccess()) {
                    response = createResponse(HttpStatus.OK, policyItemsDefinition, parentResult, true);
                } else {
                    response = createResponse(HttpStatus.BAD_REQUEST, parentResult, parentResult);
                }

            } catch (Exception ex) {
                parentResult.recordFatalError("Failed to generate value, " + ex.getMessage(), ex);
                response = handleException(parentResult, ex);
            }
        }
        return response;
    }

    @RestHandlerMethod(authorization = VALIDATE_VALUE)
    @PostMapping("/{type}/{oid}/validate")
    public ResponseEntity<?> validateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("validateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = validateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RPC_VALIDATE_VALUE)
    @PostMapping("/rpc/validate")
    public ResponseEntity<?> validateValue(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("validateValue");

        ResponseEntity<?> response = validateValue(null, policyItemsDefinition, task, result);
        finishRequest(task, result);
        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> validateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult result) {
        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", result);
            finishRequest(task, result);
            return response;
        }

        if (CollectionUtils.isEmpty(policyItemsDefinition.getPolicyItemDefinition())) {
            response = createBadPolicyItemsDefinitionResponse("No definitions for items", result);
            finishRequest(task, result);
            return response;
        }

        try {
            modelInteraction.validateValue(object, policyItemsDefinition, task, result);

            result.computeStatusIfUnknown();
            if (result.isAcceptable()) {
                response = createResponse(HttpStatus.OK, policyItemsDefinition, result, true);
            } else {
                response = status(HttpStatus.CONFLICT).body(result);
            }

        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        return response;
    }

    private ResponseEntity<?> createBadPolicyItemsDefinitionResponse(
            String message, OperationResult parentResult) {
        logger.error(message);
        parentResult.recordFatalError(message);
        return status(HttpStatus.BAD_REQUEST).body(parentResult);
    }

    @RestHandlerMethod(authorization = GET_VALUE_POLICY)
    @GetMapping("/users/{id}/policy")
    public ResponseEntity<?> getValuePolicyForUser(
            @PathVariable("id") String oid) {
        logger.debug("getValuePolicyForUser start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getValuePolicyForUser");

        ResponseEntity<?> response;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());
            PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, result);

            CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(user, task, result);

            response = createResponse(HttpStatus.OK, policy, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);

        logger.debug("getValuePolicyForUser finish");
        return response;
    }

    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping(GET_OBJECT_PATH)
    public ResponseEntity<?> getObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        logger.debug("model rest service for get operation start");

        Task task = initRequest();
        OperationResult result = createSubresult(task, "getObject");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Collection<SelectorOptions<GetOperationOptions>> getOptions =
                GetOperationOptions.fromRestOptions(options, include, exclude,
                        resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object;
            if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
                object = getCurrentNodeObject(getOptions, task, result);
            } else {
                object = model.getObject(clazz, id, getOptions, task, result);
            }
            removeExcludes(object, exclude); // temporary measure until fixed in repo

            response = createResponse(HttpStatus.OK, object, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    private PrismObject<? extends ObjectType> getCurrentNodeObject(Collection<SelectorOptions<GetOperationOptions>> getOptions,
                                                                   Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        String nodeId = taskManager.getNodeId();
        ObjectQuery query = prismContext.queryFor(NodeType.class)
                .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                .build();
        List<PrismObject<NodeType>> objects = model.searchObjects(NodeType.class, query, getOptions, task, result);
        if (objects.isEmpty()) {
            throw new ObjectNotFoundException("Current node (id " + nodeId + ") couldn't be found.", NodeType.class, nodeId);
        } else if (objects.size() > 1) {
            throw new IllegalStateException("More than one 'current' node (id " + nodeId + ") found.");
        } else {
            return objects.get(0);
        }
    }

    @RestHandlerMethod(authorization = GET_SELF)
    @GetMapping("/self/")
    public ResponseEntity<?> getSelfAlt() {
        return getSelf();
    }

    @RestHandlerMethod(authorization = GET_SELF)
    @GetMapping("/self")
    public ResponseEntity<?> getSelf() {
        logger.debug("model rest service for get operation start");
        Task task = initRequest();
        OperationResult result = createSubresult(task, "self");
        ResponseEntity<?> response;

        try {
            String loggedInUserOid = SecurityUtil.getPrincipalOidIfAuthenticated();
            PrismObject<UserType> user = model.getObject(UserType.class, loggedInUserOid, null, task, result);
            response = createResponse(HttpStatus.OK, user, result, true);
            result.recordSuccessIfUnknown();
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(logger, e);
            response = status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PostMapping("/{type}/")
    public <T extends ObjectType> ResponseEntity<?> addObjectAlt(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        return addObject(type, options, object);
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PostMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> addObject(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("addObject");

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        if (object.getCompileTimeClass() == null || !object.getCompileTimeClass().equals(clazz)) {
            String simpleName = object.getCompileTimeClass() != null ? object.getCompileTimeClass().getSimpleName() : null;
            result.recordFatalError("Request to add object of type " + simpleName + " to the collection of " + type);
            finishRequest(task, result);
            return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

        String oid;
        ResponseEntity<?> response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, result);
            logger.debug("returned oid: {}", oid);

            if (oid != null) {
                response = createResponseWithLocation(
                        clazz.isAssignableFrom(TaskType.class) ? HttpStatus.ACCEPTED : HttpStatus.CREATED,
                        uriGetObject(type, oid),
                        result);
            } else {
                // OID might be null e.g. if the object creation is a subject of workflow approval
                response = createResponse(HttpStatus.ACCEPTED, result);
            }
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    public @NotNull URI uriGetObject(@PathVariable("type") String type, String oid) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(controllerBasePath() + GET_OBJECT_PATH)
                .build(type, oid);
    }

    @RestHandlerMethod(authorization = GET_OBJECTS)
    @GetMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> searchObjectsByType(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjectsByType");

        //noinspection unchecked
        Class<T> clazz = (Class<T>) ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {

            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

            List<PrismObject<T>> objects = modelService.searchObjects(clazz, null, searchOptions, task, result);
            ObjectListType listType = new ObjectListType();
            for (PrismObject<T> object : objects) {
                listType.getObject().add(object.asObjectable());
            }

            response = createResponse(HttpStatus.OK, listType, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PutMapping("/{type}/{id}")
    public <T extends ObjectType> ResponseEntity<?> addObject(
            @PathVariable("type") String type,
            // TODO is it OK that this is not used or at least asserted?
            @SuppressWarnings("unused") @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("addObject");

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        Class<T> objectClazz = Objects.requireNonNull(object.getCompileTimeClass());
        if (!clazz.equals(objectClazz)) {
            finishRequest(task, result);
            result.recordFatalError(
                    "Request to add object of type %s to the collection of %s".formatted(objectClazz.getSimpleName(), type));
            return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
        if (modelExecuteOptions == null) {
            modelExecuteOptions = ModelExecuteOptions.create();
        }
        // TODO: Do we want to overwrite in any case? Because of PUT?
        //  This was original logic... and then there's that ignored ID.
        modelExecuteOptions.overwrite();

        String oid;
        ResponseEntity<?> response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, result);
            logger.debug("returned oid : {}", oid);

            response = createResponseWithLocation(
                    clazz.isAssignableFrom(TaskType.class) ? HttpStatus.ACCEPTED : HttpStatus.CREATED,
                    uriGetObject(type, oid),
                    result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }
        result.computeStatus();

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = DELETE_OBJECT)
    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> deleteObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options) {

        logger.debug("model rest service for delete operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("deleteObject");

        ResponseEntity<?> response;
        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        if (ObjectType.class.equals(clazz)) {
            result.recordFatalError("Type object (path /objects/) does not support deletion, use concrete type.");
            response = createResponse(HttpStatus.METHOD_NOT_ALLOWED, result);
        } else {
            try {
                if (clazz.isAssignableFrom(TaskType.class)) {
                    taskService.suspendAndDeleteTask(id, WAIT_FOR_TASK_STOP, true, task, result);
                    result.computeStatus();
                    finishRequest(task, result);
                    if (result.isSuccess()) {
                        return ResponseEntity.noContent().build();
                    }
                    return status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(result.getMessage());
                }

                ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

                model.deleteObject(clazz, id, modelExecuteOptions, task, result);
                response = createResponse(HttpStatus.NO_CONTENT, result);
            } catch (Exception ex) {
                response = handleException(result, ex);
            }
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPost(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {
        return modifyObjectPatch(type, oid, options, modificationType);
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/cases/{oid}/workItems/{id}/complete")
    public ResponseEntity<?> completeWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId,
            @RequestBody AbstractWorkItemOutputType output
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("completeWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.completeWorkItem(id, output, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/cases/{oid}/workItems/{id}/delegate")
    public ResponseEntity<?> delegateWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId,
            @RequestBody WorkItemDelegationRequestType delegationRequest
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("delegateWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.delegateWorkItem(id, delegationRequest, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/cases/{oid}/workItems/{id}/claim")
    public ResponseEntity<?> claimWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("claimWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.claimWorkItem(id, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/cases/{oid}/workItems/{id}/release")
    public ResponseEntity<?> releaseWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("releaseWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.releaseWorkItem(id, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/cases/{oid}/cancel")
    public ResponseEntity<?> cancelCase(
            @PathVariable("oid") String caseOid
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("cancelCase");

        ResponseEntity<?> response;
        try {
            caseService.cancelCase(caseOid, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PatchMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPatch(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {

        logger.debug("model rest service for modify operation start");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("modifyObjectPatch");
        ResponseEntity<?> response;
        if (ObjectType.class.equals(clazz)) {
            result.recordFatalError("Type 'object' (path /objects/) does not support modifications, use concrete type.");
            response = createResponse(HttpStatus.METHOD_NOT_ALLOWED, result);
        } else {
            try {
                ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
                Collection<? extends ItemDelta<?, ?>> modifications = DeltaConvertor.toModifications(modificationType, clazz);
                model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, result);
                response = createResponse(HttpStatus.NO_CONTENT, result);
            } catch (Exception ex) {
                result.recordFatalError("Could not modify object. " + ex.getMessage(), ex);
                response = handleException(result, ex);
            }
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = NOTIFY_CHANGE)
    @PostMapping("/notifyChange")
    public ResponseEntity<?> notifyChange(
            @RequestBody ResourceObjectShadowChangeDescriptionType changeDescription) {
        logger.debug("model rest service for notify change operation start");
        Validate.notNull(changeDescription, "Chnage description must not be null");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("notifyChange");

        ResponseEntity<?> response;
        try {
            modelService.notifyChange(changeDescription, task, result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = FIND_SHADOW_OWNER)
    @GetMapping("/shadows/{oid}/owner")
    public ResponseEntity<?> findShadowOwner(
            @PathVariable("oid") String shadowOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("findShadowOwner");

        ResponseEntity<?> response;
        try {
            PrismObject<? extends FocusType> focus = modelService.searchShadowOwner(shadowOid, null, task, result);
            response = createResponse(HttpStatus.OK, focus, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = IMPORT_SHADOW)
    @PostMapping("/shadows/{oid}/import")
    public ResponseEntity<?> importShadow(
            @PathVariable("oid") String shadowOid) {
        logger.debug("model rest service for import shadow from resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("importShadow");

        ResponseEntity<?> response;
        try {
            modelService.importFromResource(shadowOid, task, result);

            response = createResponse(HttpStatus.OK, result, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @PostMapping("/{type}/search")
    public ResponseEntity<?> searchObjects(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
            @RequestBody QueryType queryType) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);

            ObjectListType listType = new ObjectListType();
            for (PrismObject<? extends ObjectType> o : objects) {

                removeExcludes(o, exclude);        // temporary measure until fixed in repo
                listType.getObject().add(o.asObjectable());
            }

            response = createResponse(HttpStatus.OK, listType, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude)
            throws SchemaException {
        object.getValue().removePaths(
                ItemPathCollectionsUtil.pathListFromStrings(exclude, prismContext));
    }

    @RestHandlerMethod(authorization = IMPORT_FROM_RESOURCE)
    @PostMapping("/resources/{resourceOid}/import/{objectClass}")
    public ResponseEntity<?> importFromResource(
            @PathVariable("resourceOid") String resourceOid,
            @PathVariable("objectClass") String objectClass) {
        logger.debug("model rest service for import from resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("importFromResource");

        QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
        ResponseEntity<?> response;
        try {
            modelService.importFromResource(resourceOid, objClass, task, result);
            response = createResponseWithLocation(
                    HttpStatus.SEE_OTHER,
                    uriGetObject(ObjectTypes.TASK.getRestType(), task.getOid()),
                    result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = TEST_RESOURCE)
    @PostMapping("/resources/{resourceOid}/test")
    public ResponseEntity<?> testResource(
            @PathVariable("resourceOid") String resourceOid) {
        logger.debug("model rest service for test resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("testResource");

        ResponseEntity<?> response;
        OperationResult testResult = null;
        try {
            testResult = modelService.testResource(resourceOid, task, result);
            response = createResponse(HttpStatus.OK, testResult, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        if (testResult != null) {
            result.getSubresults().add(testResult);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = SUSPEND_TASK)
    @PostMapping("/tasks/{oid}/suspend")
    public ResponseEntity<?> suspendTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("suspendTask");

        ResponseEntity<?> response;
        try {
            taskService.suspendTask(taskOid, WAIT_FOR_TASK_STOP, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RESUME_TASK)
    @PostMapping("/tasks/{oid}/resume")
    public ResponseEntity<?> resumeTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("resumeTask");

        ResponseEntity<?> response;
        try {
            taskService.resumeTask(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.ACCEPTED, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("tasks/{oid}/run")
    public ResponseEntity<?> scheduleTaskNow(
            @PathVariable("oid") String taskOid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("scheduleTaskNow");

        ResponseEntity<?> response;
        try {
            taskService.scheduleTaskNow(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = EXECUTE_SCRIPT)
    @PostMapping("/rpc/executeScript")
    public ResponseEntity<?> executeScript(
            @RequestParam(value = "asynchronous", required = false) Boolean asynchronous,
            @RequestBody ExecuteScriptType command) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeScript");

        ResponseEntity<?> response;
        try {
            if (Boolean.TRUE.equals(asynchronous)) {
                var taskOid = modelInteraction.submitScriptingExpression(command, task, result);
                response = createResponseWithLocation(
                        HttpStatus.CREATED,
                        uriGetObject(ObjectTypes.TASK.getRestType(), taskOid),
                        result);
            } else {
                BulkActionExecutionResult executionResult = bulkActionsService.executeBulkAction(
                        // detached because of REST origin
                        ExecuteScriptConfigItem.of(command, ConfigurationItemOrigin.rest()),
                        VariablesMap.emptyMap(),
                        BulkActionExecutionOptions.create(),
                        task,
                        result);
                ExecuteScriptResponseType responseData = new ExecuteScriptResponseType()
                        .result(result.createOperationResultType())
                        .output(new ExecuteScriptOutputType()
                                .consoleOutput(executionResult.getConsoleOutput())
                                .dataOutput(PipelineData.prepareXmlData(executionResult.getDataOutput(), command.getOptions())));
                response = createResponse(HttpStatus.OK, responseData, result);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Couldn't execute script.", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = COMPARE_OBJECT)
    @PostMapping("/rpc/compare")
//    @Consumes({ "application/xml" }) TODO do we need to limit it to XML?
    public <T extends ObjectType> ResponseEntity<?> compare(
            @RequestParam(value = "readOptions", required = false) List<String> restReadOptions,
            @RequestParam(value = "compareOptions", required = false) List<String> restCompareOptions,
            @RequestParam(value = "ignoreItems", required = false) List<String> restIgnoreItems,
            @RequestBody PrismObject<T> clientObject) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("compare");

        ResponseEntity<?> response;
        try {
            List<ItemPath> ignoreItemPaths = ItemPathCollectionsUtil.pathListFromStrings(restIgnoreItems, prismContext);
            final GetOperationOptions getOpOptions = GetOperationOptions.fromRestOptions(restReadOptions, DefinitionProcessingOption.ONLY_IF_EXISTS);
            Collection<SelectorOptions<GetOperationOptions>> readOptions =
                    getOpOptions != null ? SelectorOptions.createCollection(getOpOptions) : null;
            ModelCompareOptions compareOptions = ModelCompareOptions.fromRestOptions(restCompareOptions);
            CompareResultType compareResult = modelService.compareObject(clientObject, readOptions, compareOptions, ignoreItemPaths, task, result);

            response = createResponse(HttpStatus.OK, compareResult, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_LOG_SIZE)
    @GetMapping(value = "/log/size",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLogFileSize() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLogFileSize");

        ResponseEntity<?> response;
        try {
            long size = modelDiagnosticService.getLogFileSize(task, result);
            response = createResponse(HttpStatus.OK, String.valueOf(size), result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_LOG)
    @GetMapping(value = "/log",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLog(
            @RequestParam(value = "fromPosition", required = false) Long fromPosition,
            @RequestParam(value = "maxSize", required = false) Long maxSize) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLog");

        ResponseEntity<?> response;
        try {
            LogFileContentType content = modelDiagnosticService.getLogFileContent(fromPosition, maxSize, task, result);

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            builder.header("ReturnedDataPosition", String.valueOf(content.getAt()));
            builder.header("ReturnedDataComplete", String.valueOf(content.isComplete()));
            builder.header("CurrentLogFileSize", String.valueOf(content.getLogFileSize()));
            response = builder.body(content.getContent());
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get log file content: fromPosition={}, maxSize={}", ex, fromPosition, maxSize);
            response = handleExceptionNoLog(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RESET_CREDENTIAL)
    @PostMapping("/users/{oid}/credential")
    public ResponseEntity<?> executeCredentialReset(
            @PathVariable("oid") String oid,
            @RequestBody ExecuteCredentialResetRequestType executeCredentialResetRequest) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeCredentialReset");

        ResponseEntity<?> response;
        try {
            PrismObject<UserType> user = modelService.getObject(UserType.class, oid, null, task, result);

            ExecuteCredentialResetResponseType executeCredentialResetResponse = modelInteraction.executeCredentialsReset(user, executeCredentialResetRequest, task, result);
            response = createResponse(HttpStatus.OK, executeCredentialResetResponse, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;

    }

    @RestHandlerMethod(authorization = GET_THREADS)
    @GetMapping(value = "/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getThreadsDump() {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getThreadsDump(task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get threads dump", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_TASKS_THREADS)
    @GetMapping(value = "/tasks/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getRunningTasksThreadsDump() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getRunningTasksThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getRunningTasksThreadsDump(task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get running tasks threads dump", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_TASK_THREADS)
    @GetMapping(value = "/tasks/{oid}/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getTaskThreadsDump(
            @PathVariable("oid") String oid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getTaskThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getTaskThreadsDump(oid, task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get task threads dump for task " + oid, ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }












    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @GetMapping("/getReport/{reportId}")
    public ResponseEntity<?> searchObjectsReport(
            @PathVariable("reportId") String reportId) throws SchemaException {


        String filePath="ok" ;
        Resource resource = null;

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"query\": {\"filter\": {\"inOid\":{\"value\":\""+reportId+"\"}}}}");

        String jsonQuery = queryBuilder.toString();
        QueryType queryType =  prismContext.parserFor(jsonQuery).parseRealValue();


        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");
        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType("reportData");
        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(null, null,
                    null, null, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);


            Collection<Item<?, ?>> items = objects.get(0).getValue().getItems();

            for (Item<?, ?> item : items) {
                // String itemDisplay =  item.getDisplayName();
                // String itemDValue =  item.getValue().toString();
                // String itemPath=  item.getPath().toString();

                if (item.getPath().toString().equals("filePath")){
                    PrismPropertyValue itemVal = (PrismPropertyValue)item.getValue();
                    filePath = itemVal.getValue().toString();
                }
            }

            try {

                Path restFilePath = Paths.get(filePath);
                resource = new UrlResource(restFilePath.toUri());

            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }


        } catch (Exception ex) {
            return handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);

        if (resource == null) {
            return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
        }
        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";
        HttpHeaders httpHeaders=new HttpHeaders();



        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);

    }



    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @GetMapping("/getReportCustomFormat/{reportId}/{format}")
    public ResponseEntity<?> searchObjectsReport(
            @PathVariable("reportId") String reportId, @PathVariable("format") String format) throws SchemaException, IOException {


        String filePath="ok" ;
        Resource resource = null;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"query\": {\"filter\": {\"inOid\":{\"value\":\""+reportId+"\"}}}}");

        String jsonQuery = queryBuilder.toString();
        QueryType queryType =  prismContext.parserFor(jsonQuery).parseRealValue();


        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");
        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType("reportData");
        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(null, null,
                    null, null, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);


            Collection<Item<?, ?>> items = objects.get(0).getValue().getItems();

            for (Item<?, ?> item : items) {
                if (item.getPath().toString().equals("filePath")){
                    PrismPropertyValue itemVal = (PrismPropertyValue)item.getValue();
                    filePath = itemVal.getValue().toString();
                }
            }

            if (format.equals("xls")) {
                String fullNameAndPathXlsFile = filePath.replaceAll(".csv",".xls");
                filePath=fullNameAndPathXlsFile;
            }

            //   if (format.equals("csv")) {}

            try {

                Path restFilePath = Paths.get(filePath);
                resource = new UrlResource(restFilePath.toUri());

            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }


        } catch (Exception ex) {
            return handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);

        if (resource == null) {
            return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
        }
        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";
        HttpHeaders httpHeaders = new HttpHeaders();

//        builder.header("ReturnedDataPosition", String.valueOf(content.getAt()));
//        builder.header("ReturnedDataComplete", String.valueOf(content.isComplete()));
//        builder.header("CurrentLogFileSize", String.valueOf(content.getLogFileSize()));
//        response = builder.body(content.getContent());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .header(HttpHeaders.CONTENT_LENGTH,Long.toString(resource.contentLength()))
                .body(resource);

    }
//му

    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping("/enum/{nameType}")
    public ResponseEntity<?> getEnumArray(
            @PathVariable("nameType") String nameType) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getEnumArray");

        ResponseEntity<?> response;
        try {

            String[] array = {};

            switch (nameType) {
                case  ("OperationResultStatusType"):
                    array = getEnumValue(OperationResultStatusType.class);
                    break;
                case  ("AuditEventTypeType"):
                    array = getEnumValue(AuditEventTypeType.class);
                    break;
                case  ("AuditEventStageType"):
                    array = getEnumValue(AuditEventStageType.class);
                    break;
                case  ("SynchronizationSituationType"):
                    array = getEnumValue(SynchronizationSituationType.class);
                    break;
                case  ("ShadowKindType"):
                    array = getEnumValue(ShadowKindType.class);
                    break;
                case  ("ActivationStatusType"):
                    array = getEnumValue(ActivationStatusType.class);
                    break;

            }

            response = ResponseEntity.ok(array);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get array to type - " + nameType, ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }




    @RestHandlerMethod(authorization = GET_SELF)
    @PostMapping("/import/objects/{format}")
    public ResponseEntity<?> importObjects(
            @PathVariable("format") String format,
            @RequestPart("file") MultipartFile file) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("importObjects");
        ResponseEntity<?> response;

        try {

            ImportOptionsType importOptions = new ImportOptionsType();
            byte[] bytes = file.getBytes();
            InputStream myInputStream = new ByteArrayInputStream(bytes);
            modelService.importObjectsFromStream(myInputStream, format, importOptions,task, result);

            response = createResponse(HttpStatus.OK, "ok", result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }


        @RestHandlerMethod(authorization = SEARCH_OBJECTS)
        @PostMapping("/object/search/{type}")
        public ResponseEntity<?> gltSearchObjects(
                @PathVariable("type") String type,
                @RequestParam(value = "options", required = false) List<String> options,
                @RequestParam(value = "include", required = false) List<String> include,
                @RequestParam(value = "exclude", required = false) List<String> exclude,
                @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
                @RequestBody QueryType queryType) {

            Task task = initRequest();
            OperationResult result = task.getResult().createSubresult("searchObjects");

            Class<? extends Containerable> clazz = getClassFromType(type);
            ResponseEntity<?> response;
            try {
                ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
                Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                        exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

                List<? extends Containerable> objects =
                        modelService.searchContainers(clazz, query, searchOptions, task, result);

                response = createResponse(HttpStatus.OK, objects, result, true);
            } catch (Exception ex) {
                response = handleException(result, ex);
            }

            result.computeStatus();
            return response;
        }


    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping("/object/{type}/{id}")
    public ResponseEntity<?> getAnyObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        logger.debug("model rest service for get operation start");

        return gltSearchObjects(type, options, include, exclude, resolveNames, getQueryTypeFilter(id));
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PostMapping("/object/add/{type}")
    public <T extends Objectable> ResponseEntity<?> addAnyObject(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> prismObjectObjectable) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("addObject");

        T objectable = prismObjectObjectable.asObjectable();
        if (!(objectable instanceof ObjectType)) {
            String message = "Cannot process type " + objectable.getClass() + " as it is not a subtype of " + ObjectType.class;
            logger.error("Import of object {} failed: {}", prismObjectObjectable, message);
            return handleException(result, new Throwable(message));
        }
        //noinspection unchecked
        PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) prismObjectObjectable;

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

        String oid;
        ResponseEntity<?> response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, result);
            logger.debug("returned oid: {}", oid);

            if (oid != null) {
                response = gltSearchObjects(type, options, null, null, null, getQueryTypeFilter(oid));
            } else {
                // OID might be null e.g. if the object creation is a subject of workflow approval
                response = createResponse(HttpStatus.ACCEPTED, result);
            }
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/object/modify/{type}/{oid}")
    public <T extends Objectable> ResponseEntity<?> modifyAnyObject(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {

        logger.debug("model rest service for modify operation start");

        Task task = initRequest();
        OperationResult parentResult = task.getResult().createSubresult("modifyObjectPatch");

        Class<? extends Objectable> clazz = getClassFromObjectableType(type);

        ResponseEntity<?> response;

        try {
            ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
            Collection<? extends ItemDelta<?, ?>> modifications = DeltaConvertor.toModifications(modificationType, clazz);
            ItemDeltaCollectionsUtil.checkConsistence(modifications, ConsistencyCheckScope.THOROUGH);
            Validate.notNull(modifications, "Object modification must not be null.");

            OperationResult result = parentResult.createSubresult("modifyObject");
            result.addArbitraryObjectCollectionAsParam("modifications", modifications);

            ObjectDelta<? extends Objectable> objectDelta = prismContext.deltaFactory().object().createModifyDelta(oid, modifications, clazz);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            modelService.executeChanges(deltas, modelExecuteOptions, task, result);

//                model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, result);
            response = createResponse(HttpStatus.NO_CONTENT, parentResult);
        } catch (Exception ex) {
            parentResult.recordFatalError("Could not modify object. " + ex.getMessage(), ex);
            response = handleException(parentResult, ex);
        }
        parentResult.computeStatus();
        finishRequest(task, parentResult);
        return response;
    }

    @RestHandlerMethod(authorization = DELETE_OBJECT)
    @DeleteMapping("/object/del/{type}/{oid}")
    public ResponseEntity<?> deleteAnyObject(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options) {

        logger.debug("model rest service for delete operation start");

        Task task = initRequest();
        OperationResult parentResult = task.getResult().createSubresult("deleteObject");
        Class<? extends Objectable> clazz = getClassFromObjectableType(type);

        ResponseEntity<?> response;


        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

        OperationResult result = parentResult.createSubresult("deleteObject");
        result.addParam(OperationResult.PARAM_OID, oid);

//        RepositoryCache.enterLocalCaches(cacheConfigurationManager);

        try {
            ObjectDelta<? extends Objectable> objectDelta = prismContext.deltaFactory().object().create(clazz, ChangeType.DELETE);
            objectDelta.setOid(oid);

            logger.debug("Deleting object with oid {}.", oid);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            modelService.executeChanges(deltas, modelExecuteOptions, task, result);

            result.recordSuccess();

            response = createResponse(HttpStatus.NO_CONTENT, parentResult);
        } catch (Exception ex) {
            response = handleException(parentResult, ex);
        }
        parentResult.computeStatus();
        finishRequest(task, parentResult);
        return response;
    }

    private QueryType getQueryTypeFilter(String id) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"query\": {\"filter\": {\"inOid\":{\"value\":\"" + id + "\"}}}}");

        String jsonQuery = queryBuilder.toString();
        QueryType queryType = null;
        try {
            queryType = prismContext.parserFor(jsonQuery).parseRealValue();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return queryType;
    }



    private static Class<? extends Objectable> getClassFromObjectableType(String nameType) {
        switch (nameType) {
            case ("AbstractRoleType"): return AbstractRoleType.class;
            case ("AccessCertificationCampaignType"): return AccessCertificationCampaignType.class;
//            case ("AccessCertificationCaseType"): return AccessCertificationCaseType.class;
            case ("AccessCertificationDefinitionType"): return AccessCertificationDefinitionType.class;
//            case ("AccessCertificationWorkItemType"): return AccessCertificationWorkItemType.class;
            case ("ArchetypeType"): return ArchetypeType.class;
            case ("AssignmentHolderType"): return AssignmentHolderType.class;
//            case ("AssignmentType"): return AssignmentType.class;
            case ("CaseType"): return CaseType.class;
//            case ("CaseWorkItemType"): return CaseWorkItemType.class;
            case ("ConnectorHostType"): return ConnectorHostType.class;
            case ("ConnectorType"): return ConnectorType.class;
            case ("DashboardType"): return DashboardType.class;
//            case ("FocusIdentityType"): return FocusIdentityType.class;
            case ("FocusType"): return FocusType.class;
            case ("FormType"): return FormType.class;
            case ("FunctionLibraryType"): return FunctionLibraryType.class;
            case ("GenericObjectType"): return GenericObjectType.class;
            case ("LookupTableType"): return LookupTableType.class;
            case ("MarkType"): return MarkType.class;
            case ("MessageTemplateType"): return MessageTemplateType.class;
            case ("NodeType"): return NodeType.class;
            case ("ObjectCollectionType"): return ObjectCollectionType.class;
            case ("ObjectTemplateType"): return ObjectTemplateType.class;
            case ("ObjectType"): return ObjectType.class;
//            case ("OperationExecutionType"): return OperationExecutionType.class;
            case ("OrgType"): return OrgType.class;
            case ("ReportDataType"): return ReportDataType.class;
            case ("ReportType"): return ReportType.class;
            case ("ResourceType"): return ResourceType.class;
            case ("RoleAnalysisClusterType"): return RoleAnalysisClusterType.class;
            case ("RoleAnalysisSessionType"): return RoleAnalysisSessionType.class;
            case ("RoleType"): return RoleType.class;
            case ("SecurityPolicyType"): return SecurityPolicyType.class;
            case ("SequenceType"): return SequenceType.class;
            case ("ServiceType"): return ServiceType.class;
            case ("ShadowType"): return ShadowType.class;
//            case ("SimulationResultProcessedObjectType"): return SimulationResultProcessedObjectType.class;
            case ("SimulationResultType"): return SimulationResultType.class;
            case ("SystemConfigurationType"): return SystemConfigurationType.class;
            case ("TaskType"): return TaskType.class;
//            case ("TriggerType"): return TriggerType.class;
            case ("UserType"): return UserType.class;
            case ("ValuePolicyType"): return ValuePolicyType.class;
        }
        throw new IllegalArgumentException("Not suitable class found for rest type: " + nameType);
    }



    private static Class<? extends Containerable> getClassFromType(String nameType) {
        switch (nameType) {
            case ("AbstractRoleType"): return AbstractRoleType.class;
            case ("AccessCertificationCampaignType"): return AccessCertificationCampaignType.class;
            case ("AccessCertificationCaseType"): return AccessCertificationCaseType.class;
            case ("AccessCertificationDefinitionType"): return AccessCertificationDefinitionType.class;
            case ("AccessCertificationWorkItemType"): return AccessCertificationWorkItemType.class;
            case ("ArchetypeType"): return ArchetypeType.class;
            case ("AssignmentHolderType"): return AssignmentHolderType.class;
            case ("AssignmentType"): return AssignmentType.class;
            case ("CaseType"): return CaseType.class;
            case ("CaseWorkItemType"): return CaseWorkItemType.class;
            case ("ConnectorHostType"): return ConnectorHostType.class;
            case ("ConnectorType"): return ConnectorType.class;
            case ("DashboardType"): return DashboardType.class;
            case ("FocusIdentityType"): return FocusIdentityType.class;
            case ("FocusType"): return FocusType.class;
            case ("FormType"): return FormType.class;
            case ("FunctionLibraryType"): return FunctionLibraryType.class;
            case ("GenericObjectType"): return GenericObjectType.class;
            case ("LookupTableType"): return LookupTableType.class;
            case ("MarkType"): return MarkType.class;
            case ("MessageTemplateType"): return MessageTemplateType.class;
            case ("NodeType"): return NodeType.class;
            case ("ObjectCollectionType"): return ObjectCollectionType.class;
            case ("ObjectTemplateType"): return ObjectTemplateType.class;
            case ("ObjectType"): return ObjectType.class;
            case ("OperationExecutionType"): return OperationExecutionType.class;
            case ("OrgType"): return OrgType.class;
            case ("ReportDataType"): return ReportDataType.class;
            case ("ReportType"): return ReportType.class;
            case ("ResourceType"): return ResourceType.class;
            case ("RoleAnalysisClusterType"): return RoleAnalysisClusterType.class;
            case ("RoleAnalysisSessionType"): return RoleAnalysisSessionType.class;
            case ("RoleType"): return RoleType.class;
            case ("SecurityPolicyType"): return SecurityPolicyType.class;
            case ("SequenceType"): return SequenceType.class;
            case ("ServiceType"): return ServiceType.class;
            case ("ShadowType"): return ShadowType.class;
            case ("SimulationResultProcessedObjectType"): return SimulationResultProcessedObjectType.class;
            case ("SimulationResultType"): return SimulationResultType.class;
            case ("SystemConfigurationType"): return SystemConfigurationType.class;
            case ("TaskType"): return TaskType.class;
            case ("TriggerType"): return TriggerType.class;
            case ("UserType"): return UserType.class;
            case ("ValuePolicyType"): return ValuePolicyType.class;

        }
        throw new IllegalArgumentException("Not suitable class found for rest type: " + nameType);
    }

    private static String[] getEnumValue(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", ");
    }



    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @PostMapping("/audit/search")
    public ResponseEntity<?> searchAuditObjects(
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
            @RequestBody QueryType queryType) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchAuditObjects");

        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(AuditEventRecordType.class, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

            List<AuditEventRecordType> records = modelAuditService.searchObjects(query, searchOptions, task, result);

            response = createResponse(HttpStatus.OK, records, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }




    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{defId}/run")
    public ResponseEntity<?> campaignTaskNow(
            @PathVariable("defId") String defId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("campaignTaskNow");
        Task task2 = initRequest();
        OperationResult result2 = task.getResult().createSubresult("campaigStart");
        ResponseEntity<?> response;
        try {
            AccessCertificationCampaignType campaign =
                    certificationManager.createCampaign(defId,task,result);
            certificationManager.openNextStage(campaign.getOid(),task2,result2);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/closeCampaign")
    public ResponseEntity<?> campaignTaskClose(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("campaignTaskClose");

        ResponseEntity<?> response;
        try {

            certificationService.closeCampaign(campaignId,task,result);
            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }


    @RestHandlerMethod(authorization = GET_VALUE_POLICY)
    @GetMapping("/certificationService/{id}/getStatus")
    public ResponseEntity<?> getStatusCert(
            @PathVariable("id") String oid) {
        logger.debug("getStatusCert start");
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getStatusCert");

        ResponseEntity<?> response;
        try {

            AccessCertificationCasesStatisticsType stat =
                    certificationManager.getCampaignStatistics(oid, true, task, result);

            response = createResponse(HttpStatus.OK, stat, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);

        logger.debug("getValuePolicyForUser finish");
        return response;
    }








    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/openNextStage")
    public ResponseEntity<?> compOpenNextStage(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("compOpenNextStage");

        ResponseEntity<?> response;
        try {

            certificationService.openNextStage(campaignId,task,result);
            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/closeCurrentStage")
    public ResponseEntity<?>  certificationCloseCurrentStage(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("certificationCloseCurrentStage");

        ResponseEntity<?> response;
        try {

            certificationService.closeCurrentStage(campaignId,task,result);
            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/reiterateCampaign")
    public ResponseEntity<?> reiterateCampaign(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("reiterateCampaign");

        ResponseEntity<?> response;
        try {

            certificationService.reiterateCampaign(campaignId,task,result);
            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/startRemediation")
    public ResponseEntity<?> startRemediation(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("startRemediation");

        ResponseEntity<?> response;
        try {

            certificationService.startRemediation(campaignId,task,result);
            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }



    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("/certificationService/{campaignId}/closeCompain")
    public ResponseEntity<?> closeCompain(
            @PathVariable("campaignId") String campaignId) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("closeCompain");

        ResponseEntity<?> response;
        try {
            certificationService.closeCampaign(campaignId,task,result);

            result.computeStatus();

            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }



    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/certificationService/{campaignoid}/{caseoid}/workItems/{id}/complete")
    public ResponseEntity<?> completeWorkItemCertification(
            @PathVariable("campaignoid") String campaignoid,
            @PathVariable("caseoid") Long caseoid,
            @PathVariable("id") Long workItemId,
            @RequestBody AbstractWorkItemOutputType output
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("completeWorkItem");

        ResponseEntity<?> response;
        try {
            /*
             * Records a particular decision of a reviewer.
             *
             * @param campaignOid OID of the campaign to which the decision belongs.
             * @param caseId ID of the certification case to which the decision belongs.
             * @param workItemId ID of the work item to which the decision belongs.
             * @param response The response.
             * @param comment Reviewer's comment.
             * @param task Task in context of which all operations will take place.
             * @param parentResult Result for the operations.
             */



            if (output.getOutcome().equalsIgnoreCase("ACCEPT")) {
                certificationService.recordDecision(campaignoid, caseoid, workItemId, ACCEPT, output.getComment(), task, result);
            }
            if (output.getOutcome().equalsIgnoreCase("REVOKE")) {
                certificationService.recordDecision(campaignoid, caseoid, workItemId, REVOKE, output.getComment(), task, result);
            }
            if (output.getOutcome().equalsIgnoreCase("REDUCE")) {
                certificationService.recordDecision(campaignoid, caseoid, workItemId, REDUCE, output.getComment(), task, result);
            }

            if (output.getOutcome().equalsIgnoreCase("NOT_DECIDED")) {
                certificationService.recordDecision(campaignoid, caseoid, workItemId, NOT_DECIDED, output.getComment(), task, result);
            }

            if (output.getOutcome().equalsIgnoreCase("NO_RESPONSE")) {
                certificationService.recordDecision(campaignoid, caseoid, workItemId, NO_RESPONSE, output.getComment(), task, result);
            }


            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }



    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping(value = "/certificationService/User/{oid}/getCompain",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getTaskCompainglt(
            @PathVariable("oid") String oid) throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getTaskCompainglt");

        ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                .item(new QName(SchemaConstants.NS_C, "ownerRef")).ref(oid)
                .build();

        List<? extends Containerable> objects   = modelService.searchContainers(
                AccessCertificationCampaignType.class, query, null, task, result);

        ResponseEntity<?> response;
        try {

            //response = createResponse(HttpStatus.OK, caseList, result, true);
            response = createResponse(HttpStatus.OK, objects, result, true);
        } catch (Exception ex) {

            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }



    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping(value = "/certificationService/Compain/{oid}/getCase",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getCaseForCompain(
            @PathVariable("oid") String oid) throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getCaseForCompain");

        Collection<SelectorOptions<GetOperationOptions>> options =
                schemaService.getOperationOptionsBuilder().item(F_CASE).retrieve().build();
        AccessCertificationCampaignType campaign = modelService.getObject(AccessCertificationCampaignType.class, oid, options, task, result).asObjectable();

        // List<AccessCertificationCaseType> caseList = campaign.getCase();

        ResponseEntity<?> response;
        try {

            //response = createResponse(HttpStatus.OK, caseList, result, true);
            response = createResponse(HttpStatus.OK, campaign, result, true);
        } catch (Exception ex) {

            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }



    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @GetMapping(value = "/translate/{lang}",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })


    public ResponseEntity<?> searchObjectslookup(
            @PathVariable("lang") String lang) throws SchemaException {

        List<String> include  = new ArrayList<String>();
        include.add(0,"row");
        String language = "Language_"+lang;
        String json="";
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\"query\":{\"filter\":{\"equal\":{\"path\":\"name\",\"value\":\"Language_ru\"}}}}");

        String jsonQuery = queryBuilder.toString();
        QueryType queryType =  prismContext.parserFor(jsonQuery).parseRealValue();
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType("lookupTables");
        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(null, include,
                    null, null, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);
            HashMap<String, HashMap<String, String>> lookupMap = new HashMap();
            ObjectListType listType = new ObjectListType();
            for (PrismObject<? extends ObjectType> o : objects) {

                PrismObjectValue<? extends ObjectType> my = o.getValue();

                for (Item<?, ?> item : my.getItems()) {
                    String blockName=item.getElementName().getLocalPart();
                    if (blockName.equalsIgnoreCase("row")){
                        for (PrismValue value : item.getValues()) {
                            String k1 = "";
                            String k2 ="";
                            String label ="";
                            if (value instanceof PrismContainerValue) {
                                PrismContainerValue<?> container = (PrismContainerValue<?>) value;
                                for (Item<?, ?> item2 : container.getItems()) {
                                    QName itemName = item2.getElementName();
                                    Object realValue = item2.getRealValue();
                                    // System.out.println("Item " + itemName + " = " + realValue);
                                    String lvalue=realValue.toString();
                                    String kvalue=itemName.toString();

                                    String[] lines = lvalue.split("\\.");


                                    if (kvalue.equalsIgnoreCase("key")) {
                                        k1=lines[0];
                                        k2=lines[1];
                                    }else {
                                        label=lvalue;
                                    }

                                    if (lookupMap.containsKey(k1))
                                    {

                                        HashMap<String, String> eventMap = new HashMap();
                                        eventMap= lookupMap.get(k1);
                                        eventMap.put(k2,label);
                                        lookupMap.put(k1,eventMap);

                                    }else {
                                        HashMap<String, String> eventMap = new HashMap();
                                        eventMap.put(k2,label);
                                        lookupMap.put(k1,eventMap);

                                    }
                                }
                            }
                        }

                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            json = objectMapper.writeValueAsString(lookupMap);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            response = createResponse(HttpStatus.OK, json, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }


    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping(value = "/{type}/fullObjectAttributes",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getObjectAttributes(@PathVariable("type") String type)  {
        GltUtilites utilites = new GltUtilites();
        Task task = initRequest();

        OperationResult result = task.getResult().createSubresult("getObjectAttributes");
        HashMap<String, String> attr=  utilites.getAttributesUserTypeWithJavaTypes(prismContext);

        ResponseEntity<?> response;
        try {

            ObjectMapper objectMapper = new ObjectMapper();
           String json = objectMapper.writeValueAsString(attr);
            response = createResponse(HttpStatus.OK, json, result, true);
        } catch (Exception ex) {

            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_OBJECT)
    @PostMapping(
            value = "/attributes/all-types",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> postTypesAttributes(@RequestBody(required = false) List<String> typeNamesToProcess) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("postTypesAttributes");
        try {
            SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
            Map<String, Object> resultMap = new LinkedHashMap<>();

            List<String> types = (typeNamesToProcess == null) ? Collections.emptyList() : typeNamesToProcess;

            boolean fetchAll = types.isEmpty() ||
                    types.stream().anyMatch(s -> s != null && ("ALL".equalsIgnoreCase(s) || "*".equals(s)));

            if (fetchAll) {
                schemaRegistry.getSchemas().stream()
                        .flatMap(s -> s.getComplexTypeDefinitions().stream())
                        .forEach(ctd -> resultMap.putIfAbsent(
                                ctd.getTypeName().getLocalPart(),
                                getAttributesForType(ctd, schemaRegistry)));
            } else {
                // только запрошенные
                for (String typeName : types) {
                    ComplexTypeDefinition ctd = findComplexTypeDefinition(schemaRegistry, typeName);
                    resultMap.put(typeName, ctd == null ? "Not found" : getAttributesForType(ctd, schemaRegistry));
                }
            }

            return ResponseEntity.ok(resultMap);

        } catch (Exception ex) {
            return handleException(result, ex);
        } finally {
            result.computeStatus();
            finishRequest(task, result);
        }
    }

    private Map<String, String> getAttributesForType(ComplexTypeDefinition ctd, SchemaRegistry registry) {
        Map<String, String> attrs = new LinkedHashMap<>();
        Set<QName> visited = new HashSet<>();
        processDefinitions(ctd.getDefinitions(), "", attrs, registry, visited, 1);
        return attrs;
    }

    private void processDefinitions(Collection<? extends ItemDefinition<?>> defs,
            String prefix, Map<String, String> attrs,
            SchemaRegistry registry, Set<QName> visited, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }

        for (ItemDefinition<?> itemDef : defs) {

            if (itemDef instanceof PrismContainerDefinition<?> containerDef) {
                String name = itemDef.getItemName().getLocalPart();
                String key = prefix.isEmpty() ? name : prefix + "_" + name;
                processDefinitions(
                        containerDef.getDefinitions(),
                        key, attrs, registry, visited,
                        depth + 1
                );
                continue;
            }

            QName fieldType = itemDef.getTypeName();

            String fieldName = itemDef.getItemName().getLocalPart();
            String key = prefix.isEmpty() ? fieldName : prefix + "_" + fieldName;

            TypeDefinition td = registry.findTypeDefinitionByType(fieldType);

            String baseType = capitalize(fieldType.getLocalPart());
            String typeStr;
            if (td instanceof EnumerationTypeDefinition enumDef) {
                String values = enumDef.getValues().stream()
                        .map(EnumerationTypeDefinition.ValueDefinition::getValue)
                        .collect(Collectors.joining(", "));
                typeStr = baseType + " (" + values + ")";
            } else {
                typeStr = baseType;
            }

            String value = itemDef.isMultiValue()
                    ? "List<" + typeStr + ">"
                    : typeStr;
            attrs.put(key, value);

            if (td instanceof ComplexTypeDefinition nested
                    && visited.add(nested.getTypeName())) {
                processDefinitions(
                        nested.getDefinitions(), key, attrs, registry, visited,
                        depth + 1
                );
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private ComplexTypeDefinition findComplexTypeDefinition(SchemaRegistry registry, String localName) {
        return registry.getSchemas().stream()
                .flatMap(s -> s.getComplexTypeDefinitions().stream())
                .filter(ctd -> ctd.getTypeName().getLocalPart().equals(localName))
                .findFirst()
                .orElse(null);
    }

    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping("/audit/test")
    public ResponseEntity<String> testAuditService() {
        if (auditService == null) {
            System.out.println("AuditService is null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AuditService is null");
        } else {
            System.out.println("AuditService is not null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AuditService is not null");
        }


    }



}
