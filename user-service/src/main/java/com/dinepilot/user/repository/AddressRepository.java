package com.dinepilot.user.repository;

import com.dinepilot.user.entity.Address;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends MongoRepository<Address, String> {

    List<Address> findByUserId(String userId);

    Optional<Address> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
