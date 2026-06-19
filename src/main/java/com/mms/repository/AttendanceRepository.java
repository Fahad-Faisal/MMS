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
    List<Attendance> findByDateAndSessionAndStudentIdIn(LocalDate date,
                                                        AttendanceSession session,
                                                        List<String> studentIds);

    Optional<Attendance> findByStudentIdAndDateAndSession(String studentId,
                                                          LocalDate date,
                                                          AttendanceSession session);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.date = :date AND a.session = :session")
    void deleteByDateAndSession(@Param("date") LocalDate date,
                                @Param("session") AttendanceSession session);
}
