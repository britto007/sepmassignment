package com.kuet.ums.repository;

import com.kuet.ums.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUsernameIgnoreCase(String username);

    Optional<Student> findByEmailIgnoreCase(String email);

    Optional<Student> findByStudentIdIgnoreCase(String studentId);
}
