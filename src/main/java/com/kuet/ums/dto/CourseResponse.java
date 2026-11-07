package com.kuet.ums.dto;

import java.util.List;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String department,
        int creditHours,
        Long teacherId,
        String teacherName,
        String teacherUsername,
        int enrolledCount,
        boolean enrolled,
        List<EnrollmentResponse> students) {
}
