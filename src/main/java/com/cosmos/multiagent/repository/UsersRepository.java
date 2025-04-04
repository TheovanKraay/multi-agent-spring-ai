package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends CosmosRepository<Users, String> {
    Optional<Users> findByUserId(Integer userId);
}
