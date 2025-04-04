package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseHistoryRepository extends CosmosRepository<PurchaseHistory, String> {
    // Custom query methods can be defined here if needed
    // For example, to find purchase history by userId:
    List<PurchaseHistory> findByUserId(String userId);
    List<PurchaseHistory> findByItemId(String itemId);
}
