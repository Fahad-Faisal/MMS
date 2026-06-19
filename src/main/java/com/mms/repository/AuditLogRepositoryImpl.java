// src/main/java/com/mms/repository/AuditLogRepositoryImpl.java
package com.mms.repository;

import com.mms.entity.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<AuditLog> searchAuditLogs(String actor, String entity, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLog> countRoot = countQuery.from(AuditLog.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, actor, entity, from, to));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        query.select(root);
        query.where(buildPredicates(cb, root, actor, entity, from, to));
        query.orderBy(cb.desc(root.get("at")));

        TypedQuery<AuditLog> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<AuditLog> items = typedQuery.getResultList();
        return new PageImpl<>(items, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<AuditLog> root,
                                        String actor, String entity,
                                        LocalDateTime from, LocalDateTime to) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(actor)) {
            predicates.add(cb.like(cb.lower(root.get("actorName")), "%" + actor.toLowerCase() + "%"));
        }

        if (StringUtils.hasText(entity)) {
            predicates.add(cb.equal(root.get("entity"), entity));
        }

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("at"), from));
        }

        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("at"), to));
        }

        return predicates.toArray(new Predicate[0]);
    }
}