package com.pfe.ekyc.security;

import com.pfe.ekyc.domain.Role;

/** Utilisateur authentifié, extrait du JWT et injecté via @AuthenticationPrincipal. */
public record AuthUser(Long id, String email, Role role) {}
