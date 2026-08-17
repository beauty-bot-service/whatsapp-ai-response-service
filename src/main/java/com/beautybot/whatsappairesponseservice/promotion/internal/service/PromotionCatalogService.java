package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.PromotionSummary;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PromotionCatalogService implements PromotionCatalog {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final Clock clock;

    @Override
    public List<PromotionSummary> findActive(Long clinicId) {
        return activeEntities(clinicId).stream().map(promotionMapper::toSummary).toList();
    }

    @Override
    public List<PromotionContent> match(Long clinicId, String message) {
        String normalizedMessage = PromotionTextNormalizer.normalizePhrase(message);
        if (normalizedMessage.isBlank()) {
            return List.of();
        }

        String paddedMessage = " " + normalizedMessage + " ";
        List<MatchedPromotion> matches = new ArrayList<>();
        for (PromotionEntity promotion : activeEntities(clinicId)) {
            int firstPosition = firstMatchPosition(paddedMessage, searchableTerms(promotion));
            if (firstPosition >= 0) {
                matches.add(new MatchedPromotion(promotion, firstPosition));
            }
        }

        return matches.stream()
                .sorted(Comparator.comparingInt(MatchedPromotion::position)
                        .thenComparing(match -> match.promotion().getCode()))
                .map(MatchedPromotion::promotion)
                .map(promotionMapper::toContent)
                .toList();
    }

    @Override
    public List<PromotionContent> findActiveByCodes(Long clinicId, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> normalizedCodes = codes.stream()
                .map(PromotionTextNormalizer::normalizeCode)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return List.of();
        }

        Instant now = clock.instant();
        Map<String, PromotionEntity> byCode = promotionRepository
                .findByClinicIdAndStatusAndCodeIn(clinicId, PromotionStatus.ACTIVE, normalizedCodes)
                .stream()
                .filter(entity -> isEffectiveAt(entity, now))
                .collect(Collectors.toMap(
                        PromotionEntity::getCode,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return normalizedCodes.stream()
                .map(byCode::get)
                .filter(entity -> entity != null)
                .map(promotionMapper::toContent)
                .toList();
    }

    static boolean isEffectiveAt(PromotionEntity entity, Instant now) {
        if (entity.getStatus() != PromotionStatus.ACTIVE) {
            return false;
        }
        if (entity.getValidFrom() != null && entity.getValidFrom().isAfter(now)) {
            return false;
        }
        return entity.getValidUntil() == null || now.isBefore(entity.getValidUntil());
    }

    private List<PromotionEntity> activeEntities(Long clinicId) {
        if (clinicId == null) {
            return List.of();
        }
        Instant now = clock.instant();
        return promotionRepository.findByClinicIdAndStatusOrderByTitleAsc(clinicId, PromotionStatus.ACTIVE)
                .stream()
                .filter(entity -> isEffectiveAt(entity, now))
                .toList();
    }

    private Set<String> searchableTerms(PromotionEntity entity) {
        Set<String> terms = new LinkedHashSet<>();
        terms.add(entity.getCode().replace('-', ' '));
        if (entity.getAliases() != null) {
            terms.addAll(entity.getAliases());
        }
        return terms;
    }

    private int firstMatchPosition(String paddedMessage, Set<String> terms) {
        int first = Integer.MAX_VALUE;
        for (String term : terms) {
            String normalizedTerm = PromotionTextNormalizer.normalizePhrase(term);
            if (normalizedTerm.isBlank()) {
                continue;
            }
            int position = paddedMessage.indexOf(" " + normalizedTerm + " ");
            if (position >= 0) {
                first = Math.min(first, position);
            }
        }
        return first == Integer.MAX_VALUE ? -1 : first;
    }

    private record MatchedPromotion(PromotionEntity promotion, int position) {
    }
}
