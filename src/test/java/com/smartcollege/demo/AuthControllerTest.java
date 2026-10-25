package com.smartcollege.demo;

import com.smartcollege.demo.entity.Role;
import com.smartcollege.demo.entity.User;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        userRepository.findByEmail("test.student@college.edu")
                .ifPresent(userRepository::delete);
        userRepository.findByEmail("duplicate.student@college.edu")
                .ifPresent(userRepository::delete);
        userRepository.findByEmail("login.student@college.edu")
                .ifPresent(userRepository::delete);
    }

    @Test
    void testRegisterSuccess() throws Exception {
        String payload = """
                {
                    "fullName": "Test Student",
                    "email": "test.student@college.edu",
                    "password": "password123",
                    "role": "STUDENT",
                    "department": "Computer Science",
                    "phone": "1234567890"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("test.student@college.edu"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void testDuplicateRegistrationFailsWithConflict() throws Exception {
        String payload = """
                {
                    "fullName": "Duplicate Student",
                    "email": "duplicate.student@college.edu",
                    "password": "password123",
                    "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testRegistrationAsAdminIsRejected() throws Exception {
        String payload = """
                {
                    "fullName": "Hacker Admin",
                    "email": "hacker.admin@college.edu",
                    "password": "password123",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("Public registration as ADMIN is not permitted")));
    }

    @Test
    void testLoginValidCredentials() throws Exception {
        String registerPayload = """
                {
                    "fullName": "Login Student",
                    "email": "login.student@college.edu",
                    "password": "secretPassword123",
                    "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = """
                {
                    "email": "login.student@college.edu",
                    "password": "secretPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value("login.student@college.edu"));
    }

    @Test
    void testLoginInvalidPasswordFailsWithUnauthorized() throws Exception {
        String registerPayload = """
                {
                    "fullName": "Login Student",
                    "email": "login.student@college.edu",
                    "password": "correctPassword",
                    "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String badLoginPayload = """
                {
                    "email": "login.student@college.edu",
                    "password": "wrongPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    void testUnauthenticatedAccessToMeIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testAuthenticatedAccessToMeSucceeds() throws Exception {
        String registerPayload = """
                {
                    "fullName": "Test Student",
                    "email": "test.student@college.edu",
                    "password": "password123",
                    "role": "STUDENT"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        String token = extractTokenFromJson(responseBody);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test.student@college.edu"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testRoleRestrictedEndpointForbiddenForStudent() throws Exception {
        String registerPayload = """
                {
                    "fullName": "Test Student",
                    "email": "test.student@college.edu",
                    "password": "password123",
                    "role": "STUDENT"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String token = extractTokenFromJson(registerResult.getResponse().getContentAsString());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testAdminAccessToAdminDashboardSucceeds() throws Exception {
        String loginPayload = """
                {
                    "email": "admin@college.edu",
                    "password": "Admin@12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String token = extractTokenFromJson(loginResult.getResponse().getContentAsString());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    private String extractTokenFromJson(String json) {
        int tokenIdx = json.indexOf("\"token\":\"");
        if (tokenIdx == -1) return null;
        int start = tokenIdx + 9;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
