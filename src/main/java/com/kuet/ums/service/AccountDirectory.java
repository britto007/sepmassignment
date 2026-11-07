package com.kuet.ums.service;

import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.model.Role;
import com.kuet.ums.repository.AdminRepository;
import com.kuet.ums.repository.StudentRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountDirectory {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public AccountDirectory(
            AdminRepository adminRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    public Optional<StoredAccount> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        Optional<StoredAccount> admin = adminRepository.findByUsernameIgnoreCase(username)
                .map(account -> new StoredAccount(account.getId(), account.getUsername(), account.getPassword(), Role.ADMIN));
        if (admin.isPresent()) {
            return admin;
        }
        Optional<StoredAccount> teacher = teacherRepository.findByUsernameIgnoreCase(username)
                .map(account -> new StoredAccount(account.getId(), account.getUsername(), account.getPassword(), Role.TEACHER));
        if (teacher.isPresent()) {
            return teacher;
        }
        return studentRepository.findByUsernameIgnoreCase(username)
                .map(account -> new StoredAccount(account.getId(), account.getUsername(), account.getPassword(), Role.STUDENT));
    }

    public void ensureUsernameAvailable(String username, Role role, Long currentId) {
        findByUsername(username).ifPresent(existing -> {
            boolean sameAccount = existing.role() == role && existing.id().equals(currentId);
            if (!sameAccount) {
                throw new ConflictException("Username is already in use.");
            }
        });
    }
}
