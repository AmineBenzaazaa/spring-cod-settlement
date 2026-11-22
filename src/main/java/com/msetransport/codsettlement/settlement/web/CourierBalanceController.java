package com.msetransport.codsettlement.settlement.web;

import com.msetransport.codsettlement.security.AuthenticatedUser;
import com.msetransport.codsettlement.settlement.service.CourierBalanceService;
import com.msetransport.codsettlement.settlement.web.dto.CourierBalanceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lives in the settlement module (it needs statements) but under the courier URL. Keeping it
 * here means logistics never depends on settlement: dependencies point one way.
 */
@RestController
@Tag(name = "Couriers", description = "Courier reference data (staff writes)")
public class CourierBalanceController {

    private final CourierBalanceService balanceService;

    public CourierBalanceController(CourierBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /** What does this courier still owe? Scoped to the caller's merchant; staff may pass {@code merchantId}. */
    @GetMapping("/api/couriers/{id}/balance")
    public CourierBalanceResponse balance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                          @RequestParam(required = false) Long merchantId) {
        return balanceService.balance(user, id, merchantId);
    }
}
