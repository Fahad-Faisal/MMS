// src/main/java/com/mms/service/ClassroomService.java
package com.mms.service;

import com.mms.dto.request.ClassroomRequest;
import com.mms.entity.Classroom;
import com.mms.entity.Student;
import com.mms.exception.BusinessException;
import com.mms.exception.ResourceNotFoundException;
import com.mms.repository.ClassroomRepository;
import com.mms.repository.StudentRepository;
import com.mms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;


    public List<Classroom> getAllClassrooms() {
        return classroomRepository.findAll();
    }


    public Classroom getClassroom(String id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
    }

    @Transactional

    public Classroom createClassroom(ClassroomRequest request) {
        if (request.getCapacity() != null && request.getStudentIds() != null) {
            if (request.getStudentIds().size() > request.getCapacity()) {
                throw new BusinessException("Cannot enroll more students than capacity");
            }
        }

        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .className(request.getClassName())
                .teacherId(request.getTeacherId())
                .capacity(request.getCapacity())
                .studentIds(request.getStudentIds() != null ? request.getStudentIds() : List.of())
                .build();

        if (request.getTeacherId() != null) {
            userRepository.findById(request.getTeacherId())
                    .ifPresent(user -> classroom.setTeacherName(user.getFullName()));
        }

        Classroom saved = classroomRepository.save(classroom);

        if (request.getStudentIds() != null) {
            for (String studentId : request.getStudentIds()) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    student.setClassroomId(saved.getId());
                    studentRepository.save(student);
                });
            }
        }

        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "CREATE_CLASSROOM", "classroom", saved.getId());
        return saved;
    }

    @Transactional

    public Classroom updateClassroom(String id, ClassroomRequest request) {
        Classroom classroom = getClassroom(id);
        if (request.getCapacity() != null) {
            int currentStudents = classroom.getStudentIds().size();
            int newStudents = request.getStudentIds() != null ? request.getStudentIds().size() : currentStudents;
            if (newStudents > request.getCapacity()) {
                throw new BusinessException("Cannot update capacity - " +
                        newStudents + " students enrolled, capacity is " + request.getCapacity());
            }
            classroom.setCapacity(request.getCapacity());
        }
        if (request.getName() != null) classroom.setName(request.getName());
        if (request.getClassName() != null) classroom.setClassName(request.getClassName());
        if (request.getTeacherId() != null) {
            classroom.setTeacherId(request.getTeacherId());
            userRepository.findById(request.getTeacherId())
                    .ifPresent(user -> classroom.setTeacherName(user.getFullName()));
        }

        if (request.getStudentIds() != null) {
            // Remove old assignments
            for (String studentId : classroom.getStudentIds()) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    if (student.getClassroomId() != null && student.getClassroomId().equals(id)) {
                        student.setClassroomId(null);
                        studentRepository.save(student);
                    }
                });
            }

            // Add new assignments
            classroom.setStudentIds(request.getStudentIds());
            for (String studentId : request.getStudentIds()) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    student.setClassroomId(id);
                    studentRepository.save(student);
                });
            }
        }

        Classroom updated = classroomRepository.save(classroom);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "UPDATE_CLASSROOM", "classroom", id);
        return updated;
    }

    @Transactional

    public void deleteClassroom(String id) {
        Classroom classroom = getClassroom(id);

        // Clear student references
        for (String studentId : classroom.getStudentIds()) {
            studentRepository.findById(studentId).ifPresent(student -> {
                student.setClassroomId(null);
                studentRepository.save(student);
            });
        }

        classroomRepository.deleteById(id);
        auditService.logAudit(getCurrentUserId(), getCurrentUserName(), "DELETE_CLASSROOM", "classroom", id);
    }

    private String getCurrentUserId() {
        return "system";
    }

    private String getCurrentUserName() {
        return "system";
    }
    private void validateCapacity(Classroom classroom, List<String> newStudentIds) {
        if (classroom.getCapacity() != null && newStudentIds != null) {
            if (newStudentIds.size() > classroom.getCapacity()) {
                throw new BusinessException(
                        String.format("Cannot enroll %d students - capacity is %d",
                                newStudentIds.size(), classroom.getCapacity())
                );
            }
        }
    }
}