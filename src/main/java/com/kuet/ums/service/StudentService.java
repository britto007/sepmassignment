package com.kuet.ums.service;

import com.kuet.ums.dto.StudentRequest;
import com.kuet.ums.dto.StudentResponse;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.model.Role;
import com.kuet.ums.model.Student;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordPolicy passwordPolicy;

    public StudentService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            AccountDirectory accountDirectory,
            PasswordPolicy passwordPolicy) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accountDirectory = accountDirectory;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> list() {
        return studentRepository.findAll().stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        return StudentResponse.from(find(id));
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        String studentId = request.getStudentId().trim();
        accountDirectory.ensureUsernameAvailable(username, Role.STUDENT, null);
        ensureEmailAvailable(email, null);
        ensureStudentIdAvailable(studentId, null);

        Student student = new Student();
        student.setFullName(request.getFullName().trim());
        student.setEmail(email);
        student.setUsername(username);
        student.setStudentId(studentId);
        student.setDepartment(request.getDepartment().trim());
        student.setBatch(request.getBatch().trim());
        student.setPassword(passwordPolicy.encodeRequired(request.getPassword()));
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = find(id);
        AccountAccess.requireOwnerOrAdmin(student.getUsername());
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        String studentId = request.getStudentId().trim();
        accountDirectory.ensureUsernameAvailable(username, Role.STUDENT, id);
        ensureEmailAvailable(email, id);
        ensureStudentIdAvailable(studentId, id);

        student.setFullName(request.getFullName().trim());
        student.setEmail(email);
        student.setUsername(username);
        student.setStudentId(studentId);
        student.setDepartment(request.getDepartment().trim());
        student.setBatch(request.getBatch().trim());
        student.setPassword(passwordPolicy.encodeOptional(request.getPassword(), student.getPassword()));
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        Student student = find(id);
        AccountAccess.requireAdminToDelete();
        rejectSelfDelete(student.getUsername());
        enrollmentRepository.deleteByStudentId(student.getId());
        studentRepository.delete(student);
    }

    private Student find(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Student not found."));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        studentRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Email is already in use.");
            }
        });
    }

    private void ensureStudentIdAvailable(String studentId, Long currentId) {
        studentRepository.findByStudentIdIgnoreCase(studentId).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Student ID is already in use.");
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
