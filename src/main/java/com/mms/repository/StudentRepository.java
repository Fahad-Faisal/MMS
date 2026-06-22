// src/main/java/com/mms/repository/StudentRepository.java
package com.mms.repository;

import com.mms.entity.Student;
import com.mms.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByRollNo(String rollNo);
    boolean existsByRollNo(String rollNo);

    @Query("SELECT s FROM Student s WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(CAST(s.fullName AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(s.rollNo AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(s.guardianPhone AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:className IS NULL OR :className = '' OR s.className = :className) AND " +
            "(:classroomId IS NULL OR :classroomId = '' OR s.classroomId = :classroomId) AND " +
            "(:status IS NULL OR s.status = :status)")
    Page<Student> searchStudents(@Param("search") String search,
                                 @Param("className") String className,
                                 @Param("classroomId") String classroomId,
                                 @Param("status") StudentStatus status,
                                 Pageable pageable);

    List<Student> findByStatus(StudentStatus status);


    // NEW: Find students by class who are NOT enrolled in any classroom
    @Query("SELECT s FROM Student s WHERE " +
            "s.className = :className AND " +
            "(s.classroomId IS NULL OR s.classroomId = '') AND " +
            "s.status = :status")
    List<Student> findAvailableStudentsForClass(@Param("className") String className,
                                                @Param("status") StudentStatus status);

    // NEW: Find students by class who are enrolled in a specific classroom
    @Query("SELECT s FROM Student s WHERE " +
            "s.className = :className AND " +
            "s.classroomId = :classroomId AND " +
            "s.status = :status")
    List<Student> findStudentsByClassAndClassroom(@Param("className") String className,
                                                  @Param("classroomId") String classroomId,
                                                  @Param("status") StudentStatus status);

    // NEW: Find students by class excluding those in a specific classroom
    @Query("SELECT s FROM Student s WHERE " +
            "s.className = :className AND " +
            "(s.classroomId IS NULL OR s.classroomId != :classroomId) AND " +
            "s.status = :status")
    List<Student> findAvailableStudentsForClassExcludingClassroom(
            @Param("className") String className,
            @Param("classroomId") String classroomId,
            @Param("status") StudentStatus status);
}