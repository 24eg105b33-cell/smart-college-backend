package com.smartcollege.demo;

import com.smartcollege.demo.entity.Resource;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import com.smartcollege.demo.repository.ResourceRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResourceControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private String adminToken;
    private String studentToken;
    private String facultyToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        adminToken = tokenProvider.generateToken(1L, "admin@college.edu", "ADMIN", "System Administrator");
        studentToken = tokenProvider.generateToken(999L, "student.tester@college.edu", "STUDENT", "Student Tester");
        facultyToken = tokenProvider.generateToken(888L, "faculty.tester@college.edu", "FACULTY", "Faculty Tester");

        // Clean up test resource if present
        resourceRepository.findByResourceCode("TEST-LAB-101")
                .ifPresent(resourceRepository::delete);
        resourceRepository.findByResourceCode("TEST-ROOM-201")
                .ifPresent(resourceRepository::delete);
    }

    @Test
    void testAdminCanCreateResource() throws Exception {
        String payload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Advanced Robotics Laboratory",
                    "category": "LABORATORY",
                    "location": "Engineering Block, Room 204",
                    "capacity": 30,
                    "description": "Robotics workstations with 3D printers and soldering stations",
                    "status": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceCode").value("TEST-LAB-101"))
                .andExpect(jsonPath("$.data.name").value("Advanced Robotics Laboratory"))
                .andExpect(jsonPath("$.data.category").value("LABORATORY"))
                .andExpect(jsonPath("$.data.capacity").value(30))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void testStudentCannotCreateResourceForbidden() throws Exception {
        String payload = """
                {
                    "resourceCode": "TEST-ROOM-201",
                    "name": "Student Created Room",
                    "category": "CLASSROOM",
                    "location": "Main Building 101",
                    "capacity": 50
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testUnauthenticatedCannotCreateResourceUnauthorized() throws Exception {
        String payload = """
                {
                    "resourceCode": "TEST-ROOM-201",
                    "name": "Anonymous Room",
                    "category": "CLASSROOM",
                    "location": "Main Building 101"
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testCreateResourceFailsWithInvalidData() throws Exception {
        String badPayload = """
                {
                    "resourceCode": "",
                    "name": "",
                    "category": null,
                    "location": "",
                    "capacity": -5
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.resourceCode", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.capacity", notNullValue()));
    }

    @Test
    void testCreateDuplicateResourceCodeFailsWithConflict() throws Exception {
        String payload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "First Robotics Lab",
                    "category": "LABORATORY",
                    "location": "Engineering Block, Room 204",
                    "capacity": 25
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Duplicate
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testStudentCanListAndFilterResources() throws Exception {
        // Create test resource first
        String payload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Advanced Robotics Laboratory",
                    "category": "LABORATORY",
                    "location": "Science Block 301",
                    "capacity": 40
                }
                """;

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Student lists resources
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("search", "Robotics")
                        .param("category", "LABORATORY")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].resourceCode").value("TEST-LAB-101"));
    }

    @Test
    void testGetResourceByIdSuccess() throws Exception {
        String payload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Robotics Lab",
                    "category": "LABORATORY",
                    "location": "Science Wing 1",
                    "capacity": 20
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Long createdId = extractIdFromJson(body);
        assertNotNull(createdId);

        mockMvc.perform(get("/api/resources/" + createdId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdId))
                .andExpect(jsonPath("$.data.resourceCode").value("TEST-LAB-101"));
    }

    @Test
    void testGetResourceByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/resources/99999999")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testAdminCanUpdateResource() throws Exception {
        String createPayload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Original Name",
                    "category": "LABORATORY",
                    "location": "Old Location",
                    "capacity": 15
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = extractIdFromJson(createResult.getResponse().getContentAsString());

        String updatePayload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Updated Lab Name",
                    "category": "LABORATORY",
                    "location": "New Location 500",
                    "capacity": 35,
                    "description": "Renovated laboratory",
                    "status": "AVAILABLE"
                }
                """;

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Lab Name"))
                .andExpect(jsonPath("$.data.location").value("New Location 500"))
                .andExpect(jsonPath("$.data.capacity").value(35));
    }

    @Test
    void testAdminCanUpdateStatusAndDeactivateResource() throws Exception {
        String createPayload = """
                {
                    "resourceCode": "TEST-LAB-101",
                    "name": "Lab to Status Test",
                    "category": "LABORATORY",
                    "location": "Room 101",
                    "capacity": 20
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = extractIdFromJson(createResult.getResponse().getContentAsString());

        // Update status to UNDER_MAINTENANCE via PATCH
        String patchPayload = """
                {
                    "status": "UNDER_MAINTENANCE",
                    "remarks": "Scheduled hardware upgrade"
                }
                """;

        mockMvc.perform(patch("/api/resources/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_MAINTENANCE"));

        // Deactivate via DELETE
        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.status").value("OUT_OF_SERVICE"));
    }

    private Long extractIdFromJson(String json) {
        int idIdx = json.indexOf("\"id\":");
        if (idIdx == -1) return null;
        int start = idIdx + 5;
        int end = json.indexOf(",", start);
        if (end == -1) {
            end = json.indexOf("}", start);
        }
        return Long.parseLong(json.substring(start, end).trim());
    }
}
