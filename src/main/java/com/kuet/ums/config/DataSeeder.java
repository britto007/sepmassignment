package com.kuet.ums.config;

import com.kuet.ums.model.Admin;
import com.kuet.ums.model.Student;
import com.kuet.ums.model.Teacher;
import com.kuet.ums.repository.AdminRepository;
import com.kuet.ums.repository.StudentRepository;
import com.kuet.ums.repository.TeacherRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AdminRepository adminRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
            Admin admin = new Admin();
            admin.setFullName("System Admin");
            admin.setEmail("admin@kuet.ac.bd");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            adminRepository.save(admin);
        }
        if (teacherRepository.findByUsernameIgnoreCase("teacher").isEmpty()) {
            Teacher teacher = new Teacher();
            teacher.setFullName("Demo Teacher");
            teacher.setEmail("teacher@kuet.ac.bd");
            teacher.setUsername("teacher");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setDepartment("Computer Science and Engineering");
            teacher.setDesignation("Lecturer");
            teacherRepository.save(teacher);
        }
        if (studentRepository.findByUsernameIgnoreCase("student").isEmpty()) {
            Student student = new Student();
            student.setFullName("Demo Student");
            student.setEmail("student@kuet.ac.bd");
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setStudentId("2201001");
            student.setDepartment("Computer Science and Engineering");
            student.setBatch("2022");
            studentRepository.save(student);
        }
    }
}
