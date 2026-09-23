package com.ridelink.account_service.repository;

import com.ridelink.account_service.entity.AccountStatus;
import com.ridelink.account_service.entity.Role;
import com.ridelink.account_service.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByStatus(AccountStatus status);

    List<User> findByRoleAndStatus(Role role, AccountStatus status);
}