package com.kuet.ums;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentEnrollmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentEnrollsThenDropsAndASecondDropIsNotFound() throws Exception {
        Number courseId = createCourse("CSE-2101", "Algorithms", null);

        mockMvc.perform(post("/api/courses/" + courseId + "/enroll")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.enrolledCount").value(1));

        mockMvc.perform(delete("/api/courses/" + courseId + "/enroll")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + courseId).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false))
                .andExpect(jsonPath("$.enrolledCount").value(0));

        mockMvc.perform(delete("/api/courses/" + courseId + "/enroll")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You are not enrolled in this course."));
    }

    @Test
    void adminCreatesACourseAssignedToATeacher() throws Exception {
        MvcResult teachers = mockMvc.perform(get("/api/teachers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        List<?> ids = JsonPath.read(teachers.getResponse().getContentAsString(), "$[?(@.username == 'teacher')].id");
        Number teacherId = (Number) ids.get(0);

        mockMvc.perform(post("/api/courses")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-2103", "Operating Systems", teacherId.longValue())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CSE-2103"))
                .andExpect(jsonPath("$.teacherId").value(teacherId.intValue()))
                .andExpect(jsonPath("$.teacherUsername").value("teacher"))
                .andExpect(jsonPath("$.teacherName").value("Demo Teacher"))
                .andExpect(jsonPath("$.enrolledCount").value(0));
    }

    @Test
    void anonymousCoursePageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private Number createCourse(String code, String title, Long teacherId) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/courses")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson(code, title, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private String courseJson(String code, String title, Long teacherId) {
        String teacher = teacherId == null ? "" : ",\n  \"teacherId\": " + teacherId;
        return """
                {
                  "code": "%s",
                  "title": "%s",
                  "department": "Computer Science and Engineering",
                  "creditHours": 3%s
                }
                """.formatted(code, title, teacher);
    }
}
