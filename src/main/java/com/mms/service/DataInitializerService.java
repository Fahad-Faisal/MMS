// src/main/java/com/mms/service/DataInitializerService.java
package com.mms.service;

import com.mms.entity.*;
import com.mms.enums.*;
import com.mms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerService implements CommandLineRunner {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceRepository attendanceRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final ReceiptRepository receiptRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.data.initialize:true}")
    private boolean initializeData;

    @Value("${app.data.force-initialize:false}")
    private boolean forceInitialize;

    private static final String[] CLASS_NAMES = {
            "Hifz - Beginner",
            "Hifz - Intermediate",
            "Hifz - Advanced",
            "Nazira",
            "Aalim - Year 1",
            "Aalim - Year 2",
            "Aalim - Year 3"
    };

    private int studentCounter = 0;

    @Override
    @Transactional
    public void run(String... args) {
        if (!initializeData) {
            log.info("Data initialization is disabled. Set app.data.initialize=true to enable.");
            return;
        }

        // Check if data already exists
        if (!forceInitialize && userRepository.count() > 0 && studentRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            log.info("To force re-initialization, set app.data.force-initialize=true");
            return;
        }

        // If force initialize, clean data first
        if (forceInitialize) {
            log.warn("⚠️ Force initialization enabled - clearing existing data...");
            clearAllData();
        }

        log.info("🚀 Starting data initialization...");

        try {
            initializeUsers();
            initializeClassrooms();
            initializeStudents();
            initializeAttendance();
            initializeLedgerAndReceipts();
            initializeAuditLogs();

            log.info("✅ Data initialization completed successfully!");
            logInitializationSummary();
        } catch (Exception e) {
            log.error("❌ Data initialization failed!", e);
            throw new RuntimeException("Failed to initialize data", e);
        }
    }

    @Transactional
    public void clearAllData() {
        log.info("Clearing all existing data...");

        // Delete in correct order to avoid foreign key constraints
        receiptRepository.deleteAll();
        ledgerRepository.deleteAll();
        attendanceRepository.deleteAll();
        auditLogRepository.deleteAll();

        // Clear student documents and students
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            student.getDocuments().clear();
        }
        studentRepository.deleteAll();

        classroomRepository.deleteAll();
        userRepository.deleteAll();

        log.info("All data cleared successfully.");
    }

    @Transactional
    public void initializeUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping user initialization.");
            return;
        }

        log.info("📝 Creating default users...");

        String defaultPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(defaultPassword);

        List<User> defaultUsers = Arrays.asList(
                User.builder()
                        .username("admin")
                        .fullName("Ahmed Hossain")
                        .email("admin@madrasa.edu")
                        .passwordHash(encodedPassword)
                        .role(Role.ADMINISTRATOR)
                        .active(true)
                        .twoFactorEnabled(false)
                        .bio("System Administrator")
                        .build(),

                User.builder()
                        .username("muhtamim")
                        .fullName("Mufti Abdul Karim")
                        .email("muhtamim@madrasa.edu")
                        .passwordHash(encodedPassword)
                        .role(Role.MUHTAMIM)
                        .active(true)
                        .twoFactorEnabled(false)
                        .bio("Principal of the Madrasa")
                        .build(),

                User.builder()
                        .username("teacher")
                        .fullName("Maulana Yusuf Ali")
                        .email("teacher@madrasa.edu")
                        .passwordHash(encodedPassword)
                        .role(Role.INSTRUCTOR)
                        .active(true)
                        .twoFactorEnabled(false)
                        .bio("Senior Instructor")
                        .build(),

                User.builder()
                        .username("accountant")
                        .fullName("Fatima Khatun")
                        .email("accountant@madrasa.edu")
                        .passwordHash(encodedPassword)
                        .role(Role.ACCOUNTANT)
                        .active(true)
                        .twoFactorEnabled(false)
                        .bio("Finance Officer")
                        .build()
        );

        userRepository.saveAll(defaultUsers);
        log.info("✅ Created {} default users", defaultUsers.size());
        log.info("   Default password for all users: admin123");
    }

    @Transactional
    public void initializeClassrooms() {
        if (classroomRepository.count() > 0) {
            log.info("Classrooms already exist. Skipping classroom initialization.");
            return;
        }

        log.info("📝 Creating default classrooms/sections...");

        // Get teacher user for assignment
        User teacher = userRepository.findByUsername("teacher").orElse(null);
        String teacherId = teacher != null ? teacher.getId() : null;
        String teacherName = teacher != null ? teacher.getFullName() : null;

        List<Classroom> classrooms = new ArrayList<>();

        // Create 2 sections per class
        for (String className : CLASS_NAMES) {
            // Section A
            classrooms.add(Classroom.builder()
                    .name(className + " - Section A")
                    .className(className)
                    .teacherId(teacherId)
                    .teacherName(teacherName)
                    .capacity(30)
                    .studentIds(new ArrayList<>())
                    .build());

            // Section B
            classrooms.add(Classroom.builder()
                    .name(className + " - Section B")
                    .className(className)
                    .teacherId(null)
                    .teacherName(null)
                    .capacity(25)
                    .studentIds(new ArrayList<>())
                    .build());
        }

        classroomRepository.saveAll(classrooms);
        log.info("✅ Created {} classrooms/sections", classrooms.size());
    }

    @Transactional
    public void initializeStudents() {
        if (studentRepository.count() > 0) {
            log.info("Students already exist. Skipping student initialization.");
            return;
        }

        log.info("📝 Creating sample students...");

        List<Classroom> classrooms = classroomRepository.findAll();
        Map<String, List<String>> classroomMap = new HashMap<>();

        // Group classroom IDs by class name
        for (Classroom classroom : classrooms) {
            classroomMap.computeIfAbsent(classroom.getClassName(), k -> new ArrayList<>())
                    .add(classroom.getId());
        }

        List<Student> students = new ArrayList<>();
        Random random = new Random();

        // Reset counter
        studentCounter = 0;

        // Create students per class with unique roll numbers
        for (String className : CLASS_NAMES) {
            List<String> classroomIds = classroomMap.getOrDefault(className, new ArrayList<>());
            int studentCount = 15 + random.nextInt(6); // 15-20 students per class

            for (int i = 1; i <= studentCount; i++) {
                studentCounter++;

                // Generate unique roll number using counter
                String rollNo = String.format("%s-%03d",
                        getClassCode(className),
                        studentCounter);

                // Assign to a classroom (distribute evenly)
                String classroomId = null;
                if (!classroomIds.isEmpty()) {
                    int index = (i - 1) % classroomIds.size();
                    classroomId = classroomIds.get(index);
                }

                students.add(Student.builder()
                        .rollNo(rollNo)
                        .fullName(generateRandomName())
                        .fatherName(generateRandomFatherName())
                        .motherName(generateRandomMotherName())
                        .dateOfBirth(LocalDate.of(
                                2010 + random.nextInt(6),
                                1 + random.nextInt(12),
                                1 + random.nextInt(28)
                        ))
                        .className(className)
                        .classroomId(classroomId)
                        .guardianPhone("+88017" + String.format("%07d", 1000000 + random.nextInt(9000000)))
                        .guardianEmail("guardian" + studentCounter + "@example.com")
                        .address(generateRandomAddress())
                        .admissionDate(LocalDate.of(
                                2024 + random.nextInt(2),
                                1 + random.nextInt(12),
                                1 + random.nextInt(28)
                        ))
                        .status(StudentStatus.ACTIVE)
                        .monthlyFee(1000 + random.nextInt(2000))
                        .photoUrl(null)
                        .documents(new ArrayList<>())
                        .build());
            }
        }

        studentRepository.saveAll(students);
        log.info("✅ Created {} sample students", students.size());

        // Update classroom student lists
        for (Student student : students) {
            if (student.getClassroomId() != null) {
                Classroom classroom = classroomRepository.findById(student.getClassroomId()).orElse(null);
                if (classroom != null && !classroom.getStudentIds().contains(student.getId())) {
                    classroom.getStudentIds().add(student.getId());
                    classroomRepository.save(classroom);
                }
            }
        }
    }

    @Transactional
    public void initializeAttendance() {
        if (attendanceRepository.count() > 0) {
            log.info("Attendance records already exist. Skipping attendance initialization.");
            return;
        }

        log.info("📝 Creating sample attendance records...");

        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            log.warn("No students found. Skipping attendance initialization.");
            return;
        }

        List<Attendance> attendanceRecords = new ArrayList<>();
        Random random = new Random();

        LocalDate today = LocalDate.now();

        // Create attendance for last 3 days
        for (int day = 0; day < 3; day++) {
            LocalDate date = today.minusDays(day);

            for (Student student : students) {
                // Skip some students randomly
                if (random.nextInt(100) < 10) continue;

                // FAJR session
                attendanceRecords.add(Attendance.builder()
                        .studentId(student.getId())
                        .date(date)
                        .session(AttendanceSession.FAJR)
                        .status(getRandomAttendanceStatus(random))
                        .note(random.nextInt(100) < 15 ? "Note: " + getRandomNote() : null)
                        .recordedBy("system")
                        .recordedAt(LocalDateTime.now())
                        .build());

                // AFTERNOON session
                if (random.nextInt(100) < 80) { // 80% attendance for afternoon
                    attendanceRecords.add(Attendance.builder()
                            .studentId(student.getId())
                            .date(date)
                            .session(AttendanceSession.AFTERNOON)
                            .status(getRandomAttendanceStatus(random))
                            .note(random.nextInt(100) < 10 ? "Note: " + getRandomNote() : null)
                            .recordedBy("system")
                            .recordedAt(LocalDateTime.now())
                            .build());
                }
            }
        }

        // Save in batches to avoid memory issues
        if (!attendanceRecords.isEmpty()) {
            int batchSize = 100;
            for (int i = 0; i < attendanceRecords.size(); i += batchSize) {
                int end = Math.min(i + batchSize, attendanceRecords.size());
                attendanceRepository.saveAll(attendanceRecords.subList(i, end));
            }
            log.info("✅ Created {} sample attendance records", attendanceRecords.size());
        } else {
            log.info("No attendance records created.");
        }
    }

    @Transactional
    public void initializeLedgerAndReceipts() {
        if (ledgerRepository.count() > 0 || receiptRepository.count() > 0) {
            log.info("Ledger/receipt records already exist. Skipping initialization.");
            return;
        }

        log.info("📝 Creating sample ledger entries and receipts...");

        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            log.warn("No students found. Skipping ledger/receipt initialization.");
            return;
        }

        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        List<Receipt> receipts = new ArrayList<>();
        Random random = new Random();

        String accountantName = userRepository.findByUsername("accountant")
                .map(User::getFullName)
                .orElse("Accountant");

        LocalDate today = LocalDate.now();

        // Create ledger for last 3 months
        for (int month = 2; month >= 0; month--) {
            LocalDate monthDate = today.minusMonths(month);
            String monthLabel = monthDate.getMonth().toString() + " " + monthDate.getYear();

            for (Student student : students) {
                // Invoice (debit)
                LedgerEntry invoice = LedgerEntry.builder()
                        .studentId(student.getId())
                        .type(LedgerEntryType.INVOICE)
                        .amount(student.getMonthlyFee())
                        .description(monthLabel + " tuition")
                        .createdBy("system")
                        .createdAt(monthDate.withDayOfMonth(1).atStartOfDay())
                        .build();
                ledgerEntries.add(invoice);

                // Payment (credit) - 80% of students pay
                if (random.nextInt(100) < 80) {
                    int paidAmount = student.getMonthlyFee();
                    if (random.nextInt(100) < 20) { // 20% pay partial
                        paidAmount = student.getMonthlyFee() - random.nextInt(500);
                        if (paidAmount < 0) paidAmount = student.getMonthlyFee() / 2;
                    }

                    // Create payment ledger entry
                    String receiptNo = String.format("R-%d-%06d",
                            monthDate.getYear(),
                            receipts.size() + 1);

                    LedgerEntry payment = LedgerEntry.builder()
                            .studentId(student.getId())
                            .type(LedgerEntryType.PAYMENT)
                            .amount(-paidAmount)
                            .description("Receipt " + receiptNo)
                            .referenceId("receipt-" + receipts.size())
                            .createdBy(accountantName)
                            .createdAt(monthDate.withDayOfMonth(7 + random.nextInt(10)).atStartOfDay())
                            .build();
                    ledgerEntries.add(payment);

                    // Create receipt
                    Receipt receipt = Receipt.builder()
                            .receiptNo(receiptNo)
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .amount(paidAmount)
                            .method(getRandomPaymentMethod(random))
                            .paidOn(monthDate.withDayOfMonth(7 + random.nextInt(10)))
                            .issuedBy(accountantName)
                            .note(monthLabel + " tuition payment")
                            .build();
                    receipts.add(receipt);
                }
            }
        }

        // Save in batches
        if (!ledgerEntries.isEmpty()) {
            int batchSize = 100;
            for (int i = 0; i < ledgerEntries.size(); i += batchSize) {
                int end = Math.min(i + batchSize, ledgerEntries.size());
                ledgerRepository.saveAll(ledgerEntries.subList(i, end));
            }
            log.info("✅ Created {} ledger entries", ledgerEntries.size());
        }

        if (!receipts.isEmpty()) {
            int batchSize = 100;
            for (int i = 0; i < receipts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, receipts.size());
                receiptRepository.saveAll(receipts.subList(i, end));
            }
            log.info("✅ Created {} receipts", receipts.size());
        }
    }

    @Transactional
    public void initializeAuditLogs() {
        if (auditLogRepository.count() > 0) {
            log.info("Audit logs already exist. Skipping audit log initialization.");
            return;
        }

        log.info("📝 Creating sample audit logs...");

        List<AuditLog> auditLogs = new ArrayList<>();
        Random random = new Random();

        List<String> actions = Arrays.asList(
                "LOGIN", "CREATE_STUDENT", "UPDATE_STUDENT", "DELETE_STUDENT",
                "CREATE_RECEIPT", "CREATE_LEDGER_ENTRY", "RUN_MONTHLY_INVOICES",
                "UPDATE_USER", "CREATE_USER", "SAVE_ATTENDANCE"
        );
        List<String> entities = Arrays.asList(
                "user", "student", "receipt", "ledger", "attendance"
        );

        // Create 50 audit logs
        for (int i = 0; i < 50; i++) {
            String action = actions.get(random.nextInt(actions.size()));
            String entity = entities.get(random.nextInt(entities.size()));

            auditLogs.add(AuditLog.builder()
                    .actorId("system")
                    .actorName(random.nextBoolean() ? "Ahmed Hossain" : "Fatima Khatun")
                    .action(action)
                    .entity(entity)
                    .entityId(UUID.randomUUID().toString().substring(0, 8))
                    .at(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .build());
        }

        auditLogRepository.saveAll(auditLogs);
        log.info("✅ Created {} sample audit logs", auditLogs.size());
    }

    // Helper methods
    private String getClassCode(String className) {
        // Map class names to short codes
        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("Hifz - Beginner", "HFB");
        codeMap.put("Hifz - Intermediate", "HFI");
        codeMap.put("Hifz - Advanced", "HFA");
        codeMap.put("Nazira", "NAZ");
        codeMap.put("Aalim - Year 1", "AL1");
        codeMap.put("Aalim - Year 2", "AL2");
        codeMap.put("Aalim - Year 3", "AL3");
        return codeMap.getOrDefault(className, className.substring(0, Math.min(4, className.length())).toUpperCase());
    }

    private AttendanceStatus getRandomAttendanceStatus(Random random) {
        int value = random.nextInt(100);
        if (value < 75) return AttendanceStatus.PRESENT;
        if (value < 85) return AttendanceStatus.ABSENT;
        if (value < 93) return AttendanceStatus.LATE;
        return AttendanceStatus.SICK;
    }

    private PaymentMethod getRandomPaymentMethod(Random random) {
        int value = random.nextInt(100);
        if (value < 40) return PaymentMethod.CASH;
        if (value < 70) return PaymentMethod.MOBILE;
        return PaymentMethod.BANK;
    }

    private String getRandomNote() {
        String[] notes = {
                "Came late",
                "Sick leave",
                "Family emergency",
                "Early dismissal",
                "Medical appointment"
        };
        Random random = new Random();
        return notes[random.nextInt(notes.length)];
    }

    private String generateRandomName() {
        String[] firstNames = {
                "Yusuf", "Ibrahim", "Aisha", "Bilal", "Maryam", "Omar", "Khadija", "Hamza",
                "Zainab", "Ali", "Fatima", "Hassan", "Hussein", "Amina", "Safiya", "Abdullah",
                "Rahman", "Karim", "Jamal", "Nadia", "Samira", "Tariq", "Layla", "Idris"
        };
        String[] lastNames = {
                "Rahman", "Khan", "Ali", "Hossain", "Siddiqui", "Akhtar", "Hasan", "Iqbal",
                "Begum", "Khatun", "Chowdhury", "Karim", "Islam", "Ahmed", "Miah"
        };
        Random random = new Random();
        return firstNames[random.nextInt(firstNames.length)] + " " +
                lastNames[random.nextInt(lastNames.length)];
    }

    private String generateRandomFatherName() {
        String[] names = {
                "Abdul Rahman", "Sadiq Khan", "Hamid Siddiqui", "Karim Hossain",
                "Rashid Akhtar", "Anwar Faruk", "Ismail Sultan", "Tariq Iqbal",
                "Mahmud Hasan", "Jamil Hasan", "Imran Noor", "Saiful Islam"
        };
        Random random = new Random();
        return names[random.nextInt(names.length)];
    }

    private String generateRandomMotherName() {
        String[] names = {
                "Ayesha Begum", "Rabia Khatun", "Khadija Begum", "Salma Akter",
                "Nasreen Begum", "Halima Khatun", "Mariam Akter", "Sabina Yasmin",
                "Ruma Khatun", "Shahnaz Begum", "Tasnim Akter", "Nazma Begum"
        };
        Random random = new Random();
        return names[random.nextInt(names.length)];
    }

    private String generateRandomAddress() {
        String[] areas = {
                "Mirpur", "Uttara", "Gulshan", "Dhanmondi", "Bashundhara",
                "Mohammadpur", "Banani", "Lalmatia", "Tejgaon", "Motijheel"
        };
        Random random = new Random();
        String area = areas[random.nextInt(areas.length)];
        int house = 1 + random.nextInt(50);
        int road = 1 + random.nextInt(20);
        return "House " + house + ", Road " + road + ", " + area + ", Dhaka";
    }

    private void logInitializationSummary() {
        log.info("📊 Data Initialization Summary:");
        log.info("   - Users: {}", userRepository.count());
        log.info("   - Students: {}", studentRepository.count());
        log.info("   - Classrooms: {}", classroomRepository.count());
        log.info("   - Attendance Records: {}", attendanceRepository.count());
        log.info("   - Ledger Entries: {}", ledgerRepository.count());
        log.info("   - Receipts: {}", receiptRepository.count());
        log.info("   - Audit Logs: {}", auditLogRepository.count());
    }
}