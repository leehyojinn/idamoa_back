package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 상태 업데이트 스케줄러
 *
 * 종료 날짜가 지난 이벤트를 자동으로 종료 상태로 변경
 *
 * 프로세스:
 * 1. EVENT 타입 게시글 중 종료일(eventEndDate)이 현재 시각보다 이전인 게시글 조회
 * 2. eventStatus가 ACTIVE이거나 NULL인 경우 ENDED로 변경
 * 3. 매시간 정각에 실행 (정시마다)
 *
 * 참고:
 * - typeData JSONB 필드에서 eventEndDate를 추출하여 비교
 * - event_status 컬럼이 ENDED가 아닌 모든 종료된 이벤트 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final BoardRepository boardRepository;

    /**
     * 이벤트 상태 업데이트 스케줄러
     * 매시간 정각에 실행 (예: 09:00, 10:00, 11:00...)
     * 애플리케이션 시작 후 30초 뒤에 첫 실행
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")  // 매시간 정각
    @Transactional
    public void updateExpiredEventStatus() {
        try {
            log.info("=== 이벤트 상태 업데이트 스케줄러 시작 ===");

            // 종료되어야 할 이벤트 조회
            List<Board> expiredEvents = boardRepository.findExpiredActiveEvents();

            if (expiredEvents.isEmpty()) {
                log.info("종료 처리할 이벤트 없음 (정상)");
                return;
            }

            log.info("종료 처리할 이벤트 {}개 발견", expiredEvents.size());

            int successCount = 0;
            int failCount = 0;

            for (Board event : expiredEvents) {
                try {
                    // 이벤트 종료 처리
                    event.endEvent();
                    boardRepository.save(event);

                    successCount++;
                    log.info("이벤트 종료 처리 성공: uuid={}, title={}, eventStatus={}",
                            event.getUuid(), event.getTitle(), event.getEventStatus());

                } catch (Exception e) {
                    failCount++;
                    log.error("이벤트 종료 처리 실패: uuid={}, title={}, error={}",
                            event.getUuid(), event.getTitle(), e.getMessage());
                }
            }

            log.info("이벤트 상태 업데이트 완료: 총 {}개, 성공 {}개, 실패 {}개",
                    expiredEvents.size(), successCount, failCount);

        } catch (Exception e) {
            log.error("이벤트 상태 업데이트 작업 중 오류 발생", e);
        }
    }

    /**
     * 즉시 업데이트 실행 (테스트/수동 실행용)
     *
     * API 엔드포인트나 관리자 기능에서 호출하여 즉시 실행 가능
     *
     * @return 업데이트된 이벤트 개수
     */
    @Transactional
    public int updateExpiredEventStatusNow() {
        log.info("=== 수동 이벤트 상태 업데이트 시작 ===");

        List<Board> expiredEvents = boardRepository.findExpiredActiveEvents();

        int count = 0;
        for (Board event : expiredEvents) {
            try {
                event.endEvent();
                boardRepository.save(event);
                count++;
                log.info("이벤트 종료 처리: uuid={}, title={}", event.getUuid(), event.getTitle());
            } catch (Exception e) {
                log.error("이벤트 종료 처리 실패: uuid={}, error={}", event.getUuid(), e.getMessage());
            }
        }

        log.info("수동 업데이트 완료: {}개 이벤트 종료 처리", count);
        return count;
    }

    /**
     * 초기 실행 (애플리케이션 시작 시)
     * 애플리케이션 시작 30초 후에 한번 실행하여 누적된 종료 이벤트 처리
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)  // 30초 후 한번만 실행
    @Transactional
    public void initialEventStatusUpdate() {
        log.info("=== 초기 이벤트 상태 업데이트 실행 ===");
        int updated = updateExpiredEventStatusNow();
        log.info("초기 업데이트 완료: {}개 이벤트 처리", updated);
    }
}