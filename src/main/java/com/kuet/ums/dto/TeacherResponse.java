package com.kuet.ums.dto;

import com.kuet.ums.model.Teacher;

public record TeacherResponse(
        Long id,
        String fullName,
        String email,
        String username,
        String department,
        String designation) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getUsername(),
                teacher.getDepartment(),
                teacher.getDesignation());
    }
}
