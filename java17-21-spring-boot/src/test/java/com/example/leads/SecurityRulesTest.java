package com.example.leads;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests the role rules. jwt() skips the token checks (signature, iss, aud, exp),
 * so those need a separate integration test against a real Keycloak.
 */
@SpringBootTest
@ActiveProfiles("local")
class SecurityRulesTest {

    private static final String LEAD_JSON = "{\"name\":\"Jane\",\"phone\":\"0800123456\",\"product\":\"loan\"}";

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
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/leads/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void readRoleCanGetButCannotPost() throws Exception {
        mockMvc.perform(get("/leads/1").with(jwt().authorities(role("LEADS_READ"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD_JSON)
                .with(jwt().authorities(role("LEADS_READ"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanReadAndWrite() throws Exception {
        mockMvc.perform(get("/leads/1").with(jwt().authorities(role("LEADS_OPERATOR"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD_JSON)
                .with(jwt().authorities(role("LEADS_OPERATOR"))))
            .andExpect(status().isCreated());
    }

    @Test
    void partnerCanSubmitOwnData() throws Exception {
        mockMvc.perform(post("/b2b/partners/XYZ001/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD_JSON)
                .with(jwt().jwt(j -> j.claim("partner_id", "XYZ001").claim("azp", "partner-xyz"))
                           .authorities(role("B2B_LEADS_SUBMIT"))))
            .andExpect(status().isOk());
    }

    @Test
    void partnerCannotWriteOtherPartnersData() throws Exception {
        mockMvc.perform(post("/b2b/partners/OTHER/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD_JSON)
                .with(jwt().jwt(j -> j.claim("partner_id", "XYZ001"))
                           .authorities(role("B2B_LEADS_SUBMIT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void mobileNeedsMobileAccess() throws Exception {
        mockMvc.perform(get("/mobile/me").with(jwt().authorities(role("LEADS_READ"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/mobile/me").with(jwt().authorities(role("MOBILE_ACCESS"))))
            .andExpect(status().isOk());
    }

    @Test
    void unlistedPathIsDeniedEvenWithAToken() throws Exception {
        mockMvc.perform(get("/internal/ping").with(jwt().authorities(role("LEADS_OPERATOR"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"phone\":\"not-a-phone\"}")
                .with(jwt().authorities(role("LEADS_WRITE"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/leads/{id}", "bad id!").with(jwt().authorities(role("LEADS_READ"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void partnerWithoutPartnerIdClaimIsDenied() throws Exception {
        mockMvc.perform(post("/b2b/partners/XYZ001/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD_JSON)
                .with(jwt().authorities(role("B2B_LEADS_SUBMIT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void errorBodiesAreProblemJson() throws Exception {
        mockMvc.perform(get("/leads/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
}
