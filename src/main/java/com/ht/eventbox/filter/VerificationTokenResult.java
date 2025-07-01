package com.ht.eventbox.filter;

public record VerificationTokenResult(boolean isTokenValid, String subject) {}
