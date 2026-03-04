package com.telecom.platform.common.security;

/**
 * Marker interface for principals that carry a customer ID (e.g. dev mock).
 */
public interface CustomerIdPrincipal {
    String getCustomerId();
}
