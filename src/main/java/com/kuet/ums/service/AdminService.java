package com.kuet.ums.service;

import com.kuet.ums.dto.AdminRequest;
import com.kuet.ums.dto.AdminResponse;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.model.Admin;
import com.kuet.ums.model.Role;
import com.kuet.ums.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordPolicy passwordPolicy;

    public AdminService(
            AdminRepository adminRepository,
            AccountDirectory accountDirectory,
            PasswordPolicy passwordPolicy) {
        this.adminRepository = adminRepository;
        this.accountDirectory = accountDirectory;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> list() {
        return adminRepository.findAll().stream().map(AdminResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminResponse get(Long id) {
        return AdminResponse.from(find(id));
    }

    @Transactional
    public AdminResponse create(AdminRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        accountDirectory.ensureUsernameAvailable(username, Role.ADMIN, null);
        ensureEmailAvailable(email, null);

        Admin admin = new Admin();
        admin.setFullName(request.getFullName().trim());
        admin.setEmail(email);
        admin.setUsername(username);
        admin.setPassword(passwordPolicy.encodeRequired(request.getPassword()));
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional
    public AdminResponse update(Long id, AdminRequest request) {
        Admin admin = find(id);
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        accountDirectory.ensureUsernameAvailable(username, Role.ADMIN, id);
        ensureEmailAvailable(email, id);

        admin.setFullName(request.getFullName().trim());
        admin.setEmail(email);
        admin.setUsername(username);
        admin.setPassword(passwordPolicy.encodeOptional(request.getPassword(), admin.getPassword()));
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional
    public void delete(Long id) {
        Admin admin = find(id);
        rejectSelfDelete(admin.getUsername());
        adminRepository.delete(admin);
    }

    private Admin find(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new NotFoundException("Admin not found."));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        adminRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Email is already in use.");
            }
        });
    }

    private void rejectSelfDelete(String username) {
        if (username.equalsIgnoreCase(CurrentUser.username())) {
            throw new ConflictException("You cannot delete the account you are logged in with.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
