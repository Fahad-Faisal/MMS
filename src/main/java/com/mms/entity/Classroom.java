// src/main/java/com/mms/entity/Classroom.java
package com.mms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classrooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 50)
    private String className;

    @Column(name = "teacher_id")
    private String teacherId;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "capacity")
    private Integer capacity;  // NEW: Max students allowed

    @ElementCollection
    @CollectionTable(name = "classroom_students", joinColumns = @JoinColumn(name = "classroom_id"))
    @Column(name = "student_id")
    @Builder.Default
    private List<String> studentIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}