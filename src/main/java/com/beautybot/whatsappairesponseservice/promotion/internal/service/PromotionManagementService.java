package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.CreatePromotionCommand;
import com.beautybot.whatsappairesponseservice.promotion.PromotionManagement;
import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.PromotionView;
import com.beautybot.whatsappairesponseservice.promotion.UpdatePromotionCommand;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEventEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.exception.InvalidPromotionException;
import com.beautybot.whatsappairesponseservice.promotion.internal.exception.PromotionConflictException;
import com.beautybot.whatsappairesponseservice.promotion.internal.exception.PromotionNotFoundException;
import com.beautybot.whatsappairesponseservice.promotion.internal.model.PromotionEventType;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionEventRepository;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
class PromotionManagementService implements PromotionManagement {

    private static final int MAX_ALIASES = 30;
    private static final int MAX_MESSAGE_LENGTH = 1800;

    private final PromotionRepository promotionRepository;
    private final PromotionEventRepository promotionEventRepository;
    private final PromotionMapper promotionMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionView> search(Long clinicId, PromotionStatus status, String query, Pageable pageable) {
        requireClinicId(clinicId);
        String normalizedQuery = hasText(query) ? query.trim() : null;
        Instant now = clock.instant();
        return promotionRepository.search(clinicId, status, normalizedQuery, pageable)
                .map(entity -> promotionMapper.toView(entity, now));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionView getById(Long clinicId, Long promotionId) {
        return promotionMapper.toView(findRequired(clinicId, promotionId), clock.instant());
    }

    @Override
    public PromotionView create(Long clinicId, CreatePromotionCommand command, String actor) {
        requireClinicId(clinicId);
        if (command == null) {
            throw new InvalidPromotionException("Promotion payload is required.");
        }
        ValidatedPromotion validated = validate(
                command.code(),
                command.title(),
                command.messageBody(),
                command.aliases(),
                command.validFrom(),
                command.validUntil()
        );
        if (promotionRepository.existsByClinicIdAndCode(clinicId, validated.code())) {
            throw new PromotionConflictException("A promotion with code '" + validated.code() + "' already exists.");
        }

        Instant now = clock.instant();
        String normalizedActor = normalizeActor(actor);
        PromotionEntity entity = PromotionEntity.builder()
                .clinicId(clinicId)
                .code(validated.code())
                .title(validated.title())
                .messageBody(validated.messageBody())
                .aliases(validated.aliases())
                .status(PromotionStatus.DRAFT)
                .validFrom(validated.validFrom())
                .validUntil(validated.validUntil())
                .createdBy(normalizedActor)
                .updatedBy(normalizedActor)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PromotionEntity saved = save(entity);
        saveEvent(saved, PromotionEventType.CREATED, "Promotion created.", normalizedActor);
        return promotionMapper.toView(saved, now);
    }

    @Override
    public PromotionView update(
            Long clinicId,
            Long promotionId,
            UpdatePromotionCommand command,
            String actor
    ) {
        if (command == null) {
            throw new InvalidPromotionException("Promotion payload is required.");
        }
        PromotionEntity entity = findRequired(clinicId, promotionId);
        requireVersion(entity, command.version());
        ValidatedPromotion validated = validate(
                command.code(),
                command.title(),
                command.messageBody(),
                command.aliases(),
                command.validFrom(),
                command.validUntil()
        );
        if (promotionRepository.existsByClinicIdAndCodeAndIdNot(clinicId, validated.code(), promotionId)) {
            throw new PromotionConflictException("A promotion with code '" + validated.code() + "' already exists.");
        }

        String normalizedActor = normalizeActor(actor);
        Instant now = clock.instant();
        entity.setCode(validated.code());
        entity.setTitle(validated.title());
        entity.setMessageBody(validated.messageBody());
        entity.setAliases(validated.aliases());
        entity.setValidFrom(validated.validFrom());
        entity.setValidUntil(validated.validUntil());
        entity.setUpdatedBy(normalizedActor);
        entity.setUpdatedAt(now);

        PromotionEntity saved = save(entity);
        saveEvent(saved, PromotionEventType.UPDATED, "Promotion updated.", normalizedActor);
        return promotionMapper.toView(saved, now);
    }

    @Override
    public PromotionView activate(Long clinicId, Long promotionId, Long version, String actor) {
        PromotionEntity entity = findRequired(clinicId, promotionId);
        requireVersion(entity, version);
        if (entity.getValidUntil() != null && !clock.instant().isBefore(entity.getValidUntil())) {
            throw new InvalidPromotionException("An expired promotion cannot be activated.");
        }
        return changeStatus(entity, PromotionStatus.ACTIVE, PromotionEventType.ACTIVATED, actor);
    }

    @Override
    public PromotionView archive(Long clinicId, Long promotionId, Long version, String actor) {
        PromotionEntity entity = findRequired(clinicId, promotionId);
        requireVersion(entity, version);
        return changeStatus(entity, PromotionStatus.ARCHIVED, PromotionEventType.ARCHIVED, actor);
    }

    private PromotionView changeStatus(
            PromotionEntity entity,
            PromotionStatus status,
            PromotionEventType eventType,
            String actor
    ) {
        String normalizedActor = normalizeActor(actor);
        Instant now = clock.instant();
        entity.setStatus(status);
        entity.setUpdatedBy(normalizedActor);
        entity.setUpdatedAt(now);
        PromotionEntity saved = save(entity);
        saveEvent(saved, eventType, "Promotion status changed to " + status + ".", normalizedActor);
        return promotionMapper.toView(saved, now);
    }

    private PromotionEntity findRequired(Long clinicId, Long promotionId) {
        requireClinicId(clinicId);
        if (promotionId == null) {
            throw new PromotionNotFoundException();
        }
        return promotionRepository.findByClinicIdAndId(clinicId, promotionId)
                .orElseThrow(PromotionNotFoundException::new);
    }

    private PromotionEntity save(PromotionEntity entity) {
        try {
            return promotionRepository.saveAndFlush(entity);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new PromotionConflictException("The promotion was modified by another user.", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new PromotionConflictException("The promotion conflicts with existing data.", exception);
        }
    }

    private void saveEvent(
            PromotionEntity promotion,
            PromotionEventType eventType,
            String description,
            String actor
    ) {
        promotionEventRepository.save(PromotionEventEntity.builder()
                .promotionId(promotion.getId())
                .clinicId(promotion.getClinicId())
                .eventType(eventType)
                .description(description)
                .createdBy(actor)
                .createdAt(clock.instant())
                .build());
    }

    private ValidatedPromotion validate(
            String code,
            String title,
            String messageBody,
            Set<String> aliases,
            Instant validFrom,
            Instant validUntil
    ) {
        String normalizedCode = PromotionTextNormalizer.normalizeCode(code);
        if (!normalizedCode.matches("[a-z0-9][a-z0-9-]{1,49}")) {
            throw new InvalidPromotionException("Code must contain 2 to 50 lowercase letters, numbers or dashes.");
        }
        String normalizedTitle = requireText(title, "Title", 120);
        String normalizedBody = requireText(messageBody, "Message body", MAX_MESSAGE_LENGTH);
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new InvalidPromotionException("validUntil must be after validFrom.");
        }

        Map<String, String> aliasesByNormalizedValue = new LinkedHashMap<>();
        if (aliases != null) {
            for (String alias : aliases) {
                String trimmed = requireText(alias, "Alias", 80);
                String normalizedAlias = PromotionTextNormalizer.normalizePhrase(trimmed);
                if (normalizedAlias.length() < 2) {
                    throw new InvalidPromotionException("Aliases must contain at least 2 characters.");
                }
                aliasesByNormalizedValue.putIfAbsent(normalizedAlias, trimmed);
            }
        }
        if (aliasesByNormalizedValue.size() > MAX_ALIASES) {
            throw new InvalidPromotionException("A promotion cannot have more than " + MAX_ALIASES + " aliases.");
        }
        Set<String> normalizedAliases = aliasesByNormalizedValue.values().stream()
                .sorted(Comparator.comparing(String::toLowerCase))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        return new ValidatedPromotion(
                normalizedCode,
                normalizedTitle,
                normalizedBody,
                normalizedAliases,
                validFrom,
                validUntil
        );
    }

    private void requireVersion(PromotionEntity entity, Long requestedVersion) {
        if (requestedVersion == null || !requestedVersion.equals(entity.getVersion())) {
            throw new PromotionConflictException("The promotion was modified by another user. Reload and try again.");
        }
    }

    private String requireText(String value, String field, int maxLength) {
        if (!hasText(value)) {
            throw new InvalidPromotionException(field + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidPromotionException(field + " cannot exceed " + maxLength + " characters.");
        }
        return trimmed;
    }

    private void requireClinicId(Long clinicId) {
        if (clinicId == null || clinicId <= 0) {
            throw new InvalidPromotionException("clinicId is required.");
        }
    }

    private String normalizeActor(String actor) {
        if (!hasText(actor)) {
            return "system";
        }
        String trimmed = actor.trim();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ValidatedPromotion(
            String code,
            String title,
            String messageBody,
            Set<String> aliases,
            Instant validFrom,
            Instant validUntil
    ) {
    }
}
