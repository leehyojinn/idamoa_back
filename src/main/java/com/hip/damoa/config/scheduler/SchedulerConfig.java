package com.hip.damoa.config.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler Configuration
 *
 * Spring Scheduler 활성화
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring Scheduler 활성화
    // @Scheduled 어노테이션을 사용한 메서드가 자동으로 실행됩니다
}
