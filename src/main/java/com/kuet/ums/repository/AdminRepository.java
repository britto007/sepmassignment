package com.kuet.ums.repository;

import com.kuet.ums.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsernameIgnoreCase(String username);

    Optional<Admin> findByEmailIgnoreCase(String email);
}
