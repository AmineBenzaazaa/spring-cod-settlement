package com.benzaazaa.codsettlement.web;

import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.security.JwtService;
import com.benzaazaa.codsettlement.support.TestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Shared helpers for controller slice tests. */
abstract class WebSliceTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JwtService jwtService;

    protected String bearer(AuthenticatedUser user) {
        return "Bearer " + jwtService.issueAccessToken(user);
    }

    protected String merchantToken() {
        return bearer(TestData.merchantUser(1));
    }

    protected String staffToken() {
        return bearer(TestData.staff());
    }
}
