package com.store.app.auth.entity;

/**
 * The flow an OTP was issued for. An OTP is only valid for the
 * purpose it was generated with.
 */
public enum OtpPurpose {
    REGISTRATION,
    FORGOT_PASSWORD
}
