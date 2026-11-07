package com.kuet.ums.service;

import com.kuet.ums.dto.CourseRequest;
import com.kuet.ums.dto.CourseResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.ForbiddenException;
import com.kuet.ums.model.Course;
import com.kuet.ums.model.Enrollment;
import com.kuet.ums.model.Student;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.StudentRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CourseService courseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCreatesCourseWithTrimmedDetailsAndIsAssignedToIt() {
        authenticate("teacher", "ROLE_TEACHER");
        Teacher teacher = teacher(10L, "teacher", "Taylor Teacher");
        when(courseRepository.findByCodeIgnoreCase("CSE-1101")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsernameIgnoreCase("teacher")).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(20L);
            return course;
        });
        when(enrollmentRepository.findByCourseId(20L)).thenReturn(List.of());

        CourseResponse response = courseService.create(courseRequest(" CSE-1101 ", " Programming ", null));

        assertEquals("CSE-1101", response.code());
        assertEquals("Programming", response.title());
        assertEquals(10L, response.teacherId());
        assertEquals("teacher", response.teacherUsername());
    }

    @Test
    void studentIsNotAllowedToCreateCourse() {
        authenticate("student", "ROLE_STUDENT");

        assertThrows(ForbiddenException.class,
                () -> courseService.create(courseRequest("CSE-1101", "Programming", null)));

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void adminMustAssignATeacherWhenCreatingCourse() {
        authenticate("admin", "ROLE_ADMIN");
        when(courseRepository.findByCodeIgnoreCase("CSE-1101")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> courseService.create(courseRequest("CSE-1101", "Programming", null)));

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void studentCannotEnrollInTheSameCourseTwice() {
        authenticate("student", "ROLE_STUDENT");
        Student student = student(30L, "student");
        Course course = course(20L);
        when(studentRepository.findByUsernameIgnoreCase("student")).thenReturn(Optional.of(student));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseIdAndStudentId(20L, 30L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> courseService.enrollCurrentStudent(20L));

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void droppingCourseEnrollmentDeletesTheCurrentStudentsEnrollment() {
        authenticate("student", "ROLE_STUDENT");
        Student student = student(30L, "student");
        Course course = course(20L);
        Enrollment enrollment = new Enrollment();
        when(studentRepository.findByUsernameIgnoreCase("student")).thenReturn(Optional.of(student));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourseIdAndStudentId(20L, 30L)).thenReturn(Optional.of(enrollment));

        courseService.dropCurrentStudent(20L);

        verify(enrollmentRepository).delete(enrollment);
    }

    private void authenticate(String username, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(username, "password", authorities));
    }

    private CourseRequest courseRequest(String code, String title, Long teacherId) {
        CourseRequest request = new CourseRequest();
        request.setCode(code);
        request.setTitle(title);
        request.setDepartment("Computer Science and Engineering");
        request.setCreditHours(3);
        request.setTeacherId(teacherId);
        return request;
    }

    private Teacher teacher(Long id, String username, String fullName) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUsername(username);
        teacher.setFullName(fullName);
        return teacher;
    }

    private Student student(Long id, String username) {
        Student student = new Student();
        student.setId(id);
        student.setUsername(username);
        return student;
    }

    private Course course(Long id) {
        Course course = new Course();
        course.setId(id);
        course.setTeacher(teacher(10L, "teacher", "Taylor Teacher"));
        return course;
    }
}
