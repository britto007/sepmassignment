package com.kuet.ums.service;

import com.kuet.ums.model.Role;

public record StoredAccount(Long id, String username, String password, Role role) {
}
