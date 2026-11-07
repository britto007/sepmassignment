package com.kuet.ums.service;

import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.model.Role;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.AdminRepository;
import com.kuet.ums.repository.StudentRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDirectoryTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private AccountDirectory accountDirectory;

    @Test
    void usernameUsedByTeacherCannotBeRegisteredAsAdmin() {
        Teacher teacher = new Teacher();
        teacher.setId(3L);
        teacher.setUsername("shared.user");
        teacher.setPassword("encoded-password");
        when(adminRepository.findByUsernameIgnoreCase("shared.user")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsernameIgnoreCase("shared.user")).thenReturn(Optional.of(teacher));

        assertThrows(ConflictException.class,
                () -> accountDirectory.ensureUsernameAvailable("shared.user", Role.ADMIN, null));

        verify(studentRepository, never()).findByUsernameIgnoreCase("shared.user");
    }
}
