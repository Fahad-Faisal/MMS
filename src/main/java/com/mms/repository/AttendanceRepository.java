// src/main/java/com/mms/repository/AttendanceRepository.java
package com.mms.repository;

import com.mms.entity.Attendance;
import com.mms.enums.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {

    // Find by date and session
    List<Attendance> findByDateAndSession(LocalDate date, AttendanceSession session);

    // Find by date only
    List<Attendance> findByDate(LocalDate date);

    // Find by session only
    List<Attendance> findBySession(AttendanceSession session);

    // Find by date, session, and student IDs
    List<Attendance> findByDateAndSessionAndStudentIdIn(LocalDate date,
                                                        AttendanceSession session,
                                                        List<String> studentIds);

    // Find by date and student IDs (any session)
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.studentId IN :studentIds")
    List<Attendance> findByDateAndStudentIdIn(@Param("date") LocalDate date,
                                              @Param("studentIds") List<String> studentIds);

    // Find by student IDs (any date)
    @Query("SELECT a FROM Attendance a WHERE a.studentId IN :studentIds")
    List<Attendance> findByStudentIdIn(@Param("studentIds") List<String> studentIds);

    // Find specific student's attendance
    Optional<Attendance> findByStudentIdAndDateAndSession(String studentId,
                                                          LocalDate date,
                                                          AttendanceSession session);

    // Student attendance history
    List<Attendance> findByStudentIdOrderByDateDesc(String studentId);

    @Query("SELECT a FROM Attendance a WHERE a.studentId = :studentId AND a.date BETWEEN :from AND :to")
    List<Attendance> findByStudentIdAndDateBetween(@Param("studentId") String studentId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT a FROM Attendance a WHERE a.studentId = :studentId AND a.date >= :from")
    List<Attendance> findByStudentIdAndDateAfter(@Param("studentId") String studentId,
                                                 @Param("from") LocalDate from);

    @Query("SELECT a FROM Attendance a WHERE a.studentId = :studentId AND a.date <= :to")
    List<Attendance> findByStudentIdAndDateBefore(@Param("studentId") String studentId,
                                                  @Param("to") LocalDate to);
}