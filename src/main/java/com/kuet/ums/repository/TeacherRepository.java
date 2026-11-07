package com.kuet.ums.repository;

import com.kuet.ums.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUsernameIgnoreCase(String username);

    Optional<Teacher> findByEmailIgnoreCase(String email);
}
