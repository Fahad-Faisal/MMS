// src/main/java/com/mms/service/DataInitializerService.java
package com.mms.service;

import com.mms.entity.Student;
import com.mms.entity.User;
import com.mms.enums.Role;
import com.mms.enums.StudentStatus;
import com.mms.repository.StudentRepository;
import com.mms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerService implements CommandLineRunner {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.data.initialize:true}")
    private boolean initializeData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!initializeData) {
            log.info("Data initialization is disabled.");
            return;
        }

        log.info("🚀 Starting data initialization...");

        initializeUsers();
        initializeStudents();

        log.info("✅ Data initialization completed!");
    }

    @Transactional
    public void initializeUsers() {
//            User user=User.builder()
//                    .username("admin")
//                    .fullName("Ahmed Hossain")
//                    .email("admin@madrasa.edu")
//                    .passwordHash(passwordEncoder.encode("admin123"))
//                    .role(Role.ADMINISTRATOR)
//                    .active(true)
//                    .twoFactorEnabled(false)
//                    .build();
//            userRepository.save(user);


        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping initialization.");
            return;
        }

        log.info("📝 Creating default users...");

        // Don't set IDs - let Hibernate generate them
        List<User> defaultUsers = Arrays.asList(
                User.builder()
                        .username("admin")
                        .fullName("Ahmed Hossain")
                        .email("admin@madrasa.edu")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.ADMINISTRATOR)
                        .active(true)
                        .twoFactorEnabled(false)
                        .build(),

                User.builder()
                        .username("muhtamim")
                        .fullName("Mufti Abdul Karim")
                        .email("muhtamim@madrasa.edu")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.MUHTAMIM)
                        .active(true)
                        .twoFactorEnabled(false)
                        .build(),

                User.builder()
                        .username("teacher")
                        .fullName("Maulana Yusuf Ali")
                        .email("teacher@madrasa.edu")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.INSTRUCTOR)
                        .active(true)
                        .twoFactorEnabled(false)
                        .build(),

                User.builder()
                        .username("accountant")
                        .fullName("Fatima Khatun")
                        .email("accountant@madrasa.edu")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.ACCOUNTANT)
                        .active(true)
                        .twoFactorEnabled(false)
                        .build()
        );

        userRepository.saveAll(defaultUsers);
        log.info("✅ Created {} default users", defaultUsers.size());
    }

    @Transactional
    public void initializeStudents() {
        if (studentRepository.count() > 0) {
            log.info("Students already exist. Skipping initialization.");
            return;
        }

        log.info("📝 Creating sample students...");

        // Don't set IDs - let Hibernate generate them
        List<Student> sampleStudents = Arrays.asList(
                Student.builder()
                        .rollNo("2026-001")
                        .fullName("Yusuf Rahman")
                        .fatherName("Abdul Rahman")
                        .motherName("Ayesha Begum")
                        .dateOfBirth(LocalDate.of(2012, 4, 11))
                        .className("Hifz - Intermediate")
                        .guardianPhone("+8801711000001")
                        .guardianEmail("guardian1@example.com")
                        .address("Mirpur, Dhaka")
                        .admissionDate(LocalDate.of(2025, 1, 15))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1500)
                        .build(),

                Student.builder()
                        .rollNo("2026-002")
                        .fullName("Ibrahim Khan")
                        .fatherName("Sadiq Khan")
                        .motherName("Rabia Khatun")
                        .dateOfBirth(LocalDate.of(2011, 8, 22))
                        .className("Hifz - Advanced")
                        .guardianPhone("+8801711000002")
                        .guardianEmail("guardian2@example.com")
                        .address("Uttara, Dhaka")
                        .admissionDate(LocalDate.of(2024, 6, 1))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1800)
                        .build(),

                Student.builder()
                        .rollNo("2026-003")
                        .fullName("Aisha Siddiqua")
                        .fatherName("Hamid Siddiqui")
                        .motherName("Khadija Begum")
                        .dateOfBirth(LocalDate.of(2013, 2, 9))
                        .className("Nazira")
                        .guardianPhone("+8801711000003")
                        .guardianEmail("guardian3@example.com")
                        .address("Gulshan, Dhaka")
                        .admissionDate(LocalDate.of(2025, 3, 10))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1200)
                        .build(),

                Student.builder()
                        .rollNo("2026-004")
                        .fullName("Bilal Hossain")
                        .fatherName("Karim Hossain")
                        .motherName("Salma Akter")
                        .dateOfBirth(LocalDate.of(2010, 11, 30))
                        .className("Aalim - Year 1")
                        .guardianPhone("+8801711000004")
                        .guardianEmail("guardian4@example.com")
                        .address("Dhanmondi, Dhaka")
                        .admissionDate(LocalDate.of(2024, 1, 20))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(2000)
                        .build(),

                Student.builder()
                        .rollNo("2026-005")
                        .fullName("Maryam Akhtar")
                        .fatherName("Rashid Akhtar")
                        .motherName("Nasreen Begum")
                        .dateOfBirth(LocalDate.of(2014, 5, 18))
                        .className("Hifz - Beginner")
                        .guardianPhone("+8801711000005")
                        .guardianEmail("guardian5@example.com")
                        .address("Bashundhara, Dhaka")
                        .admissionDate(LocalDate.of(2026, 1, 8))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1200)
                        .build(),

                Student.builder()
                        .rollNo("2026-006")
                        .fullName("Omar Faruk")
                        .fatherName("Anwar Faruk")
                        .motherName("Halima Khatun")
                        .dateOfBirth(LocalDate.of(2012, 9, 4))
                        .className("Hifz - Intermediate")
                        .guardianPhone("+8801711000006")
                        .guardianEmail("guardian6@example.com")
                        .address("Mohammadpur, Dhaka")
                        .admissionDate(LocalDate.of(2025, 2, 14))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1500)
                        .build(),

                Student.builder()
                        .rollNo("2026-007")
                        .fullName("Khadija Sultana")
                        .fatherName("Ismail Sultan")
                        .motherName("Mariam Akter")
                        .dateOfBirth(LocalDate.of(2013, 7, 21))
                        .className("Nazira")
                        .guardianPhone("+8801711000007")
                        .guardianEmail("guardian7@example.com")
                        .address("Banani, Dhaka")
                        .admissionDate(LocalDate.of(2025, 4, 5))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1200)
                        .build(),

                Student.builder()
                        .rollNo("2026-008")
                        .fullName("Hamza Iqbal")
                        .fatherName("Tariq Iqbal")
                        .motherName("Sabina Yasmin")
                        .dateOfBirth(LocalDate.of(2011, 12, 12))
                        .className("Hifz - Advanced")
                        .guardianPhone("+8801711000008")
                        .guardianEmail("guardian8@example.com")
                        .address("Mirpur, Dhaka")
                        .admissionDate(LocalDate.of(2024, 8, 19))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1800)
                        .build(),

                Student.builder()
                        .rollNo("2026-009")
                        .fullName("Zainab Tabassum")
                        .fatherName("Mahmud Hasan")
                        .motherName("Ruma Khatun")
                        .dateOfBirth(LocalDate.of(2010, 6, 27))
                        .className("Aalim - Year 2")
                        .guardianPhone("+8801711000009")
                        .guardianEmail("guardian9@example.com")
                        .address("Lalmatia, Dhaka")
                        .admissionDate(LocalDate.of(2023, 9, 12))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(2200)
                        .build(),

                Student.builder()
                        .rollNo("2026-010")
                        .fullName("Ali Hasan")
                        .fatherName("Jamil Hasan")
                        .motherName("Shahnaz Begum")
                        .dateOfBirth(LocalDate.of(2012, 1, 15))
                        .className("Hifz - Intermediate")
                        .guardianPhone("+8801711000010")
                        .guardianEmail("guardian10@example.com")
                        .address("Tejgaon, Dhaka")
                        .admissionDate(LocalDate.of(2025, 1, 22))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1500)
                        .build(),

                Student.builder()
                        .rollNo("2026-011")
                        .fullName("Safiya Noor")
                        .fatherName("Imran Noor")
                        .motherName("Tasnim Akter")
                        .dateOfBirth(LocalDate.of(2013, 10, 3))
                        .className("Hifz - Beginner")
                        .guardianPhone("+8801711000011")
                        .guardianEmail("guardian11@example.com")
                        .address("Mirpur, Dhaka")
                        .admissionDate(LocalDate.of(2025, 8, 30))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1200)
                        .build(),

                Student.builder()
                        .rollNo("2026-012")
                        .fullName("Abdullah Saif")
                        .fatherName("Saiful Islam")
                        .motherName("Nazma Begum")
                        .dateOfBirth(LocalDate.of(2009, 3, 25))
                        .className("Aalim - Year 3")
                        .guardianPhone("+8801711000012")
                        .guardianEmail("guardian12@example.com")
                        .address("Uttara, Dhaka")
                        .admissionDate(LocalDate.of(2022, 7, 1))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(2500)
                        .build()
        );

        studentRepository.saveAll(sampleStudents);
        log.info("✅ Created {} sample students", sampleStudents.size());
    }
}