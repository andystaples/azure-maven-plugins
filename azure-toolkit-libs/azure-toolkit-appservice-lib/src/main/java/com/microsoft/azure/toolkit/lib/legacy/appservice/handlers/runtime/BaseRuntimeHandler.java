/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.RuntimeHandler;

public abstract class BaseRuntimeHandler<T extends WebAppBase> implements RuntimeHandler<T> {

    private static final String TARGET_APP_SERVICE_PLAN_DO_NOT_EXIST = "Target app service plan %s cannot be found in " +
            "resource group %s, please check the configuration";

    protected String appName;
    protected String resourceGroup;
    protected Region region;
    protected PricingTier pricingTier;
    protected String servicePlanName;
    protected String servicePlanResourceGroup;
    protected Azure azure;
    protected String image;
    protected String registryUrl;

    public abstract static class Builder<T extends Builder<T>> {
        protected String appName;
        protected String resourceGroup;
        protected Region region;
        protected PricingTier pricingTier;
        protected String servicePlanName;
        protected String servicePlanResourceGroup;
        protected Azure azure;
        protected String image;
        protected String registryUrl;

        public T appName(final String value) {
            this.appName = value;
            return self();
        }

        public T resourceGroup(final String value) {
            this.resourceGroup = value;
            return self();
        }

        public T region(final Region value) {
            this.region = value;
            return self();
        }

        public T pricingTier(final PricingTier value) {
            this.pricingTier = value;
            return self();
        }

        public T servicePlanName(final String value) {
            this.servicePlanName = value;
            return self();
        }

        public T servicePlanResourceGroup(final String value) {
            this.servicePlanResourceGroup = value;
            return self();
        }

        public T azure(final Azure value) {
            this.azure = value;
            return self();
        }

        public T image(final String value) {
            this.image = value;
            return self();
        }

        public T registryUrl(final String value) {
            this.registryUrl = value;
            return self();
        }

        public abstract BaseRuntimeHandler build();

        protected abstract T self();

    }

    protected BaseRuntimeHandler(Builder<?> builder) {
        this.appName = builder.appName;
        this.resourceGroup = builder.resourceGroup;
        this.region = builder.region;
        this.pricingTier = builder.pricingTier;
        this.servicePlanName = builder.servicePlanName;
        this.servicePlanResourceGroup = builder.servicePlanResourceGroup;
        this.azure = builder.azure;
        this.image = builder.image;
        this.registryUrl = builder.registryUrl;
    }

    public abstract WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException;

    public abstract WebAppBase.Update updateAppRuntime(T app) throws AzureExecutionException;

    protected abstract void changeAppServicePlan(T app, AppServicePlan appServicePlan) throws AzureExecutionException;

    @Override
    public AppServicePlan updateAppServicePlan(T app) throws AzureExecutionException {
        throw new UnsupportedOperationException("Please switch track2 SDK.");
    }

    protected AppServicePlan getAppServicePlan() {
        throw new UnsupportedOperationException("Please switch track2 SDK.");
    }
}
