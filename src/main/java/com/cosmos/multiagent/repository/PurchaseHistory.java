// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.format.annotation.DateTimeFormat;

@Container
public class PurchaseHistory {
    String id;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDateOfPurchase() {
        return dateOfPurchase;
    }

    public void setDateOfPurchase(String dateOfPurchase) {
        this.dateOfPurchase = dateOfPurchase;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    @PartitionKey
    String userId;
    // date of purchase
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    String dateOfPurchase;

    public String getDateOfRefund() {
        return dateOfRefund;
    }

    public void setDateOfRefund(String dateOfRefund) {
        this.dateOfRefund = dateOfRefund;
    }

    String dateOfRefund;
    String itemId;
    String amount;
}
