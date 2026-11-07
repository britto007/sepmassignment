package com.kuet.ums.service;

import com.kuet.ums.exception.ForbiddenException;

public final class AccountAccess {

    private AccountAccess() {
    }

    public static void requireAdminToDelete() {
        if (!CurrentUser.isAdmin()) {
            throw new ForbiddenException("Only an admin can delete accounts.");
        }
    }

    public static void requireOwnerOrAdmin(String username) {
        if (CurrentUser.isAdmin()) {
            return;
        }
        if (username != null && username.equalsIgnoreCase(CurrentUser.username())) {
            return;
        }
        throw new ForbiddenException("You can only edit your own account.");
    }
}
