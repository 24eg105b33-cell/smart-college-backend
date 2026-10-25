package com.smartcollege.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UsageRecordRepository;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.security.JwtTokenProvider;
import com.smartcollege.demo.service.BookingService;
import com.smartcollege.demo.service.impl.BookingServiceImpl;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApprovalAndUsageControllerTest {

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

    @Autowired
    private BookingService bookingService;

    private MockMvc mockMvc;
    private User studentUser;
    private User otherStudent;
    private User facultyUser;
    private User adminUser;
    private Resource testResource;
    private String studentToken;
    private String otherStudentToken;
    private String facultyToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 1. Create or get student
        studentUser = userRepository.findByEmail("approval.student@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("Approval Student");
            u.setEmail("approval.student@college.edu");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.STUDENT);
            u.setActive(true);
            return userRepository.save(u);
        });

        // 2. Create or get second student
        otherStudent = userRepository.findByEmail("other.student@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("Other Student");
            u.setEmail("other.student@college.edu");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.STUDENT);
            u.setActive(true);
            return userRepository.save(u);
        });

        // 3. Create or get faculty
        facultyUser = userRepository.findByEmail("faculty.reviewer@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("Dr. Faculty Reviewer");
            u.setEmail("faculty.reviewer@college.edu");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.FACULTY);
            u.setActive(true);
            return userRepository.save(u);
        });

        // 4. Create or get admin
        adminUser = userRepository.findByEmail("admin@college.edu").orElseGet(() -> {
            User u = new User();
            u.setFullName("System Administrator");
            u.setEmail("admin@college.edu");
            u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            u.setActive(true);
            return userRepository.save(u);
        });

        // Tokens
        studentToken = tokenProvider.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name(), studentUser.getFullName());
        otherStudentToken = tokenProvider.generateToken(otherStudent.getId(), otherStudent.getEmail(), otherStudent.getRole().name(), otherStudent.getFullName());
        facultyToken = tokenProvider.generateToken(facultyUser.getId(), facultyUser.getEmail(), facultyUser.getRole().name(), facultyUser.getFullName());
        adminToken = tokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name(), adminUser.getFullName());

        // Test Resource
        testResource = resourceRepository.findByResourceCode("APPROVAL-LAB-1").orElseGet(() -> {
            Resource r = new Resource();
            r.setResourceCode("APPROVAL-LAB-1");
            r.setName("Approval Test Lab");
            r.setCategory(ResourceCategory.LABORATORY);
            r.setLocation("Building A, Room 105");
            r.setCapacity(40);
            r.setStatus(ResourceStatus.AVAILABLE);
            r.setActive(true);
            return resourceRepository.save(r);
        });

        // Reset policy default for tests
        if (bookingService instanceof BookingServiceImpl impl) {
            impl.setFacultyApprovalEnabled(false);
        }

        // Clear existing records for testResource to ensure test isolation across test runs
        usageRecordRepository.findByResourceId(testResource.getId())
                .forEach(usageRecordRepository::delete);
        bookingRepository.findByResourceId(testResource.getId())
                .forEach(bookingRepository::delete);
    }

    private Booking createPendingBooking(User user, LocalDate date, LocalTime start, LocalTime end, String purpose) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResource(testResource);
        booking.setBookingDate(date);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(purpose);
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    @Test
    void testAdminCanListPendingBookings() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        createPendingBooking(studentUser, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Pending review 1");

        mockMvc.perform(get("/api/bookings/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[?(@.status == 'PENDING')]").exists());
    }

    @Test
    void testStudentCannotListPendingBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/pending")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanApprovePendingBooking() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "Math seminar");

        String approvePayload = """
                {
                    "remarks": "Approved by Admin for seminar"
                }
                """;

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.adminRemarks").value("Approved by Admin for seminar"))
                .andExpect(jsonPath("$.data.reviewedById").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.reviewedByName").value(adminUser.getFullName()))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        // Verify entity persisted in DB
        Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.APPROVED, reloaded.getStatus());
        assertNotNull(reloaded.getReviewedBy());
        assertEquals(adminUser.getId(), reloaded.getReviewedBy().getId());
        assertNotNull(reloaded.getReviewedAt());
    }

    @Test
    void testAdminCanRejectPendingBookingWithReason() throws Exception {
        LocalDate date = LocalDate.now().plusDays(4);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(14, 0), LocalTime.of(15, 30), "Robotics prep");

        String rejectPayload = """
                {
                    "rejectionReason": "Resource reserved for annual campus inspection"
                }
                """;

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.adminRemarks").value("Resource reserved for annual campus inspection"))
                .andExpect(jsonPath("$.data.reviewedById").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.reviewedByName").value(adminUser.getFullName()))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
    }

    @Test
    void testRejectPendingBookingWithoutReasonFails() throws Exception {
        LocalDate date = LocalDate.now().plusDays(5);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Workshop");

        String emptyReasonPayload = """
                {
                    "rejectionReason": ""
                }
                """;

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyReasonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveNonPendingBookingFails() throws Exception {
        LocalDate date = LocalDate.now().plusDays(6);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(11, 0), LocalTime.of(12, 0), "Discussion");
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot approve booking with status: CANCELLED")));
    }

    @Test
    void testFacultyApprovalPolicyEnforcement() throws Exception {
        LocalDate date = LocalDate.now().plusDays(7);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(13, 0), LocalTime.of(14, 0), "Faculty policy test");

        // 1. By default, faculty approval is disabled -> 403 Access Denied
        if (bookingService instanceof BookingServiceImpl impl) {
            impl.setFacultyApprovalEnabled(false);
        }

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/approve")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Faculty approval is not permitted by current booking policy")));

        // 2. Enable faculty approval policy -> succeeds
        if (bookingService instanceof BookingServiceImpl impl) {
            impl.setFacultyApprovalEnabled(true);
        }

        mockMvc.perform(patch("/api/bookings/" + booking.getId() + "/approve")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\": \"Approved under faculty policy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedById").value(facultyUser.getId()));
    }

    @Test
    void testConcurrentConflictingApprovalsPrevented() throws Exception {
        LocalDate date = LocalDate.now().plusDays(8);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 0);

        // Booking 1 and Booking 2 are both PENDING for the same time slot
        Booking b1 = createPendingBooking(studentUser, date, start, end, "First slot booking");
        Booking b2 = createPendingBooking(otherStudent, date, start, end, "Conflicting slot booking");

        // Approve b1 first
        mockMvc.perform(patch("/api/bookings/" + b1.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Attempting to approve conflicting b2 must fail with 409 Conflict
        mockMvc.perform(patch("/api/bookings/" + b2.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already booked for this time slot")));
    }

    @Test
    void testCheckInForApprovedBooking() throws Exception {
        LocalDate date = LocalDate.now().plusDays(9);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(15, 0), LocalTime.of(16, 0), "Lab practice");
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        String checkInPayload = String.format("""
                {
                    "bookingId": %d,
                    "remarks": "Arrived on time for lab practice"
                }
                """, booking.getId());

        MvcResult result = mockMvc.perform(post("/api/usage/check-in")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.bookingId").value(booking.getId()))
                .andExpect(jsonPath("$.data.userId").value(studentUser.getId()))
                .andExpect(jsonPath("$.data.checkInTime").isNotEmpty())
                .andReturn();

        // Duplicate check-in must fail
        mockMvc.perform(post("/api/usage/check-in")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already has an active check-in record")));
    }

    @Test
    void testCheckInForPendingBookingFails() throws Exception {
        LocalDate date = LocalDate.now().plusDays(10);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "Unapproved session");

        String checkInPayload = String.format("""
                {
                    "bookingId": %d
                }
                """, booking.getId());

        mockMvc.perform(post("/api/usage/check-in")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Booking must be in APPROVED status")));
    }

    @Test
    void testCheckInAnotherUsersBookingFails() throws Exception {
        LocalDate date = LocalDate.now().plusDays(11);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "Alice's booking");
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        String checkInPayload = String.format("""
                {
                    "bookingId": %d
                }
                """, booking.getId());

        // otherStudent tries to check in for studentUser's booking -> 403 Forbidden
        mockMvc.perform(post("/api/usage/check-in")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCheckOutCompletesUsageAndAdvancesBookingStatus() throws Exception {
        LocalDate date = LocalDate.now().plusDays(12);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(8, 0), LocalTime.of(9, 0), "Early morning study");
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        // Check in
        UsageRecord record = new UsageRecord();
        record.setBooking(booking);
        record.setResource(testResource);
        record.setUser(studentUser);
        record.setStatus(UsageStatus.CHECKED_IN);
        record.setRemarks("Started");
        UsageRecord savedRecord = usageRecordRepository.save(record);

        // Check out
        String checkOutPayload = """
                {
                    "remarks": "Finished early and locked the doors"
                }
                """;

        mockMvc.perform(post("/api/usage/" + savedRecord.getId() + "/check-out")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkOutPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.checkOutTime").isNotEmpty())
                .andExpect(jsonPath("$.data.remarks", containsString("Finished early and locked the doors")));

        // Verify booking status was advanced to COMPLETED
        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, reloadedBooking.getStatus());

        // Repeated check-out must fail
        mockMvc.perform(post("/api/usage/" + savedRecord.getId() + "/check-out")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("record is not currently checked in")));
    }

    @Test
    void testUserCanViewMyUsageAndAdminCanViewAllUsage() throws Exception {
        LocalDate date = LocalDate.now().plusDays(13);
        Booking booking = createPendingBooking(studentUser, date, LocalTime.of(16, 0), LocalTime.of(17, 0), "Personal usage test");
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        UsageRecord record = new UsageRecord();
        record.setBooking(booking);
        record.setResource(testResource);
        record.setUser(studentUser);
        record.setStatus(UsageStatus.CHECKED_IN);
        usageRecordRepository.save(record);

        // Student can view own usage
        mockMvc.perform(get("/api/usage/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", not(empty())));

        // Student cannot view all usage (admin/faculty only)
        mockMvc.perform(get("/api/usage")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Admin can view all usage
        mockMvc.perform(get("/api/usage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }
}
