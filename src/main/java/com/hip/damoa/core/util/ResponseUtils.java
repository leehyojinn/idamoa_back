package com.hip.damoa.core.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Response에서 null 값을 안전한 기본값으로 변환하는 유틸리티
 *
 * 프론트엔드에서 null 값에 대해 .replace(), .length 등 호출 시 에러 방지
 */
public final class ResponseUtils {

    private ResponseUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    // ===== String =====

    /**
     * null이면 빈 문자열 반환
     */
    public static String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * null이면 기본값 반환
     */
    public static String safe(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ===== Number =====

    /**
     * Integer: null이면 0 반환
     */
    public static Integer safe(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Integer: null이면 기본값 반환
     */
    public static Integer safe(Integer value, Integer defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Long: null이면 0L 반환
     */
    public static Long safe(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Long: null이면 기본값 반환
     */
    public static Long safe(Long value, Long defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Double: null이면 0.0 반환
     */
    public static Double safe(Double value) {
        return value != null ? value : 0.0;
    }

    /**
     * BigDecimal: null이면 ZERO 반환
     */
    public static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ===== Boolean =====

    /**
     * Boolean: null이면 false 반환
     */
    public static Boolean safe(Boolean value) {
        return value != null ? value : false;
    }

    /**
     * Boolean: null이면 기본값 반환
     */
    public static Boolean safe(Boolean value, Boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ===== Collection =====

    /**
     * List: null이면 빈 리스트 반환
     */
    public static <T> List<T> safeList(List<T> value) {
        return value != null ? value : new ArrayList<>();
    }

    /**
     * Map: null이면 빈 맵 반환
     */
    public static <K, V> Map<K, V> safeMap(Map<K, V> value) {
        return value != null ? value : new HashMap<>();
    }

    // ===== Array =====

    /**
     * String[]: null이면 빈 배열 반환
     */
    public static String[] safeArray(String[] value) {
        return value != null ? value : new String[0];
    }

    /**
     * Long[]: null이면 빈 배열 반환
     */
    public static Long[] safeArray(Long[] value) {
        return value != null ? value : new Long[0];
    }

    // ===== Object (제네릭) =====

    /**
     * 객체: null이면 기본값 반환
     */
    public static <T> T safeOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ===== 문자열 변환 =====

    /**
     * 객체를 문자열로 변환, null이면 빈 문자열
     */
    public static String toStringSafe(Object value) {
        return value != null ? value.toString() : "";
    }

    /**
     * Enum을 문자열로 변환, null이면 빈 문자열
     */
    public static String enumToString(Enum<?> value) {
        return value != null ? value.name() : "";
    }
}
