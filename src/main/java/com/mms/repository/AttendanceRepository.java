// src/main/java/com/mms/repository/AttendanceRepository.java
package com.mms.repository;

import com.mms.entity.Attendance;
import com.mms.enums.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {

    // Get attendance by date and session with optional student IDs
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.session = :session AND a.studentId IN :studentIds")
    List<Attendance> findByDateAndSessionAndStudentIdIn(@Param("date") LocalDate date,
                                                        @Param("session") AttendanceSession session,
                                                        @Param("studentIds") List<String> studentIds);

    // Get attendance by date and session (without student filter)
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.session = :session")
    List<Attendance> findByDateAndSession(@Param("date") LocalDate date,
                                          @Param("session") AttendanceSession session);

    // Get all attendance for a date (both sessions)
    @Query("SELECT a FROM Attendance a WHERE a.date = :date")
    List<Attendance> findByDate(@Param("date") LocalDate date);

    Optional<Attendance> findByStudentIdAndDateAndSession(String studentId,
                                                          LocalDate date,
                                                          AttendanceSession session);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.date = :date AND a.session = :session")
    void deleteByDateAndSession(@Param("date") LocalDate date,
                                @Param("session") AttendanceSession session);
}