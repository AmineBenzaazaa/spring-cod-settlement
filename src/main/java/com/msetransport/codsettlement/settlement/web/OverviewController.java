package com.msetransport.codsettlement.settlement.web;

import com.msetransport.codsettlement.security.AuthenticatedUser;
import com.msetransport.codsettlement.settlement.service.SettlementOverviewService;
import com.msetransport.codsettlement.settlement.web.dto.OverviewResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** One call for the dashboard's KPI tiles and courier balance table. */
@RestController
@Tag(name = "Overview", description = "Dashboard totals and per-courier balances")
public class OverviewController {

    private final SettlementOverviewService overviewService;

    public OverviewController(SettlementOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /** Scoped to the caller's merchant; staff see all merchants or pass {@code merchantId}. */
    @GetMapping("/api/overview")
    public OverviewResponse overview(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(required = false) Long merchantId) {
        return overviewService.overview(user, merchantId);
    }
}
