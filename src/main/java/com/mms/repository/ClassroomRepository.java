package com.mms.repository;

import com.mms.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, String> {
    List<Classroom> findByTeacherId(String teacherId);
    List<Classroom> findByClassName(String className);
}
