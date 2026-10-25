package com.smartcollege.demo;

import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UsageRecordRepository;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ReportControllerTest {

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

    private User adminUser;
    private User facultyUser;
    private User studentUser;

    private String adminToken;
    private String facultyToken;
    private String studentToken;

    private Resource classroom;
    private Resource laboratory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        usageRecordRepository.deleteAll();
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = new User();
        adminUser.setFullName("Admin Reports");
        adminUser.setEmail("admin." + suffix + "@college.edu");
        adminUser.setPassword(passwordEncoder.encode("Pass@123"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setDepartment("Administration");
        adminUser.setActive(true);
        userRepository.save(adminUser);
        adminToken = tokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name(), adminUser.getFullName());

        facultyUser = new User();
        facultyUser.setFullName("Faculty Reports");
        facultyUser.setEmail("faculty." + suffix + "@college.edu");
        facultyUser.setPassword(passwordEncoder.encode("Pass@123"));
        facultyUser.setRole(Role.FACULTY);
        facultyUser.setDepartment("Computer Science");
        facultyUser.setActive(true);
        userRepository.save(facultyUser);
        facultyToken = tokenProvider.generateToken(facultyUser.getId(), facultyUser.getEmail(), facultyUser.getRole().name(), facultyUser.getFullName());

        studentUser = new User();
        studentUser.setFullName("Student Reports");
        studentUser.setEmail("student." + suffix + "@college.edu");
        studentUser.setPassword(passwordEncoder.encode("Pass@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setDepartment("Computer Science");
        studentUser.setActive(true);
        userRepository.save(studentUser);
        studentToken = tokenProvider.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name(), studentUser.getFullName());

        classroom = new Resource(
                "CR-" + suffix,
                "Smart Classroom 101",
                ResourceCategory.CLASSROOM,
                "Science Wing",
                60,
                "Equipped with smart screen",
                ResourceStatus.AVAILABLE
        );
        resourceRepository.save(classroom);

        laboratory = new Resource(
                "LAB-" + suffix,
                "Electronics Station",
                ResourceCategory.LABORATORY,
                "Engineering Hub",
                30,
                "Equipped with microcontrollers",
                ResourceStatus.AVAILABLE
        );
        resourceRepository.save(laboratory);
    }

    @AfterEach
    void tearDown() {
        usageRecordRepository.deleteAll();
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getSummaryReport_asAdmin_returns200AndValidStats() throws Exception {
        LocalDate today = LocalDate.now();

        // Create an approved booking: 10:00 to 12:00 (2 hours)
        Booking b1 = new Booking(studentUser, classroom, today, LocalTime.of(10, 0), LocalTime.of(12, 0), "Workshop");
        b1.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(b1);

        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalActiveResources", is(2)))
                .andExpect(jsonPath("$.data.totalBookings", is(1)))
                .andExpect(jsonPath("$.data.approvedBookings", is(1)))
                .andExpect(jsonPath("$.data.totalBookedHours", is(2.0)))
                .andExpect(jsonPath("$.data.trends", not(empty())))
                .andExpect(jsonPath("$.data.categorySummaries", not(empty())));
    }

    @Test
    void getSummaryReport_asFaculty_returns200() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void getSummaryReport_asStudent_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummaryReport_unauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummaryReport_withFilters_returnsFilteredMetrics() throws Exception {
        LocalDate today = LocalDate.now();

        // Booking on classroom
        Booking b1 = new Booking(studentUser, classroom, today, LocalTime.of(9, 0), LocalTime.of(11, 0), "Math Seminar");
        b1.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(b1);

        // Booking on laboratory
        Booking b2 = new Booking(facultyUser, laboratory, today, LocalTime.of(14, 0), LocalTime.of(16, 0), "Robotics Demo");
        b2.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(b2);

        // Filter for CLASSROOM only
        mockMvc.perform(get("/api/reports/summary")
                        .param("category", "CLASSROOM")
                        .param("startDate", today.minusDays(7).toString())
                        .param("endDate", today.toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveResources", is(1)))
                .andExpect(jsonPath("$.data.totalBookings", is(1)))
                .andExpect(jsonPath("$.data.totalBookedHours", is(2.0)));
    }

    @Test
    void utilizationCalculation_deduplicatesOverlappingTimePeriodsCorrectly() throws Exception {
        LocalDate today = LocalDate.now();

        // Suppose two overlapping intervals exist:
        // Booking A: 09:00 - 11:00 (2 hours)
        // Booking B: 10:00 - 12:00 (2 hours)
        // Total merged span is 09:00 - 12:00 = exactly 3.0 hours (NOT 4.0 hours)
        Booking bA = new Booking(studentUser, classroom, today, LocalTime.of(9, 0), LocalTime.of(11, 0), "Session A");
        bA.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(bA);

        Booking bB = new Booking(facultyUser, classroom, today, LocalTime.of(10, 0), LocalTime.of(12, 0), "Session B");
        bB.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(bB);

        mockMvc.perform(get("/api/reports/summary")
                        .param("resourceId", classroom.getId().toString())
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookedHours", is(3.0)));
    }

    @Test
    void getSummaryReport_handlesZeroDenominatorsAndEmptyDatasetsSafely() throws Exception {
        // Clear all resources and bookings to test zero denominator handling
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();

        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        mockMvc.perform(get("/api/reports/summary")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveResources", is(0)))
                .andExpect(jsonPath("$.data.totalBookings", is(0)))
                .andExpect(jsonPath("$.data.utilizationRate", is(0.0)))
                .andExpect(jsonPath("$.data.totalBookedHours", is(0.0)))
                .andExpect(jsonPath("$.data.trends", hasSize(6))); // 6 continuous days filled with zero
    }
}
