package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseHistoryRepository extends CosmosRepository<PurchaseHistory, String> {
    List<PurchaseHistory> findByUserId(String userId);
    List<PurchaseHistory> findByItemId(String itemId);
}
