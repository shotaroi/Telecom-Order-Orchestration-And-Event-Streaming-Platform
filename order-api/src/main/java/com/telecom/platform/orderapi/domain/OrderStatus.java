package com.telecom.platform.orderapi.domain;

public enum OrderStatus {
    CREATED,
    VALIDATING,
    VALIDATED,
    PROVISIONING,
    FULFILLED,
    FAILED
}
