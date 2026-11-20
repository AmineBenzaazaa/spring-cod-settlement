package com.benzaazaa.codsettlement.account.domain;

/**
 * MERCHANT users only see their own merchant's data; STAFF (operations) see everything
 * and can perform back-office actions such as resolving disputes.
 */
public enum Role {
    MERCHANT,
    STAFF;

    /** Spring Security convention: {@code hasRole('STAFF')} checks the authority {@code ROLE_STAFF}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
