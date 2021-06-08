/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingsTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplates;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;


public class FunctionUtils {
    private static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    private static final String INVALID_JAVA_VERSION = "Invalid java version %s, using default value java 8";
    private static final String UNSUPPORTED_JAVA_VERSION = "Unsupported java version %s, using default value java 8";
    private static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    private static final String LOAD_BINDING_TEMPLATES_FAIL = "Failed to load function binding template.";
    private static final String INVALID_FUNCTION_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION is empty or invalid, " +
            "please check the configuration";

    public static JavaVersion parseJavaVersion(String javaVersion) {
        if (StringUtils.isEmpty(javaVersion)) {
            return DEFAULT_JAVA_VERSION;
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        try {
            final int version = Integer.parseInt(javaVersion);
            switch (version) {
                case 8:
                    return JavaVersion.JAVA_8_NEWEST;
                case 11:
                    return JavaVersion.JAVA_11;
                default:
                    messager.warning(String.format(UNSUPPORTED_JAVA_VERSION, version));
                    return DEFAULT_JAVA_VERSION;
            }
        } catch (NumberFormatException e) {
            messager.warning(String.format(INVALID_JAVA_VERSION, javaVersion));
            return DEFAULT_JAVA_VERSION;
        }
    }

    // Workaround for SDK will throw exception when get nonexistent function slot
    public static FunctionDeploymentSlot getFunctionDeploymentSlotByName(FunctionApp functionApp, String slotName) {
        try {
            return functionApp.deploymentSlots().getByName(slotName);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static FunctionExtensionVersion parseFunctionExtensionVersion(String version) throws AzureExecutionException {
        return Arrays.stream(FunctionExtensionVersion.values())
                .filter(versionEnum -> StringUtils.equalsIgnoreCase(versionEnum.getVersion(), version))
                .findFirst()
                .orElseThrow(() -> new AzureExecutionException(INVALID_FUNCTION_EXTENSION_VERSION));
    }

    public static BindingTemplate loadBindingTemplate(String type) {
        try (final InputStream is = FunctionUtils.class.getResourceAsStream("/bindings.json")) {
            final String bindingsJsonStr = IOUtils.toString(is, StandardCharsets.UTF_8);
            final BindingsTemplate bindingsTemplate = new ObjectMapper()
                    .readValue(bindingsJsonStr, BindingsTemplate.class);
            return bindingsTemplate.getBindingTemplateByName(type);
        } catch (IOException e) {
            AzureMessager.getMessager().warning(LOAD_BINDING_TEMPLATES_FAIL);
            // Add task should work without Binding Template, just return null if binding load fail
            return null;
        }
    }

    public static List<FunctionTemplate> loadAllFunctionTemplates() throws AzureExecutionException {
        try (final InputStream is = FunctionUtils.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtils.toString(is, StandardCharsets.UTF_8);
            return parseTemplateJson(templatesJsonStr);
        } catch (Exception e) {
            AzureMessager.getMessager().error(LOAD_TEMPLATES_FAIL);
            throw new AzureExecutionException(LOAD_TEMPLATES_FAIL, e);
        }
    }

    private static List<FunctionTemplate> parseTemplateJson(final String templateJson) throws IOException {
        final FunctionTemplates templates = new ObjectMapper().readValue(templateJson, FunctionTemplates.class);
        return templates.getTemplates();
    }
}
