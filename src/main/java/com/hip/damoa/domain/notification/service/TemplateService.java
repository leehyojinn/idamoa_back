package com.hip.damoa.domain.notification.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.notification.model.NotificationTemplate;
import com.hip.damoa.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 알림 템플릿 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    // 변수 치환 패턴: {{변수명}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    /**
     * 템플릿 조회 (캐싱 적용)
     *
     * @param code    템플릿 코드
     * @param channel 채널 (EMAIL, SMS, PUSH, KAKAO, IN_APP)
     * @return 템플릿
     */
    @Cacheable(value = "notificationTemplates", key = "#code + '_' + #channel")
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(String code, String channel) {
        log.debug("템플릿 조회: code={}, channel={}", code, channel);

        return templateRepository.findByCodeAndChannel(code, channel)
                .orElseThrow(() -> {
                    log.error("템플릿을 찾을 수 없습니다: code={}, channel={}", code, channel);
                    return new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
                });
    }

    /**
     * 템플릿 렌더링 (변수 치환)
     *
     * @param template  템플릿
     * @param variables 치환할 변수 (key: 변수명, value: 값)
     * @return 렌더링된 제목과 본문
     */
    public RenderedTemplate renderTemplate(NotificationTemplate template, Map<String, String> variables) {
        log.debug("템플릿 렌더링 시작: code={}, variables={}", template.getCode(), variables.keySet());

        String renderedTitle = null;
        if (template.getTitleTemplate() != null) {
            renderedTitle = replaceVariables(template.getTitleTemplate(), variables);
        }

        String renderedContent = replaceVariables(template.getContentTemplate(), variables);

        log.debug("템플릿 렌더링 완료: code={}", template.getCode());
        return new RenderedTemplate(renderedTitle, renderedContent);
    }

    /**
     * 변수 치환 수행
     *
     * @param text      원본 텍스트
     * @param variables 치환할 변수
     * @return 치환된 텍스트
     */
    private String replaceVariables(String text, Map<String, String> variables) {
        if (text == null) {
            return null;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String replacement = variables.getOrDefault(variableName, "");

            // 특수 문자 이스케이프 처리
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 렌더링된 템플릿 결과
     */
    public record RenderedTemplate(String title, String content) {
    }
}
