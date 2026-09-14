package com.placementtracker.repository;

import com.placementtracker.User;
import com.placementtracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByCompanyIdAndRole(Long companyId, Role role);
}
