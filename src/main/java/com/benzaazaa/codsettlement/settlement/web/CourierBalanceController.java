package com.benzaazaa.codsettlement.settlement.web;

import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.service.CourierBalanceService;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
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
