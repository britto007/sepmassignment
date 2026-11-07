package com.kuet.ums.service;

import com.kuet.ums.dto.TeacherRequest;
import com.kuet.ums.dto.TeacherResponse;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.model.Role;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final AccountDirectory accountDirectory;
    private final PasswordPolicy passwordPolicy;

    public TeacherService(
            TeacherRepository teacherRepository,
            CourseRepository courseRepository,
            AccountDirectory accountDirectory,
            PasswordPolicy passwordPolicy) {
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.accountDirectory = accountDirectory;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> list() {
        return teacherRepository.findAll().stream().map(TeacherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(Long id) {
        return TeacherResponse.from(find(id));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        accountDirectory.ensureUsernameAvailable(username, Role.TEACHER, null);
        ensureEmailAvailable(email, null);

        Teacher teacher = new Teacher();
        teacher.setFullName(request.getFullName().trim());
        teacher.setEmail(email);
        teacher.setUsername(username);
        teacher.setDepartment(request.getDepartment().trim());
        teacher.setDesignation(request.getDesignation().trim());
        teacher.setPassword(passwordPolicy.encodeRequired(request.getPassword()));
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = find(id);
        AccountAccess.requireOwnerOrAdmin(teacher.getUsername());
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        accountDirectory.ensureUsernameAvailable(username, Role.TEACHER, id);
        ensureEmailAvailable(email, id);

        teacher.setFullName(request.getFullName().trim());
        teacher.setEmail(email);
        teacher.setUsername(username);
        teacher.setDepartment(request.getDepartment().trim());
        teacher.setDesignation(request.getDesignation().trim());
        teacher.setPassword(passwordPolicy.encodeOptional(request.getPassword(), teacher.getPassword()));
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(Long id) {
        Teacher teacher = find(id);
        AccountAccess.requireAdminToDelete();
        rejectSelfDelete(teacher.getUsername());
        if (courseRepository.existsByTeacherId(teacher.getId())) {
            throw new ConflictException("Remove this teacher's courses before deleting the account.");
        }
        teacherRepository.delete(teacher);
    }

    private Teacher find(Long id) {
        return teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("Teacher not found."));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        teacherRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
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
