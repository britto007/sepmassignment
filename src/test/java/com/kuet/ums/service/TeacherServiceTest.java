package com.kuet.ums.service;

import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AccountDirectory accountDirectory;

    @Mock
    private PasswordPolicy passwordPolicy;

    @InjectMocks
    private TeacherService teacherService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteRejectsTeacherWhoStillHasCourses() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "password", "ROLE_ADMIN"));
        Teacher teacher = new Teacher();
        teacher.setId(7L);
        teacher.setUsername("teacher");
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(teacher));
        when(courseRepository.existsByTeacherId(7L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> teacherService.delete(7L));

        verify(teacherRepository, never()).delete(teacher);
    }
}
