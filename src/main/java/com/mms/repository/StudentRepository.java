// src/main/java/com/mms/repository/StudentRepository.java
package com.mms.repository;

import com.mms.entity.Student;
import com.mms.enums.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String>, StudentRepositoryCustom {
    Optional<Student> findByRollNo(String rollNo);
    boolean existsByRollNo(String rollNo);
    List<Student> findByStatus(StudentStatus status);
}