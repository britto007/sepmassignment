package com.kuet.ums.service;

import com.kuet.ums.dto.StudentRequest;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.model.Student;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AccountDirectory accountDirectory;

    @Mock
    private PasswordPolicy passwordPolicy;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createRejectsAStudentIdAlreadyUsedByAnotherStudent() {
        StudentRequest request = new StudentRequest();
        request.setFullName("Sam Student");
        request.setEmail("sam@kuet.ac.bd");
        request.setUsername("sam.student");
        request.setStudentId("2024001");
        request.setDepartment("CSE");
        request.setBatch("2024");
        request.setPassword("secret12");
        Student existingStudent = new Student();
        existingStudent.setId(2L);
        when(studentRepository.findByEmailIgnoreCase("sam@kuet.ac.bd")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentIdIgnoreCase("2024001")).thenReturn(Optional.of(existingStudent));

        assertThrows(ConflictException.class, () -> studentService.create(request));

        verify(studentRepository, never()).save(any(Student.class));
    }
}
