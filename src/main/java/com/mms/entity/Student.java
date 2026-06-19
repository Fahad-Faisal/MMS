package com.mms.entity;

import com.mms.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 20)
    private String rollNo;

    @Column(nullable = false, length = 80)
    private String fullName;

    @Column(length = 80)
    private String fatherName;

    @Column(length = 80)
    private String motherName;

    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 50)
    private String className;

    @Column(name = "classroom_id")
    private String classroomId;

    @Column(length = 20)
    private String guardianPhone;

    @Column(length = 254)
    private String guardianEmail;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private LocalDate admissionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    @Column(nullable = false)
    private Integer monthlyFee;

    private String photoUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "student_id")
    @Builder.Default
    private List<StudentDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
