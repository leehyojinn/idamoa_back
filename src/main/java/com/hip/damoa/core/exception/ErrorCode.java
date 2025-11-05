package com.hip.damoa.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", " Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", " Method Not Allowed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Server Error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", " Invalid Type Value"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "User already exists"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U003", "Email already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U004", "Invalid email or password"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "U005", "Account is disabled"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "U006", "Account is locked"),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "U007", "Account is deleted"),

    // Verification
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "V001", "Verification code expired"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "V002", "Verification code mismatch"),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "V003", "Too many attempts. Please try again later"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "V004", "Email not verified"),
    PHONE_NOT_VERIFIED(HttpStatus.FORBIDDEN, "V005", "Phone not verified"),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "V006", "Email verification is required"),
    SMS_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "V007", "SMS verification is required"),

    // JWT
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J001", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "J002", "Expired token"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "J003", "Refresh token not found"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "J004", "Refresh token expired"),

    // Signup
    SIGNUP_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "S001", "Signup token not found or expired"),
    SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "S002", "Invalid signup token"),

    // OAuth
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "O001", "Invalid OAuth provider"),
    OAUTH_STATE_NOT_FOUND(HttpStatus.BAD_REQUEST, "O002", "OAuth state not found or expired"),
    OAUTH_STATE_MISMATCH(HttpStatus.BAD_REQUEST, "O003", "OAuth state mismatch"),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "O004", "Invalid OAuth state"),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "O005", "Social account already linked to another user"),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "O006", "Social account not found"),
    CANNOT_UNLINK_LAST_SOCIAL_ACCOUNT(HttpStatus.BAD_REQUEST, "O007", "Cannot unlink last social account. Set a password first"),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "O008", "OAuth provider not supported"),
    OAUTH_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "O009", "Email not provided by OAuth provider"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "O010", "This email is already registered. Please login with email/password or use password recovery"),

    // Estimate/Bidding
    ESTIMATE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "Estimate request not found"),
    ESTIMATE_REQUEST_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "E002", "Estimate request cannot be updated"),
    ESTIMATE_REQUEST_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "E003", "Estimate request already submitted"),
    ESTIMATE_REQUEST_EXPIRED(HttpStatus.BAD_REQUEST, "E004", "Estimate request expired"),
    ESTIMATE_REQUEST_CLOSED(HttpStatus.BAD_REQUEST, "E005", "Estimate request closed"),
    ESTIMATE_REQUEST_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "E006", "Estimate request is not editable"),
    ESTIMATE_REQUEST_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "E007", "Estimate request already published"),

    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Proposal not found"),
    PROPOSAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "Proposal already exists for this request"),
    PROPOSAL_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "P003", "Proposal cannot be updated"),
    PROPOSAL_CANNOT_BE_ACCEPTED(HttpStatus.BAD_REQUEST, "P004", "Proposal cannot be accepted"),
    INSUFFICIENT_SUBSCRIPTION_QUOTA(HttpStatus.BAD_REQUEST, "P005", "Insufficient subscription quota"),
    INSUFFICIENT_CREDITS(HttpStatus.BAD_REQUEST, "P006", "Insufficient credits"),

    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "Match not found"),
    MATCH_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "Match already exists for this request"),
    MATCH_CANNOT_BE_STARTED(HttpStatus.BAD_REQUEST, "M003", "Match cannot be started"),
    MATCH_CANNOT_BE_COMPLETED(HttpStatus.BAD_REQUEST, "M004", "Match cannot be completed"),

    // Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "A001", "Access forbidden"),

    // Company
    COMPANY_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "Company profile not found"),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "CP002", "Company already exists for this user"),
    COMPANY_SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "CP003", "Company slug already exists"),

    // Contest
    CONTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "Contest not found"),
    CONTEST_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "CT002", "Contest already submitted"),
    CONTEST_NOT_ACCEPTING_ENTRIES(HttpStatus.BAD_REQUEST, "CT003", "Contest is not accepting entries"),
    CONTEST_NOT_EXPIRED(HttpStatus.BAD_REQUEST, "CT004", "Contest has not expired yet"),
    CONTEST_WINNER_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "CT005", "Contest winner already selected"),
    CONTEST_WINNER_NOT_FOUND(HttpStatus.NOT_FOUND, "CT006", "Contest winner not found"),
    INVALID_CONTEST_DATES(HttpStatus.BAD_REQUEST, "CT007", "Invalid contest dates"),

    // Contest Entry
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "CE001", "Contest entry not found"),
    ENTRY_ALREADY_EXISTS(HttpStatus.CONFLICT, "CE002", "Entry already exists for this contest"),
    ENTRY_CANNOT_BE_EDITED(HttpStatus.BAD_REQUEST, "CE003", "Entry cannot be edited"),
    ENTRY_CANNOT_BE_WITHDRAWN(HttpStatus.BAD_REQUEST, "CE004", "Entry cannot be withdrawn"),
    ENTRY_CANNOT_BE_RATED(HttpStatus.BAD_REQUEST, "CE005", "Entry cannot be rated"),

    // Subscription
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SB001", "Subscription not found"),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "SB002", "Active subscription already exists"),
    SUBSCRIPTION_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "SB003", "Subscription plan not found"),
    INVALID_SUBSCRIPTION_UPGRADE(HttpStatus.BAD_REQUEST, "SB004", "Invalid subscription upgrade"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PY001", "Payment not found"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PY002", "Payment already completed"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PY003", "Payment failed"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PY004", "Invalid payment amount"),
    IDEMPOTENCY_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "PY005", "Idempotency key already exists"),

    // Invoice
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "IV001", "Invoice not found"),
    INVOICE_ALREADY_PAID(HttpStatus.BAD_REQUEST, "IV002", "Invoice already paid"),
    INVOICE_CANNOT_BE_PAID(HttpStatus.BAD_REQUEST, "IV003", "Invoice cannot be paid"),

    // Planner Request
    PLANNER_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "Planner request not found"),
    PLANNER_REQUEST_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "PR002", "Planner request cannot be edited"),
    PLANNER_REQUEST_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "PR003", "Planner request cannot be deleted"),

    // File Upload
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "File not found"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "F002", "File is empty"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "F003", "File size exceeds maximum limit (10MB)"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "F004", "File type not allowed"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "File upload failed"),

    // Profile
    INVALID_PROFILE_TYPE(HttpStatus.BAD_REQUEST, "PR001", "Invalid profile type"),
    PROFILE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PR002", "Profile already completed"),
    PROFILE_TYPE_NOT_SELECTED(HttpStatus.BAD_REQUEST, "PR003", "Profile type not selected"),
    UNAUTHORIZED_ROLE_CHANGE(HttpStatus.FORBIDDEN, "PR004", "Unauthorized role change attempt"),

    // Board/Content
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BD001", "Board not found"),
    BOARD_CANNOT_BE_EDITED(HttpStatus.BAD_REQUEST, "BD002", "Board cannot be edited"),
    BOARD_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "BD003", "Board cannot be deleted");

    private final HttpStatus status;
    private final String code;
    private final String message;
}