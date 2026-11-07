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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teacherCreatesACourseAndStudentEnrollsOnce() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());

        MvcResult created = mockMvc.perform(post("/api/courses")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-1101", "Structured Programming", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CSE-1101"))
                .andExpect(jsonPath("$.teacherUsername").value("teacher"))
                .andExpect(jsonPath("$.enrolledCount").value(0))
                .andReturn();

        Number courseId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/courses")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-9999", "Not allowed", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/courses/" + courseId + "/enroll")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.enrolledCount").value(1));

        mockMvc.perform(post("/api/courses/" + courseId + "/enroll")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/courses/" + courseId)
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-1101", "Changed by student", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotEditAnotherTeachersCourse() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Other Teacher",
                                  "email": "course.other@kuet.ac.bd",
                                  "username": "course.other",
                                  "password": "secret12",
                                  "department": "Computer Science and Engineering",
                                  "designation": "Lecturer"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult created = mockMvc.perform(post("/api/courses")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-1203", "Discrete Mathematics", null)))
                .andExpect(status().isCreated())
                .andReturn();
        Number courseId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/courses/" + courseId)
                        .with(user("course.other").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-1203", "Taken over", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/courses/" + courseId)
                        .with(user("course.other").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/courses/" + courseId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("CSE-1203", "Discrete Mathematics II", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Discrete Mathematics II"))
                .andExpect(jsonPath("$.teacherUsername").value("teacher"));
    }

    @Test
    void coursePagesFollowTheSignedInRole() throws Exception {
        mockMvc.perform(get("/courses").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Courses")))
                .andExpect(content().string(containsString("Create course")));

        mockMvc.perform(get("/courses").with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Courses")))
                .andExpect(content().string(not(containsString("Create course"))));

        mockMvc.perform(get("/courses").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"teacherId\"")));
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
