package com.beautybot.whatsappairesponseservice.promotion.internal.repository;

import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {

    Optional<PromotionEntity> findByClinicIdAndId(Long clinicId, Long id);

    boolean existsByClinicIdAndCode(Long clinicId, String code);

    boolean existsByClinicIdAndCodeAndIdNot(Long clinicId, String code, Long id);

    @Query("""
            select p from PromotionModulePromotion p
            where p.clinicId = :clinicId
              and (:status is null or p.status = :status)
              and (:query is null
                   or lower(p.code) like lower(concat('%', :query, '%'))
                   or lower(p.title) like lower(concat('%', :query, '%')))
            """)
    Page<PromotionEntity> search(
            @Param("clinicId") Long clinicId,
            @Param("status") PromotionStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "aliases")
    List<PromotionEntity> findByClinicIdAndStatusOrderByTitleAsc(Long clinicId, PromotionStatus status);

    @EntityGraph(attributePaths = "aliases")
    List<PromotionEntity> findByClinicIdAndStatusAndCodeIn(
            Long clinicId,
            PromotionStatus status,
            Collection<String> codes
    );
}
