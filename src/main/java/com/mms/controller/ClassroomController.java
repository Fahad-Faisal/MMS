package com.mms.controller;

import com.mms.dto.request.ClassroomRequest;
import com.mms.entity.Classroom;
import com.mms.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classrooms")
@RequiredArgsConstructor
public class ClassroomController {
    private final ClassroomService classroomService;

    @GetMapping
    public ResponseEntity<List<Classroom>> getClassrooms() {
        return ResponseEntity.ok(classroomService.getAllClassrooms());
    }

    @PostMapping
    public ResponseEntity<Classroom> createClassroom(@Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(classroomService.createClassroom(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Classroom> updateClassroom(@PathVariable String id,
                                                     @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(classroomService.updateClassroom(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(@PathVariable String id) {
        classroomService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
    }
}
