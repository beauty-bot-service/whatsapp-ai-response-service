package com.beautybot.whatsappairesponseservice.admin.promotion;

import com.beautybot.whatsappairesponseservice.admin.security.AdminPrincipal;
import com.beautybot.whatsappairesponseservice.promotion.CreatePromotionCommand;
import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import com.beautybot.whatsappairesponseservice.promotion.PromotionManagement;
import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.PromotionView;
import com.beautybot.whatsappairesponseservice.promotion.UpdatePromotionCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/promotions")
public class PromotionAdminController {

    private final PromotionManagement promotionManagement;
    private final PromotionCatalog promotionCatalog;

    @GetMapping
    public PromotionPageResponse search(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false, name = "q") String query,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return PromotionPageResponse.from(
                promotionManagement.search(principal.clinicId(), status, query, pageable)
        );
    }

    @GetMapping("/{promotionId}")
    public PromotionView getById(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long promotionId
    ) {
        return promotionManagement.getById(principal.clinicId(), promotionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionView create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody PromotionCreateRequest request
    ) {
        return promotionManagement.create(
                principal.clinicId(),
                new CreatePromotionCommand(
                        request.code(),
                        request.title(),
                        request.messageBody(),
                        request.aliases() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.aliases()),
                        request.validFrom(),
                        request.validUntil()
                ),
                principal.email()
        );
    }

    @PutMapping("/{promotionId}")
    public PromotionView update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionUpdateRequest request
    ) {
        return promotionManagement.update(
                principal.clinicId(),
                promotionId,
                new UpdatePromotionCommand(
                        request.version(),
                        request.code(),
                        request.title(),
                        request.messageBody(),
                        request.aliases() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.aliases()),
                        request.validFrom(),
                        request.validUntil()
                ),
                principal.email()
        );
    }

    @PostMapping("/{promotionId}/activate")
    public PromotionView activate(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionVersionRequest request
    ) {
        return promotionManagement.activate(principal.clinicId(), promotionId, request.version(), principal.email());
    }

    @PostMapping("/{promotionId}/archive")
    public PromotionView archive(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionVersionRequest request
    ) {
        return promotionManagement.archive(principal.clinicId(), promotionId, request.version(), principal.email());
    }

    @PostMapping("/match-preview")
    public PromotionMatchResponse match(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody PromotionMatchRequest request
    ) {
        return new PromotionMatchResponse(promotionCatalog.match(principal.clinicId(), request.message()));
    }
}
