package com.ai.applications.rag.insightvault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditListener.class);

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.info("Login succeeded for user '{}'", event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Login failed for user '{}': {}",
                event.getAuthentication().getName(), event.getException().getMessage());
    }
}
