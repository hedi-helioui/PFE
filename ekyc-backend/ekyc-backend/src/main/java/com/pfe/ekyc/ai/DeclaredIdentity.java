package com.pfe.ekyc.ai;

import java.time.LocalDate;

/** Identité saisie par le client dans le formulaire d'enrôlement. */
public record DeclaredIdentity(String fullName, String idNumber, LocalDate birthDate) {}
