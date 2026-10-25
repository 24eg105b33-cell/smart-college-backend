package com.smartcollege.demo.config;

import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data.enabled:true}")
    private boolean enabled;

    public DemoDataInitializer(UserRepository userRepository,
                               ResourceRepository resourceRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        // 1. Seed demo student
        String studentEmail = "student.a@college.edu";
        if (!userRepository.existsByEmail(studentEmail)) {
            User student = new User();
            student.setFullName("Student Alice");
            student.setEmail(studentEmail);
            student.setPassword(passwordEncoder.encode("password123"));
            student.setRole(Role.STUDENT);
            student.setDepartment("Computer Science");
            student.setPhone("+1-555-0101");
            student.setActive(true);
            userRepository.save(student);
            log.info("Demo student account created: {}", studentEmail);
        }

        // 2. Seed demo faculty
        String facultyEmail = "faculty.john@college.edu";
        if (!userRepository.existsByEmail(facultyEmail)) {
            User faculty = new User();
            faculty.setFullName("Prof. John Smith");
            faculty.setEmail(facultyEmail);
            faculty.setPassword(passwordEncoder.encode("Faculty@123"));
            faculty.setRole(Role.FACULTY);
            faculty.setDepartment("Computer Science");
            faculty.setPhone("+1-555-0102");
            faculty.setActive(true);
            userRepository.save(faculty);
            log.info("Demo faculty account created: {}", facultyEmail);
        }

        // 3. Seed demo resources if table is empty
        if (resourceRepository.count() == 0) {
            List<Resource> sampleResources = List.of(
                    new Resource(
                            "LAB-CS-301",
                            "Advanced Computing Lab",
                            ResourceCategory.LABORATORY,
                            "Tech Block, 3rd Floor",
                            60,
                            "High-performance GPU workstations for computer science, artificial intelligence, and simulations.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "AUD-HALL-A",
                            "Main Auditorium & Seminar Hall",
                            ResourceCategory.SEMINAR_HALL,
                            "Academic Block A, Ground Floor",
                            250,
                            "Equipped with Dolby surround sound, dual 4K laser projection screens, and stage lighting for seminars.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "CR-204",
                            "Smart Classroom 204",
                            ResourceCategory.CLASSROOM,
                            "Science Block, 2nd Floor",
                            70,
                            "Equipped with interactive smartboard, lecture recording system, and central air conditioning.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "AV-PROJ-01",
                            "4K UltraHD Laser Projector Unit",
                            ResourceCategory.PROJECTOR,
                            "Media Resource Center, Room 12",
                            1,
                            "High-lumen portable laser projector with HDMI, Wi-Fi 6 casting, and ceiling mount adapters.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "LIB-RES-05",
                            "Digital Library Research Commons",
                            ResourceCategory.LIBRARY,
                            "Central Library, 2nd Floor",
                            40,
                            "Quiet study zones, high-speed campus Wi-Fi, reference database terminals, and discussion pods.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "LAB-IOT-02",
                            "Robotics & IoT Innovation Station",
                            ResourceCategory.EQUIPMENT,
                            "Engineering Hub, Bay 4",
                            25,
                            "Rapid prototyping workstations with 3D printers, oscilloscopes, sensors, and microcontrollers.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "WS-GPU-808",
                            "High-Performance Workstation Node",
                            ResourceCategory.COMPUTER,
                            "AI & Data Science Pod 8",
                            1,
                            "Dedicated workstation with NVIDIA RTX 4090 GPU, 64GB RAM, and deep learning framework environment.",
                            ResourceStatus.AVAILABLE
                    ),
                    new Resource(
                            "SPT-INDOOR-1",
                            "Campus Indoor Sports Complex",
                            ResourceCategory.SPORTS,
                            "Physical Education Wing",
                            100,
                            "Indoor multi-court facility for badminton, table tennis, and yoga sessions.",
                            ResourceStatus.AVAILABLE
                    )
            );

            resourceRepository.saveAll(sampleResources);
            log.info("Initialized {} demo campus resources", sampleResources.size());
        }
    }
}
