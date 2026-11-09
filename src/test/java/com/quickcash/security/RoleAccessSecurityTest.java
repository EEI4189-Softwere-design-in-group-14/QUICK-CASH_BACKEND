
package com.quickcash.security;

import com.quickcash.config.SecurityConfig;
import com.quickcash.controller.ManagerLoanController;
import com.quickcash.controller.CollectorPaymentController;
import com.quickcash.service.LoanService;
import com.quickcash.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = {
        ManagerLoanController.class,
        CollectorPaymentController.class
})
@Import(SecurityConfig.class)
class RoleAccessSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCannotAccessManagerLoans() throws Exception {
        mvc.perform(get("/api/manager/loans/pending"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanAccessManagerLoans() throws Exception {
        when(loanService.getPendingLoans())
                .thenReturn(List.of());

        mvc.perform(get("/api/manager/loans/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanAccessManagerLoans() throws Exception {
        when(loanService.getPendingLoans())
                .thenReturn(List.of());

        mvc.perform(get("/api/manager/loans/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotAccessCollectorPayments() throws Exception {
        mvc.perform(get("/api/collector/payments/loan/2"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "collector@test.com",
            roles = "COLLECTOR")
    void collectorCanAccessPaymentHistory() throws Exception {
        when(paymentService.getLoanPayments(
                2L, "collector@test.com"))
                .thenReturn(List.of());

        mvc.perform(get("/api/collector/payments/loan/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCannotApproveLoan() throws Exception {
        mvc.perform(
                put("/api/manager/loans/2/approve")
                        .contentType("application/json")
                        .content("{\"approvedAmount\":1000}")
        ).andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }
}
