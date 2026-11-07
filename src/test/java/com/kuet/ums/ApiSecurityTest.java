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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousApiCallsAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admins")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/teachers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesCanUseOnlyTheirResources() throws Exception {
        mockMvc.perform(get("/api/admins").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/students").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/teachers").with(user("teacher").roles("TEACHER"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admins").with(user("teacher").roles("TEACHER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students").with(user("teacher").roles("TEACHER"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/students").with(user("teacher").roles("TEACHER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("blocked", "blocked@kuet.ac.bd", "blocked.student")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/students").with(user("student").roles("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admins").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/teachers").with(user("student").roles("STUDENT")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("blocked", "blocked-teacher@kuet.ac.bd", "blocked.teacher")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCrudHidesPasswordsAndRejectsBadData() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Nabila Rahman", "nabila@kuet.ac.bd", "nabila")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("nabila"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("secret12"))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/teachers/" + id).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.department").value("Computer Science and Engineering"));

        mockMvc.perform(put("/api/teachers/" + id)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nabila Rahman",
                                  "email": "nabila@kuet.ac.bd",
                                  "username": "nabila",
                                  "password": "changed1",
                                  "department": "Computer Science and Engineering",
                                  "designation": "Assistant Professor"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/teachers/" + id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nabila Rahman",
                                  "email": "nabila@kuet.ac.bd",
                                  "username": "nabila",
                                  "password": "changed1",
                                  "department": "Computer Science and Engineering",
                                  "designation": "Assistant Professor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation").value("Assistant Professor"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "nabila")
                        .param("password", "changed1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/teachers"));

        mockMvc.perform(delete("/api/teachers/" + id).with(user("teacher").roles("TEACHER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/teachers/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teachers/" + id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateUsernameMissingFieldsAndSelfDeleteAreRejected() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Another Teacher", "another.teacher@kuet.ac.bd", "teacher")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/students")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","username":"x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fullName").exists());

        MvcResult teachers = mockMvc.perform(get("/api/teachers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andReturn();
        List<?> ids = JsonPath.read(teachers.getResponse().getContentAsString(), "$[?(@.username == 'teacher')].id");
        int ownId = ((Number) ids.get(0)).intValue();

        mockMvc.perform(delete("/api/teachers/" + ownId).with(user("teacher").roles("TEACHER")).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only an admin can delete accounts."));

        mockMvc.perform(get("/api/students/99999").with(user("student").roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginLogoutAndPagesFollowRoles() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admins"));

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "student")
                        .param("password", "student123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/students"));

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "teacher")
                        .param("password", "wrong-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(post("/logout").with(user("teacher").roles("TEACHER")).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/admins").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/access-denied"));

        mockMvc.perform(get("/teachers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/teachers\"")))
                .andExpect(content().string(containsString("Edit")))
                .andExpect(content().string(containsString("Create teacher")))
                .andExpect(content().string(not(containsString("href=\"/admins\""))))
                .andExpect(content().string(not(containsString("href=\"/students\""))))
                .andExpect(content().string(not(containsString("teacher123"))))
                .andExpect(content().string(not(containsString("$2a$"))));

        mockMvc.perform(get("/students").with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/students\"")))
                .andExpect(content().string(containsString("Edit")))
                .andExpect(content().string(containsString("Create student")))
                .andExpect(content().string(not(containsString("href=\"/admins\""))))
                .andExpect(content().string(not(containsString("href=\"/teachers\""))));

        mockMvc.perform(get("/teachers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create teacher")))
                .andExpect(content().string(containsString("Delete")));

        mockMvc.perform(get("/admins").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admins\"")))
                .andExpect(content().string(containsString("href=\"/teachers\"")))
                .andExpect(content().string(containsString("href=\"/students\"")));
    }

    @Test
    void teacherAndStudentCanEditOnlyTheirOwnAccount() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/teachers")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Other Teacher", "other.teacher@kuet.ac.bd", "other.teacher")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        mockMvc.perform(post("/api/students")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Other Student", "other.student@kuet.ac.bd", "other.student")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
        Number otherId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/teachers/" + otherId + "/edit").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/teachers"));

        MvcResult teachers = mockMvc.perform(get("/api/teachers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andReturn();
        List<?> ownIds = JsonPath.read(teachers.getResponse().getContentAsString(), "$[?(@.username == 'teacher')].id");
        int ownId = ((Number) ownIds.get(0)).intValue();

        mockMvc.perform(put("/api/teachers/" + ownId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Demo Teacher",
                                  "email": "teacher@kuet.ac.bd",
                                  "username": "teacher",
                                  "department": "Computer Science and Engineering",
                                  "designation": "Senior Lecturer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation").value("Senior Lecturer"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/teachers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/teachers/" + ownId + "/edit\"")))
                .andExpect(content().string(not(containsString("href=\"/teachers/" + otherId + "/edit\""))));
    }

    private String teacherJson(String fullName, String email, String username) {
        return """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "username": "%s",
                  "password": "secret12",
                  "department": "Computer Science and Engineering",
                  "designation": "Lecturer"
                }
                """.formatted(fullName, email, username);
    }

    private String studentJson(String fullName, String email, String username) {
        return """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "username": "%s",
                  "password": "secret12",
                  "studentId": "%s",
                  "department": "Computer Science and Engineering",
                  "batch": "2024"
                }
                """.formatted(fullName, email, username, username);
    }
}
