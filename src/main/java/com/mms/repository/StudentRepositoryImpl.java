// src/main/java/com/mms/repository/StudentRepositoryImpl.java
package com.mms.repository;

import com.mms.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StudentRepositoryImpl implements StudentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Student> searchStudents(String search, String className, String status, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Student> countRoot = countQuery.from(Student.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, search, className, status));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<Student> query = cb.createQuery(Student.class);
        Root<Student> root = query.from(Student.class);
        query.select(root);
        query.where(buildPredicates(cb, root, search, className, status));
        query.orderBy(cb.asc(root.get("fullName")));

        TypedQuery<Student> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Student> items = typedQuery.getResultList();
        return new PageImpl<>(items, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Student> root,
                                        String search, String className, String status) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(search)) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            Predicate namePredicate = cb.like(cb.lower(root.get("fullName")), searchPattern);
            Predicate rollPredicate = cb.like(cb.lower(root.get("rollNo")), searchPattern);
            Predicate phonePredicate = cb.like(cb.lower(root.get("guardianPhone")), searchPattern);
            predicates.add(cb.or(namePredicate, rollPredicate, phonePredicate));
        }

        if (StringUtils.hasText(className)) {
            predicates.add(cb.equal(root.get("className"), className));
        }

        if (StringUtils.hasText(status)) {
            predicates.add(cb.equal(root.get("status"), status));
        }

        return predicates.toArray(new Predicate[0]);
    }
}