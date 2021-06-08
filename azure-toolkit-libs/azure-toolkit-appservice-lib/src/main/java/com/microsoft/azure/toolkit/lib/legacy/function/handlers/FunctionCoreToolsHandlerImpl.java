/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.github.zafarkhaja.semver.Version;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.CommandUtils;

import java.io.File;

public class FunctionCoreToolsHandlerImpl implements FunctionCoreToolsHandler {

    public static final String FUNC_EXTENSIONS_INSTALL_TEMPLATE = "func extensions install -c \"%s\" --java";
    public static final String INSTALL_FUNCTION_EXTENSIONS_FAIL = "Failed to install the Function extensions";
    public static final String CANNOT_AUTO_INSTALL = "Local Azure Functions Core Tools does not " +
            "exist or is too old to support function extension installation, skip package phase." +
            " To install or update it, see: https://aka.ms/azfunc-install";
    public static final String NEED_UPDATE_FUNCTION_CORE_TOOLS = "Local version of Azure Functions Core Tools (%s) " +
            "does not match the latest (%s). Please update it for the best experience. " +
            "See: https://aka.ms/azfunc-install";
    public static final String GET_LATEST_VERSION_CMD = "npm view azure-functions-core-tools dist-tags.core";
    public static final String GET_LATEST_VERSION_FAIL = "Failed to check update for Azure Functions Core Tools";
    public static final String GET_LOCAL_VERSION_CMD = "func --version";
    public static final String GET_LOCAL_VERSION_FAIL = "Failed to get Azure Functions Core Tools version locally";
    public static final Version LEAST_SUPPORTED_VERSION = Version.valueOf("2.0.1-beta.26");

    private final CommandHandler commandHandler;

    public FunctionCoreToolsHandlerImpl(final CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void installExtension(File stagingDirectory, File basedir) throws AzureExecutionException {
        assureRequirementAddressed();
        installFunctionExtension(stagingDirectory, basedir);
    }

    protected void installFunctionExtension(File stagingDirector, File basedir) throws AzureExecutionException {
        commandHandler.runCommandWithReturnCodeCheck(
                String.format(FUNC_EXTENSIONS_INSTALL_TEMPLATE, basedir.getAbsolutePath()),
                true,
                stagingDirector.getAbsolutePath(),
                CommandUtils.getDefaultValidReturnCodes(),
                INSTALL_FUNCTION_EXTENSIONS_FAIL
        );
    }

    protected void assureRequirementAddressed() throws AzureExecutionException {
        final String localVersion = getLocalFunctionCoreToolsVersion();
        final String latestCoreVersion = getLatestFunctionCoreToolsVersion();
        // Ensure azure function core tools has been installed and support extension auto-install
        if (localVersion == null || LEAST_SUPPORTED_VERSION.greaterThan(Version.valueOf(localVersion))) {
            throw new AzureExecutionException(CANNOT_AUTO_INSTALL);
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        // Verify whether local function core tools is the latest version
        if (latestCoreVersion == null) {
            messager.warning(GET_LATEST_VERSION_FAIL);
        } else if (Version.valueOf(localVersion).lessThan(Version.valueOf(latestCoreVersion))) {
            messager.warning(String.format(NEED_UPDATE_FUNCTION_CORE_TOOLS, localVersion, latestCoreVersion));
        }
    }

    protected String getLatestFunctionCoreToolsVersion() {
        try {
            final String latestCoreVersion = commandHandler.runCommandAndGetOutput(
                    GET_LATEST_VERSION_CMD,
                    false, /* showStdout */
                    null /* workingDirectory */
            );
            Version.valueOf(latestCoreVersion);
            return latestCoreVersion;
        } catch (Exception e) {
            AzureMessager.getMessager().warning(GET_LATEST_VERSION_FAIL);
            return null;
        }
    }

    protected String getLocalFunctionCoreToolsVersion() {
        try {
            final String localVersion = commandHandler.runCommandAndGetOutput(
                    GET_LOCAL_VERSION_CMD,
                    false, /* showStdout */
                    null /* workingDirectory */
            );
            Version.valueOf(localVersion);
            return localVersion;
        } catch (Exception e) {
            AzureMessager.getMessager().warning(GET_LOCAL_VERSION_FAIL);
            return null;
        }
    }
}
