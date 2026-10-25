package com.smartcollege.demo;

import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UsageRecordRepository;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookingControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private User studentA;
    private User studentB;
    private User adminUser;
    private Resource testResource;
    private String tokenA;
    private String tokenB;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 1. Create or get student A
        studentA = userRepository.findByEmail("student.a@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("Alice Student");
            u.setEmail("student.a@college.edu");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.STUDENT);
            u.setActive(true);
            return userRepository.save(u);
        });

        // 2. Create or get student B
        studentB = userRepository.findByEmail("student.b@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("Bob Student");
            u.setEmail("student.b@college.edu");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.STUDENT);
            u.setActive(true);
            return userRepository.save(u);
        });

        // 3. Get admin
        adminUser = userRepository.findByEmail("admin@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("System Administrator");
            u.setEmail("admin@college.edu");
            u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            u.setActive(true);
            return userRepository.save(u);
        });

        // Generate tokens
        tokenA = tokenProvider.generateToken(studentA.getId(), studentA.getEmail(), studentA.getRole().name(), studentA.getFullName());
        tokenB = tokenProvider.generateToken(studentB.getId(), studentB.getEmail(), studentB.getRole().name(), studentB.getFullName());
        adminToken = tokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name(), adminUser.getFullName());

        // 4. Create or get available test resource
        testResource = resourceRepository.findByResourceCode("SEM-HALL-TEST").orElseGet(() -> {
            Resource r = new Resource();
            r.setResourceCode("SEM-HALL-TEST");
            r.setName("Test Seminar Hall");
            r.setCategory(ResourceCategory.SEMINAR_HALL);
            r.setLocation("Block C, 3rd Floor");
            r.setCapacity(120);
            r.setStatus(ResourceStatus.AVAILABLE);
            r.setActive(true);
            return resourceRepository.save(r);
        });

        // Clear existing bookings for test resource and students to ensure test isolation
        usageRecordRepository.deleteAll();
        bookingRepository.findByResourceId(testResource.getId())
                .forEach(bookingRepository::delete);
        bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(studentA.getId())
                .forEach(bookingRepository::delete);
        bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(studentB.getId())
                .forEach(bookingRepository::delete);
    }

    @Test
    void testCreateBookingSuccess() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String payload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "10:00:00",
                    "endTime": "11:30:00",
                    "purpose": "Project presentation rehearsal"
                }
                """, testResource.getId(), tomorrow);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.resourceId").value(testResource.getId()))
                .andExpect(jsonPath("$.data.userId").value(studentA.getId()))
                .andExpect(jsonPath("$.data.purpose").value("Project presentation rehearsal"));
    }

    @Test
    void testOverlappingApprovedBookingFailsWithConflict() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(3);

        // Pre-create an APPROVED booking from 10:00 to 11:00
        Booking approvedBooking = new Booking();
        approvedBooking.setUser(adminUser);
        approvedBooking.setResource(testResource);
        approvedBooking.setBookingDate(futureDate);
        approvedBooking.setStartTime(LocalTime.of(10, 0));
        approvedBooking.setEndTime(LocalTime.of(11, 0));
        approvedBooking.setPurpose("Existing approved conference");
        approvedBooking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(approvedBooking);

        // Student A tries to book overlapping interval 10:30 to 11:30
        String overlappingPayload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "10:30:00",
                    "endTime": "11:30:00",
                    "purpose": "Conflicting workshop"
                }
                """, testResource.getId(), futureDate);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already booked for this time slot")));
    }

    @Test
    void testAdjacentBookingsDoNotConflict() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(4);

        // Pre-create an APPROVED booking ending at 11:00
        Booking approvedBooking = new Booking();
        approvedBooking.setUser(adminUser);
        approvedBooking.setResource(testResource);
        approvedBooking.setBookingDate(futureDate);
        approvedBooking.setStartTime(LocalTime.of(10, 0));
        approvedBooking.setEndTime(LocalTime.of(11, 0));
        approvedBooking.setPurpose("Morning presentation");
        approvedBooking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(approvedBooking);

        // Student A books adjacent slot starting exactly at 11:00 (11:00 to 12:00)
        String adjacentPayload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "11:00:00",
                    "endTime": "12:00:00",
                    "purpose": "Adjacent meeting immediately following"
                }
                """, testResource.getId(), futureDate);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjacentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testInvalidDateOrTimeRejected() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        String pastPayload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "10:00:00",
                    "endTime": "11:00:00",
                    "purpose": "Past booking attempt"
                }
                """, testResource.getId(), pastDate);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pastPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Inverted time (end before start)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String invertedTimePayload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "14:00:00",
                    "endTime": "13:00:00",
                    "purpose": "Inverted time test"
                }
                """, testResource.getId(), tomorrow);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invertedTimePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("End time must be after start time")));
    }

    @Test
    void testBookingUnavailableResourceFails() throws Exception {
        // Create or get an inactive resource
        Resource savedBroken = resourceRepository.findByResourceCode("BROKEN-PROJECTOR").orElseGet(() -> {
            Resource outOfServiceResource = new Resource();
            outOfServiceResource.setResourceCode("BROKEN-PROJECTOR");
            outOfServiceResource.setName("Broken Projector 5");
            outOfServiceResource.setCategory(ResourceCategory.PROJECTOR);
            outOfServiceResource.setLocation("Room 101");
            outOfServiceResource.setStatus(ResourceStatus.UNDER_MAINTENANCE);
            outOfServiceResource.setActive(true);
            return resourceRepository.save(outOfServiceResource);
        });

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String payload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "10:00:00",
                    "endTime": "11:00:00",
                    "purpose": "Trying to book broken projector"
                }
                """, savedBroken.getId(), tomorrow);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be booked")));
    }

    @Test
    void testUserCanViewOwnBookingsAndNotOtherUserBookings() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Student A creates booking
        String payload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "09:00:00",
                    "endTime": "10:00:00",
                    "purpose": "Student A private study"
                }
                """, testResource.getId(), tomorrow);

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = extractIdFromJson(createResult.getResponse().getContentAsString());
        assertNotNull(bookingId);

        // Student A views their own bookings via /api/bookings/my
        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].purpose").value("Student A private study"));

        // Student A views specific booking via /api/bookings/{id}
        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId));

        // Student B tries to view Student A's booking -> 403 Forbidden
        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Admin can view Student A's booking -> 200 OK
        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId));
    }

    @Test
    void testCancelBookingOwnershipAndStateTransitions() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        String payload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "14:00:00",
                    "endTime": "15:00:00",
                    "purpose": "Study group to cancel"
                }
                """, testResource.getId(), tomorrow);

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = extractIdFromJson(createResult.getResponse().getContentAsString());

        // Student B attempts to cancel Student A's booking -> 403 Forbidden
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Student A cancels their own booking -> 200 OK
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Attempting to cancel an already cancelled booking -> 400 Bad Request
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already cancelled")));
    }

    @Test
    void testAdminCanReviewAndApproveBooking() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        String payload = String.format("""
                {
                    "resourceId": %d,
                    "bookingDate": "%s",
                    "startTime": "13:00:00",
                    "endTime": "14:00:00",
                    "purpose": "Faculty guest lecture"
                }
                """, testResource.getId(), futureDate);

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = extractIdFromJson(createResult.getResponse().getContentAsString());

        // Student attempts to review -> 403 Forbidden
        String reviewPayload = """
                {
                    "status": "APPROVED",
                    "adminRemarks": "Approved by administrator"
                }
                """;

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/review")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                .andExpect(status().isForbidden());

        // Admin approves booking
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.adminRemarks").value("Approved by administrator"));
    }

    @Test
    void testAvailabilityCheckEndpoint() throws Exception {
        LocalDate checkDate = LocalDate.now().plusDays(6);

        // Pre-create approved booking
        Booking approvedBooking = new Booking();
        approvedBooking.setUser(adminUser);
        approvedBooking.setResource(testResource);
        approvedBooking.setBookingDate(checkDate);
        approvedBooking.setStartTime(LocalTime.of(14, 0));
        approvedBooking.setEndTime(LocalTime.of(16, 0));
        approvedBooking.setPurpose("Department Meeting");
        approvedBooking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(approvedBooking);

        // Check availability during the booked slot -> isAvailable: false
        mockMvc.perform(get("/api/bookings/availability")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("resourceId", String.valueOf(testResource.getId()))
                        .param("date", checkDate.toString())
                        .param("startTime", "15:00:00")
                        .param("endTime", "15:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.bookedSlots", hasSize(1)));

        // Check availability outside the booked slot -> isAvailable: true
        mockMvc.perform(get("/api/bookings/availability")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("resourceId", String.valueOf(testResource.getId()))
                        .param("date", checkDate.toString())
                        .param("startTime", "16:00:00")
                        .param("endTime", "17:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
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
