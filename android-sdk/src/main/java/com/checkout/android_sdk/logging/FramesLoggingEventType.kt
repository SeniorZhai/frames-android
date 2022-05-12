package com.checkout.android_sdk.logging

internal enum class FramesLoggingEventType(val eventId: String) {
    PAYMENT_FORM_PRESENTED("payment_form_presented"),
    CHECKOUT_API_CLIENT_INITIALISED("checkout_api_client_initialised"),
    TOKEN_REQUESTED("token_requested"),
    TOKEN_RESPONSE("token_response"),
    BILLING_FORM_PRESENTED("billing_form_presented"),
    THREEDS_WEBVIEW_PRESENTED("3ds_webview_presented"),
    THREEDS_WEBVIEW_LOADED("3ds_webview_loaded"),
    THREEDS_WEBVIEW_COMPLETE("3ds_webview_complete"),
}