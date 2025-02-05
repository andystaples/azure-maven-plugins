/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.entity;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServicePlanEntity implements IAzureResourceEntity {
    private final String subscriptionId;
    private final String id;
    private final String name;
    private final String region;
    private final String resourceGroup;
    private final PricingTier pricingTier;
    private final OperatingSystem operatingSystem;
}
