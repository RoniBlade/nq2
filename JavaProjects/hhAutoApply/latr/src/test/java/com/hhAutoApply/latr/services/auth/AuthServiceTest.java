package com.hhAutoApply.latr.services.auth;

import com.hhAutoApply.latr.clients.auth.AuthClient;
import com.hhAutoApply.latr.config.AppProperties;
import com.hhAutoApply.latr.exceptions.AuthException;
import com.hhAutoApply.latr.utlis.CodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CodeProvider codeProvider;

    @Mock
    private AuthClient authClient;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(appProperties.getAuth_url()).thenReturn("https://auth.example.com");
        when(appProperties.getClient_id()).thenReturn("test_client_id");
        when(appProperties.getRedirect_uri()).thenReturn("https://redirect.example.com");
    }


    @Test
    void generateLinkForAuthTest() {
        String expectedLink = "https://auth.example.com?response_type=code&client_id=test_client_id&redirect_uri=https://redirect.example.com";

        String actualLink = authService.generateLinkForAuth();

        assertNotNull(actualLink);
        assertTrue(actualLink.contains("client_id=test_client_id"));
        assertTrue(actualLink.contains("redirect_uri=https://redirect.example.com"));
        assertEquals(actualLink, expectedLink);

    }

    @Test
    void accessCodeTest_Successful() {

        when(codeProvider.getCode()).thenReturn("access_code");
        when(authClient.fetchAccessToken("access_code")).thenReturn("auth_code");

        String result = authService.getAccessCode();

        assertEquals(result, "auth_code");

    }

    @Test
    void accessCodeTest_Failed_AccesCodeIsNull() {

        when(codeProvider.getCode()).thenReturn(null);

        assertThrows(AuthException.class, authService::getAccessCode);

    }

    @Test
    void accessCodeTest_Failed_AccesCodeIsEmpty() {

        when(codeProvider.getCode()).thenReturn("");

        assertThrows(AuthException.class, authService::getAccessCode);

    }

}


