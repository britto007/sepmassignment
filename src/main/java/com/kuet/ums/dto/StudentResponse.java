package com.kuet.ums.dto;

import com.kuet.ums.model.Student;

public record StudentResponse(
        Long id,
        String fullName,
        String email,
        String username,
        String studentId,
        String department,
        String batch) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getUsername(),
                student.getStudentId(),
                student.getDepartment(),
                student.getBatch());
    }
}
