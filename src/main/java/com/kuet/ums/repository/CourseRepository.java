package com.kuet.ums.repository;

import com.kuet.ums.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCodeAsc();

    Optional<Course> findByCodeIgnoreCase(String code);

    boolean existsByTeacherId(Long teacherId);
}
