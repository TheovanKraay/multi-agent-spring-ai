package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends CosmosRepository<Products, String> {
    @Query("SELECT * FROM Products p WHERE p.metadata.productId = @productId OFFSET 0 LIMIT 1")
    List<Products> findByProductId(String productId);
}
