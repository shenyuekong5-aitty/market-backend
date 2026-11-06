package com.market.config;

import com.market.common.AdminOperation;
import com.market.entity.User;
import com.market.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationAspectTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsSuccessfulAdminMutationForCurrentUser() {
        OperationLogService logService = mock(OperationLogService.class);
        AdminOperation operation = mock(AdminOperation.class);
        when(operation.value()).thenReturn("更新集市资料");

        User user = new User();
        user.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null)
        );

        new AdminOperationAspect(logService).record(operation);

        verify(logService).saveLog(7L, "更新集市资料", "更新集市资料");
    }
}
