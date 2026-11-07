package com.kuet.ums.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CourseRequest {

    @NotBlank(message = "Course code is required.")
    @Size(min = 2, max = 20, message = "Course code must be between 2 and 20 characters.")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Course code can contain letters, digits, dots, underscores, and hyphens.")
    private String code;

    @NotBlank(message = "Title is required.")
    @Size(max = 160, message = "Title must be at most 160 characters.")
    private String title;

    @NotBlank(message = "Department is required.")
    @Size(max = 100, message = "Department must be at most 100 characters.")
    private String department;

    @NotNull(message = "Credit hours are required.")
    @Min(value = 1, message = "Credit hours must be at least 1.")
    @Max(value = 6, message = "Credit hours must be at most 6.")
    private Integer creditHours;

    private Long teacherId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(Integer creditHours) {
        this.creditHours = creditHours;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }
}
