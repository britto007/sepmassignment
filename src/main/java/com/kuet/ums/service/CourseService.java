package com.kuet.ums.service;

import com.kuet.ums.dto.CourseRequest;
import com.kuet.ums.dto.CourseResponse;
import com.kuet.ums.dto.EnrollmentResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.ForbiddenException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.model.Course;
import com.kuet.ums.model.Enrollment;
import com.kuet.ums.model.Student;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.StudentRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list() {
        Long currentStudentId = currentStudentId();
        return courseRepository.findAllByOrderByCodeAsc().stream()
                .map(course -> toResponse(course, currentStudentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse get(Long id) {
        return toResponse(find(id), currentStudentId());
    }

    @Transactional(readOnly = true)
    public void ensureCanEdit(Long id) {
        ensureCanChange(find(id));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        if (!CurrentUser.isAdmin() && !CurrentUser.hasRole("TEACHER")) {
            throw new ForbiddenException("Only an admin or a teacher can create a course.");
        }
        String code = request.getCode().trim();
        ensureCodeAvailable(code, null);
        Course course = new Course();
        applyDetails(course, request, code);
        course.setTeacher(teacherForCreate(request));
        return toResponse(courseRepository.save(course), currentStudentId());
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = find(id);
        ensureCanChange(course);
        String code = request.getCode().trim();
        ensureCodeAvailable(code, course.getId());
        applyDetails(course, request, code);
        if (CurrentUser.isAdmin()) {
            course.setTeacher(resolveTeacher(request.getTeacherId()));
        }
        return toResponse(courseRepository.save(course), currentStudentId());
    }

    @Transactional
    public void delete(Long id) {
        Course course = find(id);
        ensureCanChange(course);
        enrollmentRepository.deleteByCourseId(course.getId());
        courseRepository.delete(course);
    }

    @Transactional
    public CourseResponse enrollCurrentStudent(Long courseId) {
        Student student = requireCurrentStudent();
        Course course = find(courseId);
        if (enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {
            throw new ConflictException("You are already enrolled in this course.");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollmentRepository.save(enrollment);
        return toResponse(course, student.getId());
    }

    @Transactional
    public void dropCurrentStudent(Long courseId) {
        Student student = requireCurrentStudent();
        Course course = find(courseId);
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(course.getId(), student.getId())
                .orElseThrow(() -> new NotFoundException("You are not enrolled in this course."));
        enrollmentRepository.delete(enrollment);
    }

    @Transactional
    public void removeStudent(Long courseId, Long studentId) {
        Course course = find(courseId);
        ensureCanChange(course);
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student not found.");
        }
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(course.getId(), studentId)
                .orElseThrow(() -> new NotFoundException("That student is not enrolled in this course."));
        enrollmentRepository.delete(enrollment);
    }

    private Course find(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course not found."));
    }

    private void applyDetails(Course course, CourseRequest request, String code) {
        course.setCode(code);
        course.setTitle(request.getTitle().trim());
        course.setDepartment(request.getDepartment().trim());
        course.setCreditHours(request.getCreditHours());
    }

    private Teacher teacherForCreate(CourseRequest request) {
        if (CurrentUser.isAdmin()) {
            return resolveTeacher(request.getTeacherId());
        }
        return teacherRepository.findByUsernameIgnoreCase(CurrentUser.username())
                .orElseThrow(() -> new ForbiddenException("Only an admin or a teacher can create a course."));
    }

    private Teacher resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            throw new BadRequestException("Choose a teacher for this course.");
        }
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Teacher not found."));
    }

    private void ensureCanChange(Course course) {
        if (CurrentUser.isAdmin()) {
            return;
        }
        String teacherUsername = course.getTeacher().getUsername();
        if (teacherUsername != null && teacherUsername.equalsIgnoreCase(CurrentUser.username())) {
            return;
        }
        throw new ForbiddenException("You can only change a course you teach.");
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        courseRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Course code is already in use.");
            }
        });
    }

    private Student requireCurrentStudent() {
        if (!CurrentUser.hasRole("STUDENT")) {
            throw new ForbiddenException("Only a student can enroll in a course.");
        }
        return studentRepository.findByUsernameIgnoreCase(CurrentUser.username())
                .orElseThrow(() -> new ForbiddenException("Only a student can enroll in a course."));
    }

    private Long currentStudentId() {
        if (!CurrentUser.hasRole("STUDENT")) {
            return null;
        }
        return studentRepository.findByUsernameIgnoreCase(CurrentUser.username())
                .map(Student::getId)
                .orElse(null);
    }

    private CourseResponse toResponse(Course course, Long currentStudentId) {
        List<EnrollmentResponse> students = enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(enrollment -> enrollment.getStudent())
                .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(student -> new EnrollmentResponse(
                        student.getId(),
                        student.getFullName(),
                        student.getUsername(),
                        student.getStudentId()))
                .toList();
        boolean enrolled = currentStudentId != null
                && students.stream().anyMatch(student -> student.studentId().equals(currentStudentId));
        Teacher teacher = course.getTeacher();
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDepartment(),
                course.getCreditHours(),
                teacher.getId(),
                teacher.getFullName(),
                teacher.getUsername(),
                students.size(),
                enrolled,
                students);
    }
}
