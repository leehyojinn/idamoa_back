package com.hip.damoa.core.exception;

import com.hip.damoa.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 봇/스캐너가 자주 요청하는 무시할 경로 패턴
    private static final Set<String> IGNORED_PATHS = Set.of(
            "/favicon.ico",
            "/robots.txt",
            "/.env",
            "/wp-admin",
            "/wp-login.php",
            "/.git",
            "/phpinfo.php",
            "/admin.php"
    );

    // 무시할 확장자 패턴
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".css", ".js", ".map", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".woff", ".woff2", ".ttf"
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 필드 이름과 에러 메시지를 포함한 상세한 메시지 생성
        String fieldName = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getField()
                : "unknown";
        String rejectedValue = e.getBindingResult().getFieldError() != null
                && e.getBindingResult().getFieldError().getRejectedValue() != null
                ? e.getBindingResult().getFieldError().getRejectedValue().toString()
                : "null";
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 구체적인 에러 메시지 생성
        String detailedMessage = String.format("[%s] 필드 오류: %s (입력값: '%s')",
                fieldName, errorMessage, rejectedValue);

        log.warn("유효성 검증 실패: {}", detailedMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(detailedMessage));
    }

    // 크롤러/봇 요청으로 인해 자주 발생하는 에러 코드 (DEBUG 레벨로 로깅)
    private static final Set<ErrorCode> DEBUG_LEVEL_ERRORS = Set.of(
            ErrorCode.COMPANY_PROFILE_NOT_FOUND,  // CP001 - SEO 크롤러가 삭제된 업체 페이지 방문 시
            ErrorCode.PORTFOLIO_NOT_FOUND         // 삭제된 포트폴리오 접근 시
    );

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        // 커스텀 메시지가 있으면 사용, 없으면 기본 에러 메시지 사용
        final String message = e.getMessage();

        // 크롤러로 인한 빈번한 에러는 DEBUG로, 나머지는 WARN으로 로깅
        if (DEBUG_LEVEL_ERRORS.contains(errorCode)) {
            log.debug("비즈니스 예외 발생 (크롤러 관련): {} - {}", errorCode.getCode(), message);
        } else {
            log.warn("비즈니스 예외 발생: {} - {}", errorCode.getCode(), message);
        }

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("인증 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다. 로그인 후 다시 시도해주세요."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("접근 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {} (허용: {})", e.getMethod(), e.getSupportedHttpMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("지원하지 않는 HTTP 메서드입니다: " + e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        String path = e.getResourcePath();
        // 무시할 경로면 로그 생략
        if (!shouldIgnorePath(path)) {
            log.warn("리소스를 찾을 수 없음: {} {}", e.getHttpMethod(), path);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String path = e.getRequestURL();
        // 무시할 경로면 로그 생략, 그 외에는 DEBUG 레벨로 로깅
        if (!shouldIgnorePath(path)) {
            log.debug("핸들러를 찾을 수 없음: {} {}", e.getHttpMethod(), path);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }

    /**
     * 클라이언트가 연결을 끊은 경우 (Broken pipe)
     * - 사용자가 페이지 이탈, 네트워크 불안정 등
     * - 비즈니스 로직 문제가 아니므로 로깅하지 않음
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException e) {
        // 클라이언트가 이미 연결을 끊었으므로 응답도 의미 없음
        // 로그도 남기지 않음 (빈번하게 발생할 수 있음)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("연결이 끊어졌습니다."));
    }

    /**
     * 무시할 경로인지 확인 (봇/스캐너 요청, 정적 리소스 등)
     */
    private boolean shouldIgnorePath(String path) {
        if (path == null) {
            return false;
        }
        // 정확히 일치하는 경로
        if (IGNORED_PATHS.contains(path)) {
            return true;
        }
        // 확장자 기반 필터링
        String lowerPath = path.toLowerCase();
        for (String ext : IGNORED_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }
        // 특정 디렉토리 패턴
        return lowerPath.startsWith("/wp-") ||
               lowerPath.startsWith("/.") ||
               lowerPath.contains("/php");
    }

    /**
     * 타입 변환 오류 (UUID, Long 등의 파라미터 타입 불일치)
     * - 잘못된 URL 접근 시 발생
     * - 어떤 URL에서 발생했는지 상세 로깅
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("타입 변환 오류 - URL: {} {}, 파라미터: {}, 값: {}, 필요한 타입: {}",
                request.getMethod(),
                request.getRequestURI(),
                e.getName(),
                e.getValue(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("잘못된 파라미터 형식입니다: " + e.getName()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("handleException - URL: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}