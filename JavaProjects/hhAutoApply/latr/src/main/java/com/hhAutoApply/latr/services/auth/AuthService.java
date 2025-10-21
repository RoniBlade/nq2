    package com.hhAutoApply.latr.services.auth;

    import com.hhAutoApply.latr.clients.auth.AuthClient;
    import com.hhAutoApply.latr.config.AppProperties;
    import com.hhAutoApply.latr.exceptions.AuthException;
    import com.hhAutoApply.latr.utlis.CodeProvider;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class AuthService {

        private final CodeProvider codeProvider;
        private final AppProperties appProperties;
        private final AuthClient authClient;

        public String getAccessCode() {
            String authUrl = generateLinkForAuth();
            log.info("Перейдите по ссылке и введите в консоль полученный код:\n{}", authUrl);

            String authCode = codeProvider.getCode();
            if (authCode == null || authCode.isBlank()) {
                log.error("Код авторизации не был получен.");
                throw new AuthException("Не удалось получить код авторизации.");
            }

            return authClient.fetchAccessToken(authCode);
        }

        String generateLinkForAuth() {
            return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s",
                    appProperties.getAuth_url(),
                    appProperties.getClient_id(),
                    appProperties.getRedirect_uri());
        }
    }