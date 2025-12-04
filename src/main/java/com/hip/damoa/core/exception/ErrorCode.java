package com.hip.damoa.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C006", "중복된 리소스입니다"),
    RESOURCE_IN_USE(HttpStatus.CONFLICT, "C007", "사용 중인 리소스는 삭제할 수 없습니다"),
    INVALID_UUID_FORMAT(HttpStatus.BAD_REQUEST, "C008", "유효하지 않은 UUID 형식입니다"),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, "C009", "유효하지 않은 회원 상태입니다 (ACTIVE, INACTIVE, SUSPENDED, PENDING 중 선택)"),
    INVALID_USER_ROLE(HttpStatus.BAD_REQUEST, "C010", "유효하지 않은 회원 역할입니다 (USER, COMPANY, ADMIN 중 선택)"),
    INVALID_ESTIMATE_STATUS(HttpStatus.BAD_REQUEST, "C011", "유효하지 않은 견적 상태입니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 사용자입니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U003", "이미 존재하는 이메일입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U004", "이메일 또는 비밀번호가 올바르지 않습니다"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "U005", "비활성화된 계정입니다"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "U006", "잠긴 계정입니다"),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "U007", "삭제된 계정입니다"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "U008", "로그인이 필요합니다"),

    // Verification
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "V001", "인증 코드가 만료되었습니다"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "V002", "인증 코드가 일치하지 않습니다"),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "V003", "너무 많은 시도가 있었습니다. 잠시 후 다시 시도해주세요"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "V004", "이메일 인증이 완료되지 않았습니다"),
    PHONE_NOT_VERIFIED(HttpStatus.FORBIDDEN, "V005", "전화번호 인증이 완료되지 않았습니다"),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "V006", "이메일 인증이 필요합니다"),
    SMS_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "V007", "SMS 인증이 필요합니다"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "V008", "올바른 이메일 형식이 아닙니다"),
    TOKEN_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "V009", "토큰과 이메일이 일치하지 않습니다"),

    // JWT
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "J002", "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "J003", "리프레시 토큰을 찾을 수 없습니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "J004", "리프레시 토큰이 만료되었습니다"),

    // Signup
    SIGNUP_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "S001", "회원가입 토큰을 찾을 수 없거나 만료되었습니다"),
    SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "S002", "유효하지 않은 회원가입 토큰입니다"),

    // Password Reset
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "PW001", "비밀번호 재설정 토큰을 찾을 수 없거나 만료되었습니다"),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "PW002", "비밀번호 재설정 토큰이 만료되었습니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "PW003", "현재 비밀번호가 일치하지 않습니다"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "PW004", "새 비밀번호는 현재 비밀번호와 달라야 합니다"),

    // OAuth
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "O001", "유효하지 않은 OAuth 제공자입니다"),
    OAUTH_STATE_NOT_FOUND(HttpStatus.BAD_REQUEST, "O002", "OAuth 상태를 찾을 수 없거나 만료되었습니다"),
    OAUTH_STATE_MISMATCH(HttpStatus.BAD_REQUEST, "O003", "OAuth 상태가 일치하지 않습니다"),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "O004", "유효하지 않은 OAuth 상태입니다"),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "O005", "이미 다른 사용자에게 연결된 소셜 계정입니다"),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "O006", "소셜 계정을 찾을 수 없습니다"),
    CANNOT_UNLINK_LAST_SOCIAL_ACCOUNT(HttpStatus.BAD_REQUEST, "O007", "마지막 소셜 계정은 연결 해제할 수 없습니다. 먼저 비밀번호를 설정해주세요"),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "O008", "지원하지 않는 OAuth 제공자입니다"),
    OAUTH_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "O009", "OAuth 제공자로부터 이메일을 받지 못했습니다"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "O010", "이미 가입된 이메일입니다. 이메일/비밀번호로 로그인하거나 비밀번호 찾기를 이용해주세요"),
    OAUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "O011", "유효하지 않거나 만료된 OAuth 코드입니다"),

    // Estimate/Bidding
    ESTIMATE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "견적 요청을 찾을 수 없습니다"),
    ESTIMATE_REQUEST_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "E002", "견적 요청을 수정할 수 없습니다"),
    ESTIMATE_REQUEST_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "E003", "이미 제출된 견적 요청입니다"),
    ESTIMATE_REQUEST_EXPIRED(HttpStatus.BAD_REQUEST, "E004", "만료된 견적 요청입니다"),
    ESTIMATE_REQUEST_CLOSED(HttpStatus.BAD_REQUEST, "E005", "종료된 견적 요청입니다"),
    ESTIMATE_REQUEST_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "E006", "수정할 수 없는 견적 요청입니다"),
    ESTIMATE_REQUEST_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "E007", "이미 게시된 견적 요청입니다"),
    ESTIMATE_REQUEST_HAS_PROPOSALS(HttpStatus.BAD_REQUEST, "E008", "제안이 있는 견적 요청은 삭제할 수 없습니다"),
    ESTIMATE_REQUEST_ALREADY_MATCHED(HttpStatus.BAD_REQUEST, "E009", "이미 매칭된 견적 요청은 수정하거나 삭제할 수 없습니다"),

    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "제안서를 찾을 수 없습니다"),
    PROPOSAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "이미 이 요청에 대한 제안서가 존재합니다"),
    PROPOSAL_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "P003", "제안서를 수정할 수 없습니다"),
    PROPOSAL_CANNOT_BE_ACCEPTED(HttpStatus.BAD_REQUEST, "P004", "제안서를 수락할 수 없습니다"),
    PROPOSAL_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "P007", "수락된 제안서는 삭제할 수 없습니다"),
    INSUFFICIENT_SUBSCRIPTION_QUOTA(HttpStatus.BAD_REQUEST, "P005", "구독 한도가 부족합니다"),
    INSUFFICIENT_CREDITS(HttpStatus.BAD_REQUEST, "P006", "크레딧이 부족합니다"),

    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "매칭을 찾을 수 없습니다"),
    MATCH_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "이미 이 요청에 대한 매칭이 존재합니다"),
    MATCH_CANNOT_BE_STARTED(HttpStatus.BAD_REQUEST, "M003", "매칭을 시작할 수 없습니다"),
    MATCH_CANNOT_BE_COMPLETED(HttpStatus.BAD_REQUEST, "M004", "매칭을 완료할 수 없습니다"),

    // Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "A001", "접근 권한이 없습니다"),
    COMPANY_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "A002", "업체 회원만 이용할 수 있습니다"),
    ADMIN_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "A003", "관리자만 이용할 수 있습니다"),
    NOT_COMPANY_OWNER(HttpStatus.FORBIDDEN, "A004", "본인 업체만 수정/삭제할 수 있습니다"),
    NOT_PROPOSAL_OWNER(HttpStatus.FORBIDDEN, "A005", "본인이 제출한 제안서만 수정/삭제할 수 있습니다"),
    NOT_REQUEST_OWNER(HttpStatus.FORBIDDEN, "A006", "본인의 견적 요청만 수정/삭제할 수 있습니다"),
    NOT_REVIEW_AUTHOR(HttpStatus.FORBIDDEN, "A007", "본인이 작성한 리뷰만 수정/삭제할 수 있습니다"),
    NOT_REVIEW_COMPANY_OWNER(HttpStatus.FORBIDDEN, "A008", "본인 업체에 대한 리뷰에만 답변할 수 있습니다"),
    NOT_FILE_OWNER(HttpStatus.FORBIDDEN, "A009", "본인이 업로드한 파일만 삭제할 수 있습니다"),
    PROPOSAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A010", "제안서 조회 권한이 없습니다"),
    NOT_BOARD_AUTHOR(HttpStatus.FORBIDDEN, "A011", "본인이 작성한 게시글만 수정/삭제할 수 있습니다"),

    // Company
    COMPANY_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "업체 프로필을 찾을 수 없습니다"),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "CP002", "이미 이 사용자의 업체가 존재합니다"),
    COMPANY_SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "CP003", "이미 존재하는 업체 슬러그입니다"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "CP004", "리뷰를 찾을 수 없습니다"),

    // Contest
    CONTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "콘테스트를 찾을 수 없습니다"),
    CONTEST_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "CT002", "이미 제출된 콘테스트입니다"),
    CONTEST_NOT_ACCEPTING_ENTRIES(HttpStatus.BAD_REQUEST, "CT003", "현재 참가 신청을 받지 않는 콘테스트입니다"),
    CONTEST_NOT_EXPIRED(HttpStatus.BAD_REQUEST, "CT004", "아직 종료되지 않은 콘테스트입니다"),
    CONTEST_WINNER_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "CT005", "이미 우승자가 선정된 콘테스트입니다"),
    CONTEST_WINNER_NOT_FOUND(HttpStatus.NOT_FOUND, "CT006", "콘테스트 우승자를 찾을 수 없습니다"),
    INVALID_CONTEST_DATES(HttpStatus.BAD_REQUEST, "CT007", "유효하지 않은 콘테스트 일정입니다"),

    // Contest Entry
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "CE001", "콘테스트 참가 항목을 찾을 수 없습니다"),
    ENTRY_ALREADY_EXISTS(HttpStatus.CONFLICT, "CE002", "이미 이 콘테스트에 참가 항목이 존재합니다"),
    ENTRY_CANNOT_BE_EDITED(HttpStatus.BAD_REQUEST, "CE003", "참가 항목을 수정할 수 없습니다"),
    ENTRY_CANNOT_BE_WITHDRAWN(HttpStatus.BAD_REQUEST, "CE004", "참가를 철회할 수 없습니다"),
    ENTRY_CANNOT_BE_RATED(HttpStatus.BAD_REQUEST, "CE005", "평가할 수 없는 항목입니다"),

    // Subscription
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SB001", "구독을 찾을 수 없습니다"),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "SB002", "이미 활성 구독이 존재합니다"),
    SUBSCRIPTION_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "SB003", "구독 플랜을 찾을 수 없습니다"),
    INVALID_SUBSCRIPTION_UPGRADE(HttpStatus.BAD_REQUEST, "SB004", "유효하지 않은 구독 업그레이드입니다"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PY001", "결제 정보를 찾을 수 없습니다"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PY002", "이미 완료된 결제입니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PY003", "결제에 실패했습니다"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PY004", "유효하지 않은 결제 금액입니다"),
    IDEMPOTENCY_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "PY005", "이미 존재하는 멱등성 키입니다"),
    PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "PY006", "결제가 필요합니다"),
    PAYMENT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "PY007", "완료되지 않은 결제입니다"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PY008", "결제 금액이 일치하지 않습니다"),

    // Credit
    CREDIT_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "크레딧 정보를 찾을 수 없습니다"),
    INVALID_CREDIT_PACKAGE(HttpStatus.BAD_REQUEST, "CR002", "유효하지 않은 충전 패키지입니다"),
    CREDIT_PURCHASE_FAILED(HttpStatus.BAD_REQUEST, "CR003", "크레딧 충전에 실패했습니다"),
    CREDIT_REFUND_FAILED(HttpStatus.BAD_REQUEST, "CR004", "크레딧 환불에 실패했습니다"),
    REFUND_AMOUNT_TOO_SMALL(HttpStatus.BAD_REQUEST, "CR005", "최소 환불 금액은 1,000원입니다"),
    REFUND_EXCEEDS_BALANCE(HttpStatus.BAD_REQUEST, "CR006", "환불 금액이 잔액을 초과합니다"),
    CREDIT_ALREADY_REFUNDED(HttpStatus.CONFLICT, "CR007", "이미 환불된 크레딧입니다"),
    PAYMENT_SESSION_EXPIRED(HttpStatus.BAD_REQUEST, "CR008", "결제 세션이 만료되었습니다"),
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "CR013", "환불 정보를 찾을 수 없습니다"),
    REFUND_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "CR014", "이미 처리된 환불 요청입니다"),
    CREDIT_PACKAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR009", "크레딧 패키지를 찾을 수 없습니다"),
    CREDIT_PACKAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CR010", "동일한 단위 금액의 패키지가 이미 존재합니다"),
    BONUS_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "CR011", "3만원 미만 패키지는 보너스를 적용할 수 없습니다"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "CR012", "수량은 1 이상이어야 합니다"),

    // Company
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "업체를 찾을 수 없습니다"),
    COMPANY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CO002", "업체 접근 권한이 없습니다"),

    // Ad Campaign
    AD_CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "AD001", "광고 캠페인을 찾을 수 없습니다"),
    AD_CAMPAIGN_ALREADY_ACTIVE(HttpStatus.CONFLICT, "AD002", "이미 활성 캠페인이 존재합니다"),
    AD_CAMPAIGN_EXPIRED(HttpStatus.BAD_REQUEST, "AD003", "만료된 캠페인입니다"),
    AD_MIN_DAILY_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "AD004", "최소 일당 금액(500원)을 충족하지 않습니다"),
    AD_INVALID_DURATION(HttpStatus.BAD_REQUEST, "AD005", "유효하지 않은 광고 기간입니다. 7일, 14일, 30일 중 선택해주세요"),
    AD_CAMPAIGN_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "AD006", "활성 상태가 아닌 캠페인입니다"),
    AD_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AD007", "광고 결제 내역을 찾을 수 없습니다"),
    AD_CAMPAIGN_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "AD008", "캠페인을 취소할 수 없습니다"),
    AD_CAMPAIGN_ALREADY_EXISTS(HttpStatus.CONFLICT, "AD009", "이미 해당 업체의 캠페인이 존재합니다"),
    AD_CAMPAIGN_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "AD010", "이미 종료된 캠페인입니다"),

    // Invoice
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "IV001", "청구서를 찾을 수 없습니다"),
    INVOICE_ALREADY_PAID(HttpStatus.BAD_REQUEST, "IV002", "이미 결제된 청구서입니다"),
    INVOICE_CANNOT_BE_PAID(HttpStatus.BAD_REQUEST, "IV003", "결제할 수 없는 청구서입니다"),

    // Planner Request
    PLANNER_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "플래너 요청을 찾을 수 없습니다"),
    PLANNER_REQUEST_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "PR002", "플래너 요청을 수정할 수 없습니다"),
    PLANNER_REQUEST_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "PR003", "플래너 요청을 삭제할 수 없습니다"),

    // File Upload
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "파일을 찾을 수 없습니다"),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "F002", "빈 파일입니다"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "F003", "파일 크기가 최대 허용 크기(10MB)를 초과했습니다"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "F004", "허용되지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "파일 업로드에 실패했습니다"),
    FILE_PRICING_NOT_FOUND(HttpStatus.NOT_FOUND, "F006", "파일 가격 정보를 찾을 수 없습니다"),
    DOWNLOAD_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "F007", "다운로드 제한을 초과했습니다"),
    FILE_ALREADY_PURCHASED(HttpStatus.CONFLICT, "F008", "이미 구매한 파일입니다"),
    FILE_PURCHASE_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "F009", "파일 구매가 필요합니다"),

    // Profile
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PR000", "프로필을 찾을 수 없습니다"),
    INVALID_PROFILE_TYPE(HttpStatus.BAD_REQUEST, "PR001", "유효하지 않은 프로필 타입입니다"),
    PROFILE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PR002", "이미 완료된 프로필입니다"),
    PROFILE_TYPE_NOT_SELECTED(HttpStatus.BAD_REQUEST, "PR003", "프로필 타입이 선택되지 않았습니다"),
    UNAUTHORIZED_ROLE_CHANGE(HttpStatus.FORBIDDEN, "PR004", "권한 변경 권한이 없습니다"),

    // Board/Content
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BD001", "게시글을 찾을 수 없습니다"),
    BOARD_CANNOT_BE_EDITED(HttpStatus.BAD_REQUEST, "BD002", "게시글을 수정할 수 없습니다"),
    BOARD_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "BD003", "게시글을 삭제할 수 없습니다"),
    BOARD_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "BD004", "게시판 타입이 일치하지 않습니다"),
    INVALID_BOARD_TYPE(HttpStatus.BAD_REQUEST, "BD005", "유효하지 않은 게시판 타입입니다"),
    BOARD_TYPE_NOT_SUPPORT_FILTER(HttpStatus.BAD_REQUEST, "BD006", "이 게시판 타입은 필터를 지원하지 않습니다"),
    BOARD_TYPE_NOT_SUPPORT_BOOKMARK(HttpStatus.BAD_REQUEST, "BD007", "이 게시판 타입은 북마크를 지원하지 않습니다"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "BD008", "카테고리를 찾을 수 없습니다"),
    EVENT_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "BD009", "이벤트 게시글이 아니므로 이벤트 상태를 변경할 수 없습니다"),
    INVALID_EVENT_STATUS(HttpStatus.BAD_REQUEST, "BD010", "유효하지 않은 이벤트 상태값입니다. ACTIVE 또는 ENDED만 가능합니다"),
    GALLERY_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "BD011", "사진 게시글에는 최소 1개 이상의 이미지가 필요합니다"),
    DOCUMENT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "BD012", "자료 게시글에는 최소 1개 이상의 파일이 필요합니다"),

    // Filter
    FILTER_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "FC001", "필터 카테고리를 찾을 수 없습니다"),
    FILTER_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "FC002", "필터 옵션을 찾을 수 없습니다"),
    FILTER_PARENT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "FC003", "부모 필터 옵션을 찾을 수 없습니다"),
    FILTER_CATEGORY_CODE_DUPLICATE(HttpStatus.CONFLICT, "FC004", "이미 존재하는 필터 카테고리 코드입니다"),
    FILTER_OPTION_CODE_DUPLICATE(HttpStatus.CONFLICT, "FC005", "이미 존재하는 필터 옵션 코드입니다"),
    FILTER_CATEGORY_HAS_OPTIONS(HttpStatus.CONFLICT, "FC006", "필터 옵션이 있는 카테고리는 삭제할 수 없습니다"),
    FILTER_OPTION_HAS_CHILDREN(HttpStatus.CONFLICT, "FC007", "하위 필터 옵션이 있는 옵션은 삭제할 수 없습니다"),
    FILTER_INVALID_HIERARCHY(HttpStatus.BAD_REQUEST, "FC008", "유효하지 않은 필터 계층 구조입니다"),
    FILTER_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "FC009", "필터 계층의 최대 깊이를 초과했습니다"),
    FILTER_ENTITY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "FC010", "필터 엔티티 타입이 일치하지 않습니다"),
    FILTER_INVALID_METADATA(HttpStatus.BAD_REQUEST, "FC011", "유효하지 않은 필터 메타데이터입니다"),
    FILTER_BOARD_TYPES_REQUIRED(HttpStatus.BAD_REQUEST, "FC012", "BOARD 타입 필터는 board_types 메타데이터가 필수입니다"),
    FILTER_SAME_OPTION_MIGRATION(HttpStatus.BAD_REQUEST, "FC013", "동일한 필터 옵션으로는 마이그레이션할 수 없습니다"),

    // Notification
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "NT001", "알림 템플릿을 찾을 수 없습니다"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT002", "알림을 찾을 수 없습니다"),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NT003", "알림에 접근할 권한이 없습니다"),

    // Quick Consultation
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "QC001", "상담을 찾을 수 없습니다"),
    CONSULTATION_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "QC002", "비밀번호가 일치하지 않습니다"),
    CONSULTATION_ALREADY_ASSIGNED(HttpStatus.BAD_REQUEST, "QC003", "이미 배정된 상담입니다"),
    CONSULTATION_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "QC004", "상담을 수정할 수 없습니다"),
    CONSULTATION_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "QC005", "필수 동의가 필요합니다"),
    INVALID_CONSULTATION_STATUS(HttpStatus.BAD_REQUEST, "QC006", "유효하지 않은 상담 상태입니다"),

    // Planner Application
    PLANNER_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PA001", "플래너 신청서를 찾을 수 없습니다"),
    PLANNER_APPLICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PA002", "플래너 신청서에 접근할 수 없습니다"),
    PLANNER_APPLICATION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PA003", "유효하지 않은 플래너 신청서 상태입니다"),
    PLANNER_APPLICATION_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "PA004", "첨부파일 전체 크기가 100MB를 초과했습니다"),
    PLANNER_APPLICATION_CANNOT_MODIFY(HttpStatus.BAD_REQUEST, "PA005", "대기중 상태에서만 수정할 수 있습니다"),
    PLANNER_APPLICATION_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "PA006", "대기중 상태에서만 삭제할 수 있습니다"),

    // Popup
    POPUP_NOT_FOUND(HttpStatus.NOT_FOUND, "POP001", "팝업을 찾을 수 없습니다"),
    POPUP_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "POP002", "팝업을 수정할 수 없습니다"),
    POPUP_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "POP003", "팝업을 삭제할 수 없습니다"),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채팅방을 찾을 수 없습니다"),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH002", "채팅 메시지를 찾을 수 없습니다"),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH003", "채팅방에 접근할 권한이 없습니다"),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "CH004", "메시지 내용이 비어있습니다"),
    CHAT_COMPANY_UUID_REQUIRED(HttpStatus.BAD_REQUEST, "CH005", "채팅방 생성 시 업체 UUID가 필요합니다"),

    // Inquiry
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "IQ001", "문의를 찾을 수 없습니다"),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "IQ002", "답변을 찾을 수 없습니다"),
    INQUIRY_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "IQ003", "대기중 상태의 문의만 수정/삭제할 수 있습니다"),
    INVALID_INQUIRY_TYPE(HttpStatus.BAD_REQUEST, "IQ004", "유효하지 않은 문의 유형입니다"),
    INVALID_INQUIRY_STATUS(HttpStatus.BAD_REQUEST, "IQ005", "유효하지 않은 문의 상태입니다"),

    // Partnership Inquiry
    INVALID_PARTNERSHIP_TYPE(HttpStatus.BAD_REQUEST, "PI001", "잘못된 문의 유형입니다"),
    PARTNERSHIP_INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "PI002", "제휴/광고 문의를 찾을 수 없습니다"),
    PARTNERSHIP_INQUIRY_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "PI003", "처리 완료된 문의는 수정할 수 없습니다"),
    INVALID_PARTNERSHIP_STATUS(HttpStatus.BAD_REQUEST, "PI004", "잘못된 문의 상태입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}