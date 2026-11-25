package com.pfe.ekyc.ai;

import java.time.LocalDate;

/** Données lues sur le document par l'OCR. */
public record OcrResult(String fullName, String idNumber, LocalDate birthDate) {}
