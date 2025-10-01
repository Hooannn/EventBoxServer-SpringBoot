package com.ht.eventbox.constant;

public class Constant {
    public static class MailSubject {
        public static final String REGISTER = "EventBox - Welcome to EventBox";
        public static final String FORGOT_PASSWORD = "EventBox - Reset your password";
        public static final String VERIFY_EMAIL = "EventBox - Verify your email address";
        public static final String RESEND_VERIFY_EMAIL = "EventBox - Resend verify your email address";
        public static final String RESET_PASSWORD_OTP = "EventBox - Reset your password OTP";
        public static final String RESET_PASSWORD = "EventBox - Reset your password";
        public static final String VERIFY_EMAIL_SUCCESS = "EventBox - Verify your email address success";
        public static final String FORGOT_PASSWORD_OTP = "EventBox - Forgot password OTP";
        public static final String FORGOT_PASSWORD_OTP_SUCCESS = "EventBox - Forgot password OTP success";
        public static final String RESET_PASSWORD_OTP_SUCCESS = "EventBox - Reset password OTP success";
        public static final String MEMBER_ADDED = "EventBox - You have been added to an organization";
        public static final String MEMBER_REMOVED = "EventBox - You have been removed from an organization";
        public static final String ORDER_PAID = "EventBox - Your order has been paid successfully";
        public static final String UPCOMING_EVENT = "EventBox - Upcoming event reminder";
        public static final String GIVEAWAY_TICKET = "EventBox - You have received a giveaway ticket";
    }

    public static class MailTemplate {
        public static final String VERIFY_EMAIL = "verify-email";
        public static final String FORGOT_PASSWORD = "forgot-password-otp";
        public static final String MEMBER_ADDED = "member-added";
        public static final String MEMBER_REMOVED = "member-removed";
        public static final String ORDER_PAID = "order-paid";
        public static final String UPCOMING_EVENT = "upcoming-event";
        public static final String GIVEAWAY_TICKET = "giveaway-ticket";
    }

    public static class ValidationCode {
        public static final String EMAIL_NOT_EMPTY = "email_not_empty";
        public static final String EMAIL_MUST_BE_VALID = "email_must_be_valid";
        public static final String PASSWORD_NOT_EMPTY = "password_not_empty";
        public static final String PASSWORD_MIN_LENGTH = "password_min_length";
        public static final String FIRST_NAME_NOT_EMPTY = "first_name_not_empty";
        public static final String LAST_NAME_NOT_EMPTY = "last_name_not_empty";
        public static final String OTP_NOT_EMPTY = "otp_not_empty";
        public static final String TOKEN_NOT_EMPTY = "token_not_empty";
        public static final String PLATFORM_NOT_EMPTY = "platform_not_empty";
        public static final String ORGANIZATION_ROLE_NOT_EMPTY = "organization_role_not_empty";
    }

    public static class ErrorCode {
        public static final String TICKET_ITEM_NOT_USED = "ticket_item_not_used";
        public static final String NOT_ALLOWED_OPERATION = "not_allowed_operation";
        public static final String USER_NOT_FOUND = "user_not_found";
        public static final String USER_ALREADY_EXISTS = "user_already_exists";
        public static final String INVALID_CREDENTIALS = "invalid_credentials";
        public static final String INVALID_TOKEN = "invalid_token";
        public static final String INVALID_SIGNING_METHOD = "invalid_signing_method";
        public static final String MISSING_TOKEN = "missing_token";
        public static final String INVALID_TOKEN_SIGNATURE = "invalid_token_signature";
        public static final String EXPIRED_TOKEN = "expired_token";
        public static final String USER_ALREADY_IN_ORGANIZATION = "user_already_in_organization";
        public static final String ORGANIZATION_NOT_FOUND = "organization_not_found";
        public static final String USER_NOT_IN_ORGANIZATION = "user_not_in_organization";
        public static final String UNAUTHORIZED = "unauthorized";
        public static final String CLOUDINARY_UPLOAD_FAILED = "cloudinary_upload_failed";
        public static final String EVENT_NOT_FOUND = "event_not_found";
        public static final String ROLE_NOT_FOUND = "role_not_found";
        public static final String ROLE_IN_USE = "role_in_use";
        public static final String ROLE_ALREADY_EXISTS = "role_already_exists";
        public static final String PERMISSION_ALREADY_EXISTS = "permission_already_exists";
        public static final String PERMISSION_NOT_FOUND = "permission_not_found";
        public static final String PERMISSION_IN_USE = "permission_in_use";
        public static final String ORGANIZATION_HAS_EVENTS = "organization_has_events";
        public static final String TICKET_NOT_FOUND = "ticket_not_found";
        public static final String INTERNAL_SERVER_ERROR = "internal_server_error";
        public static final String TICKET_OUT_OF_STOCK = "ticket_out_of_stock";
        public static final String ORDER_NOT_FOUND = "order_not_found";
        public static final String TICKET_SALE_NOT_STARTED = "ticket_sale_not_started";
        public static final String TICKET_SALE_ENDED = "ticket_sale_ended";
        public static final String TICKET_ITEM_NOT_FOUND = "ticket_item_not_found";
        public static final String TICKET_ITEM_INVALID = "ticket_item_invalid";
        public static final String SHOW_NOT_STARTED = "show_not_started";
        public static final String SHOW_ENDED = "show_ended";
        public static final String CATEGORY_NOT_FOUND = "category_not_found";
        public static final String CATEGORY_IN_USE = "category_in_use";
        public static final String EVENT_ALREADY_PAID = "event_already_paid";
        public static final String EVENT_NOT_ENDED = "event_not_ended";
        public static final String PAYOUT_FAILED = "payout_failed";
        public static final String TICKET_NOT_SOLD = "ticket_not_sold";
        public static final String SHOW_NOT_ENDED = "show_not_ended";
        public static final String TICKET_ITEM_ALREADY_USED = "ticket_item_already_used";
        public static final String VOUCHER_NOT_FOUND = "voucher_not_found";
        public static final String VOUCHER_HAS_BEEN_USED = "voucher_has_been_used";
        public static final String VOUCHER_CODE_ALREADY_EXISTS = "voucher_code_already_exists";
        public static final String VOUCHER_TIME_NOT_VALID = "voucher_time_not_valid";
        public static final String VOUCHER_CONDITION_NOT_MET = "voucher_condition_not_met";
        public static final String VOUCHER_USAGE_LIMIT_EXCEEDED = "voucher_usage_limit_exceeded";
        public static final String VOUCHER_PER_USER_LIMIT_EXCEEDED = "voucher_per_user_limit_exceeded";
    }

    public static class SuccessCode {
        public static final String REGISTER_SUCCESS = "register_success";
        public static final String LOGIN_SUCCESS = "login_success";
        public static final String LOGOUT_SUCCESS = "logout_success";
        public static final String REFRESH_SUCCESS = "refresh_success";
        public static final String FORGOT_PASSWORD_OTP_SUCCESS = "forgot_password_otp_success";
        public static final String RESET_PASSWORD_OTP_SUCCESS = "reset_password_otp_success";
        public static final String RESEND_VERIFY_SUCCESS = "resend_verify_success";
        public static final String VERIFY_SUCCESS = "verify_success";
        public static final String UPDATED = "updated";
        public static final String UPDATE_SUCCESSFULLY = "UPDATE_SUCCESSFULLY";
        public static final String DELETE_SUCCESSFULLY = "DELETE_SUCCESSFULLY";
        public static final String REQUEST_FOR_PAYOUT_SUCCESSFULLY = "REQUEST_FOR_PAYOUT_SUCCESSFULLY";
    }

    public static class ContextKey {
        public static final String RESPONSE = "X-Response";
        public static final String USER_ID = "X-User-ID";
        public static final String USER_ROLES = "X-User-Roles";
        public static final String USER_PERMISSIONS = "X-User-Permissions";
        public static final String ACCESS_TOKEN = "X-Access-Token";
        public static final String DEVICE_ID = "X-Device-ID";
    }

    public static class RedisPrefix {
        public static final String CACHE = "cache";
        public static final String RESET_PASSWORD_OTP = "reset_password_otp";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String PAYPAL_ACCESS_TOKEN = "paypal_access_token";
        public static final String REGISTER = "register";
    }

    public static class RedisKey {
        public static final int RESERVATION_EXPIRES = 15 * 60;
        public static final int EXTENDED_RESERVATION_EXPIRES = 15 * 60;
    }

    public static class StorageFolder {
        public static final String USER_ASSETS = "UserAssets";
        public static final String ORGANIZATION_ASSETS = "OrganizationAssets";
        public static final String EVENT_ASSETS = "EventAssets";
    }

    public static class DefaultRole {
        public static final String USER = "user";
        public static final String USER_DESCRIPTION = "User of the system";
    }
}
