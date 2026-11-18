package com.example.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Loads the real XML files (root context + servlet context) and checks the access rules.
 * jwt() skips the token checks (signature, iss, aud, exp, typ);
 * those are covered by KeycloakJwtDecoderFactoryTest.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/applicationContext-security.xml"),
    @ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/mvc-servlet.xml")
})
class XmlSecurityRulesTest {

    private static final String ORDER_JSON = "{\"item\":\"book\"}";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static SimpleGrantedAuthority role(String name) {
        return new SimpleGrantedAuthority("ROLE_" + name);
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void ordersRoleCanCreateOrder() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON)
                .with(jwt().authorities(role("ORDERS_WRITE"))))
            .andExpect(status().isOk());
    }

    @Test
    void paymentsRoleCannotCreateOrder() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON)
                .with(jwt().authorities(role("PAYMENTS_WRITE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidPaymentIsBadRequest() throws Exception {
        mockMvc.perform(post("/payment").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-5}")
                .with(jwt().authorities(role("PAYMENTS_WRITE"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/status/{id}", "bad id!").with(jwt().authorities(role("ORDERS_WRITE"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unlistedPathIsDeniedEvenWithAToken() throws Exception {
        mockMvc.perform(get("/internal/ping").with(jwt().authorities(role("ORDERS_WRITE"))))
            .andExpect(status().isForbidden());
    }
}
