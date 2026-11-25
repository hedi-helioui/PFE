package com.pfe.ekyc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kyc")
public record KycProperties(Jwt jwt, Storage storage, Thresholds thresholds, Mock mock, Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Storage(String root) {}

    public record Thresholds(double approve, double reject, double fraudApproveMax, double fraudRejectMin) {}

    public record Mock(double faceMatchScore, double livenessScore, double fraudScore) {}

    public record Seed(boolean enabled, String agentEmail, String agentPassword,
                       String adminEmail, String adminPassword) {}
}
