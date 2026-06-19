// src/main/java/com/mms/repository/StudentRepositoryCustom.java
package com.mms.repository;

import com.mms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentRepositoryCustom {
    Page<Student> searchStudents(String search, String className, String status, Pageable pageable);
}