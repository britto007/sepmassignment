package com.kuet.ums.dto;

import com.kuet.ums.model.Admin;

public record AdminResponse(Long id, String fullName, String email, String username) {

    public static AdminResponse from(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getFullName(), admin.getEmail(), admin.getUsername());
    }
}
