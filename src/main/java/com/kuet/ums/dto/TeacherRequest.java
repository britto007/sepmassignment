package com.kuet.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Password is required when creating an account.
 * On update, a blank password leaves the current password unchanged.
 */
public class TeacherRequest {

    @NotBlank(message = "Full name is required.")
    @Size(max = 120, message = "Full name must be at most 120 characters.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 180, message = "Email must be at most 180 characters.")
    private String email;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can contain letters, digits, dots, underscores, and hyphens.")
    private String username;

    @Size(max = 72, message = "Password must be at most 72 characters.")
    private String password;

    @NotBlank(message = "Department is required.")
    @Size(max = 100, message = "Department must be at most 100 characters.")
    private String department;

    @NotBlank(message = "Designation is required.")
    @Size(max = 100, message = "Designation must be at most 100 characters.")
    private String designation;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
