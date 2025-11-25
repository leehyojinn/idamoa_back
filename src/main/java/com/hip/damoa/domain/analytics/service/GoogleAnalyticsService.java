package com.hip.damoa.domain.analytics.service;

import com.google.analytics.data.v1beta.*;
import com.hip.damoa.domain.analytics.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Analytics Data API 서비스
 * google.analytics.enabled=true일 때만 활성화됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "google.analytics.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class GoogleAnalyticsService {

    private final BetaAnalyticsDataClient analyticsDataClient;

    @Value("${google.analytics.property-id}")
    private String propertyId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 일별 트래픽 데이터 조회
     */
    public List<DailyTrafficData> getDailyTraffic(LocalDate startDate, LocalDate endDate) {
        log.info("일별 트래픽 조회 시작: startDate={}, endDate={}", startDate, endDate);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("date"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setDimension(OrderBy.DimensionOrderBy.newBuilder()
                                    .setDimensionName("date")
                                    .setOrderType(OrderBy.DimensionOrderBy.OrderType.ALPHANUMERIC)))
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<DailyTrafficData> result = response.getRowsList().stream()
                    .map(row -> DailyTrafficData.builder()
                            .date(formatDate(row.getDimensionValues(0).getValue()))
                            .activeUsers(Long.parseLong(row.getMetricValues(0).getValue()))
                            .sessions(Long.parseLong(row.getMetricValues(1).getValue()))
                            .screenPageViews(Long.parseLong(row.getMetricValues(2).getValue()))
                            .avgSessionDuration(Double.parseDouble(row.getMetricValues(3).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("일별 트래픽 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("일별 트래픽 조회 실패", e);
            throw new RuntimeException("일별 트래픽 조회 중 오류 발생", e);
        }
    }

    /**
     * 페이지별 조회수 데이터 조회
     */
    public List<PageViewData> getPageViews(LocalDate startDate, LocalDate endDate, int limit) {
        log.info("페이지별 조회수 조회 시작: startDate={}, endDate={}, limit={}", startDate, endDate, limit);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("pagePath"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("screenPageViews"))
                            .setDesc(true))
                    .setLimit(limit)
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<PageViewData> result = response.getRowsList().stream()
                    .map(row -> PageViewData.builder()
                            .pagePath(row.getDimensionValues(0).getValue())
                            .screenPageViews(Long.parseLong(row.getMetricValues(0).getValue()))
                            .activeUsers(Long.parseLong(row.getMetricValues(1).getValue()))
                            .avgSessionDuration(Double.parseDouble(row.getMetricValues(2).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("페이지별 조회수 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("페이지별 조회수 조회 실패", e);
            throw new RuntimeException("페이지별 조회수 조회 중 오류 발생", e);
        }
    }

    /**
     * 특정 페이지 경로의 통계 조회
     */
    public PageViewData getPageStatsByPath(String pagePath, LocalDate startDate, LocalDate endDate) {
        log.info("특정 페이지 통계 조회 시작: pagePath={}, startDate={}, endDate={}",
                pagePath, startDate, endDate);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("pagePath"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .setDimensionFilter(FilterExpression.newBuilder()
                            .setFilter(Filter.newBuilder()
                                    .setFieldName("pagePath")
                                    .setStringFilter(Filter.StringFilter.newBuilder()
                                            .setMatchType(Filter.StringFilter.MatchType.EXACT)
                                            .setValue(pagePath))))
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            if (response.getRowsCount() == 0) {
                return PageViewData.builder()
                        .pagePath(pagePath)
                        .screenPageViews(0L)
                        .activeUsers(0L)
                        .avgSessionDuration(0.0)
                        .build();
            }

            Row row = response.getRows(0);
            PageViewData result = PageViewData.builder()
                    .pagePath(row.getDimensionValues(0).getValue())
                    .screenPageViews(Long.parseLong(row.getMetricValues(0).getValue()))
                    .activeUsers(Long.parseLong(row.getMetricValues(1).getValue()))
                    .avgSessionDuration(Double.parseDouble(row.getMetricValues(2).getValue()))
                    .build();

            log.info("특정 페이지 통계 조회 완료: {}", pagePath);
            return result;

        } catch (Exception e) {
            log.error("특정 페이지 통계 조회 실패", e);
            throw new RuntimeException("특정 페이지 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * 실시간 활성 사용자 수 조회
     */
    public RealtimeUsersResponse getRealtimeUsers() {
        log.info("실시간 활성 사용자 수 조회 시작");

        try {
            RunRealtimeReportRequest request = RunRealtimeReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .build();

            RunRealtimeReportResponse response = analyticsDataClient.runRealtimeReport(request);

            Long activeUsers = 0L;
            if (response.getRowsCount() > 0) {
                activeUsers = Long.parseLong(response.getRows(0).getMetricValues(0).getValue());
            }

            RealtimeUsersResponse result = RealtimeUsersResponse.builder()
                    .activeUsers(activeUsers)
                    .build();

            log.info("실시간 활성 사용자 수 조회 완료: {} 명", activeUsers);
            return result;

        } catch (Exception e) {
            log.error("실시간 활성 사용자 수 조회 실패", e);
            throw new RuntimeException("실시간 활성 사용자 수 조회 중 오류 발생", e);
        }
    }

    /**
     * 요약 통계 조회 (대시보드용)
     */
    public SummaryStatsResponse getSummaryStats(LocalDate startDate, LocalDate endDate) {
        log.info("요약 통계 조회 시작: startDate={}, endDate={}", startDate, endDate);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("bounceRate"))
                    .addMetrics(Metric.newBuilder().setName("engagementRate"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            if (response.getRowsCount() == 0) {
                return SummaryStatsResponse.builder()
                        .activeUsers(0L)
                        .sessions(0L)
                        .screenPageViews(0L)
                        .bounceRate(0.0)
                        .engagementRate(0.0)
                        .avgSessionDuration(0.0)
                        .build();
            }

            Row row = response.getRows(0);
            SummaryStatsResponse result = SummaryStatsResponse.builder()
                    .activeUsers(Long.parseLong(row.getMetricValues(0).getValue()))
                    .sessions(Long.parseLong(row.getMetricValues(1).getValue()))
                    .screenPageViews(Long.parseLong(row.getMetricValues(2).getValue()))
                    .bounceRate(Double.parseDouble(row.getMetricValues(3).getValue()) * 100)
                    .engagementRate(Double.parseDouble(row.getMetricValues(4).getValue()) * 100)
                    .avgSessionDuration(Double.parseDouble(row.getMetricValues(5).getValue()))
                    .build();

            log.info("요약 통계 조회 완료");
            return result;

        } catch (Exception e) {
            log.error("요약 통계 조회 실패", e);
            throw new RuntimeException("요약 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * 이벤트별 카운트 데이터 조회
     */
    public List<EventCountData> getEventCounts(LocalDate startDate, LocalDate endDate, int limit) {
        log.info("이벤트 카운트 조회 시작: startDate={}, endDate={}, limit={}", startDate, endDate, limit);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("eventName"))
                    .addMetrics(Metric.newBuilder().setName("eventCount"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("eventCount"))
                            .setDesc(true))
                    .setLimit(limit)
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<EventCountData> result = response.getRowsList().stream()
                    .map(row -> EventCountData.builder()
                            .eventName(row.getDimensionValues(0).getValue())
                            .eventCount(Long.parseLong(row.getMetricValues(0).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("이벤트 카운트 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("이벤트 카운트 조회 실패", e);
            throw new RuntimeException("이벤트 카운트 조회 중 오류 발생", e);
        }
    }

    /**
     * 사용자 획득 채널 데이터 조회
     */
    public List<AcquisitionChannelData> getAcquisitionChannels(LocalDate startDate, LocalDate endDate, int limit) {
        log.info("획득 채널 조회 시작: startDate={}, endDate={}, limit={}", startDate, endDate, limit);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("sessionSource"))
                    .addDimensions(Dimension.newBuilder().setName("sessionMedium"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("newUsers"))
                    .addMetrics(Metric.newBuilder().setName("engagementRate"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("sessions"))
                            .setDesc(true))
                    .setLimit(limit)
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<AcquisitionChannelData> result = response.getRowsList().stream()
                    .map(row -> AcquisitionChannelData.builder()
                            .source(row.getDimensionValues(0).getValue())
                            .medium(row.getDimensionValues(1).getValue())
                            .sessions(Long.parseLong(row.getMetricValues(0).getValue()))
                            .newUsers(Long.parseLong(row.getMetricValues(1).getValue()))
                            .engagementRate(Double.parseDouble(row.getMetricValues(2).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("획득 채널 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("획득 채널 조회 실패", e);
            throw new RuntimeException("획득 채널 조회 중 오류 발생", e);
        }
    }

    /**
     * 기기별 통계 조회
     */
    public List<DeviceStatsData> getDeviceStats(LocalDate startDate, LocalDate endDate) {
        log.info("기기별 통계 조회 시작: startDate={}, endDate={}", startDate, endDate);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("deviceCategory"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("bounceRate"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("sessions"))
                            .setDesc(true))
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<DeviceStatsData> result = response.getRowsList().stream()
                    .map(row -> DeviceStatsData.builder()
                            .deviceCategory(row.getDimensionValues(0).getValue())
                            .sessions(Long.parseLong(row.getMetricValues(0).getValue()))
                            .users(Long.parseLong(row.getMetricValues(1).getValue()))
                            .bounceRate(Double.parseDouble(row.getMetricValues(2).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("기기별 통계 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("기기별 통계 조회 실패", e);
            throw new RuntimeException("기기별 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * 지역별 통계 조회
     */
    public List<LocationStatsData> getLocationStats(LocalDate startDate, LocalDate endDate, int limit) {
        log.info("지역별 통계 조회 시작: startDate={}, endDate={}, limit={}", startDate, endDate, limit);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("country"))
                    .addDimensions(Dimension.newBuilder().setName("city"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("sessions"))
                            .setDesc(true))
                    .setLimit(limit)
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<LocationStatsData> result = response.getRowsList().stream()
                    .map(row -> LocationStatsData.builder()
                            .country(row.getDimensionValues(0).getValue())
                            .city(row.getDimensionValues(1).getValue())
                            .sessions(Long.parseLong(row.getMetricValues(0).getValue()))
                            .users(Long.parseLong(row.getMetricValues(1).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("지역별 통계 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("지역별 통계 조회 실패", e);
            throw new RuntimeException("지역별 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * 브라우저별 통계 조회
     */
    public List<BrowserStatsData> getBrowserStats(LocalDate startDate, LocalDate endDate, int limit) {
        log.info("브라우저별 통계 조회 시작: startDate={}, endDate={}, limit={}", startDate, endDate, limit);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addDimensions(Dimension.newBuilder().setName("browser"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                    .setMetricName("sessions"))
                            .setDesc(true))
                    .setLimit(limit)
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            List<BrowserStatsData> result = response.getRowsList().stream()
                    .map(row -> BrowserStatsData.builder()
                            .browser(row.getDimensionValues(0).getValue())
                            .sessions(Long.parseLong(row.getMetricValues(0).getValue()))
                            .users(Long.parseLong(row.getMetricValues(1).getValue()))
                            .build())
                    .collect(Collectors.toList());

            log.info("브라우저별 통계 조회 완료: {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("브라우저별 통계 조회 실패", e);
            throw new RuntimeException("브라우저별 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * 참여도 지표 조회
     */
    public EngagementMetricsResponse getEngagementMetrics(LocalDate startDate, LocalDate endDate) {
        log.info("참여도 지표 조회 시작: startDate={}, endDate={}", startDate, endDate);

        try {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addMetrics(Metric.newBuilder().setName("bounceRate"))
                    .addMetrics(Metric.newBuilder().setName("engagementRate"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViewsPerSession"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(startDate.format(DATE_FORMATTER))
                            .setEndDate(endDate.format(DATE_FORMATTER)))
                    .build();

            RunReportResponse response = analyticsDataClient.runReport(request);

            if (response.getRowsCount() == 0) {
                return EngagementMetricsResponse.builder()
                        .bounceRate(0.0)
                        .engagementRate(0.0)
                        .averageSessionDuration(0.0)
                        .pageViewsPerSession(0.0)
                        .build();
            }

            Row row = response.getRows(0);
            EngagementMetricsResponse result = EngagementMetricsResponse.builder()
                    .bounceRate(Double.parseDouble(row.getMetricValues(0).getValue()))
                    .engagementRate(Double.parseDouble(row.getMetricValues(1).getValue()))
                    .averageSessionDuration(Double.parseDouble(row.getMetricValues(2).getValue()))
                    .pageViewsPerSession(Double.parseDouble(row.getMetricValues(3).getValue()))
                    .build();

            log.info("참여도 지표 조회 완료");
            return result;

        } catch (Exception e) {
            log.error("참여도 지표 조회 실패", e);
            throw new RuntimeException("참여도 지표 조회 중 오류 발생", e);
        }
    }

    /**
     * 기간 비교 통계 조회
     */
    public ComparisonStatsResponse getComparisonStats(
            LocalDate currentStart, LocalDate currentEnd,
            LocalDate previousStart, LocalDate previousEnd) {
        log.info("기간 비교 통계 조회 시작: current=[{} ~ {}], previous=[{} ~ {}]",
                currentStart, currentEnd, previousStart, previousEnd);

        try {
            // 현재 기간 데이터
            RunReportRequest currentRequest = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("bounceRate"))
                    .addMetrics(Metric.newBuilder().setName("engagementRate"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(currentStart.format(DATE_FORMATTER))
                            .setEndDate(currentEnd.format(DATE_FORMATTER)))
                    .build();

            RunReportResponse currentResponse = analyticsDataClient.runReport(currentRequest);

            // 이전 기간 데이터
            RunReportRequest previousRequest = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("screenPageViews"))
                    .addMetrics(Metric.newBuilder().setName("bounceRate"))
                    .addMetrics(Metric.newBuilder().setName("engagementRate"))
                    .addMetrics(Metric.newBuilder().setName("averageSessionDuration"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(previousStart.format(DATE_FORMATTER))
                            .setEndDate(previousEnd.format(DATE_FORMATTER)))
                    .build();

            RunReportResponse previousResponse = analyticsDataClient.runReport(previousRequest);

            // 현재 기간 데이터 변환
            ComparisonStatsResponse.SummaryStatsData currentData = buildSummaryStatsData(currentResponse);

            // 이전 기간 데이터 변환
            ComparisonStatsResponse.SummaryStatsData previousData = buildSummaryStatsData(previousResponse);

            ComparisonStatsResponse result = ComparisonStatsResponse.builder()
                    .current(currentData)
                    .previous(previousData)
                    .build();

            log.info("기간 비교 통계 조회 완료");
            return result;

        } catch (Exception e) {
            log.error("기간 비교 통계 조회 실패", e);
            throw new RuntimeException("기간 비교 통계 조회 중 오류 발생", e);
        }
    }

    /**
     * RunReportResponse를 SummaryStatsData로 변환
     */
    private ComparisonStatsResponse.SummaryStatsData buildSummaryStatsData(RunReportResponse response) {
        if (response.getRowsCount() == 0) {
            return ComparisonStatsResponse.SummaryStatsData.builder()
                    .activeUsers(0L)
                    .sessions(0L)
                    .pageViews(0L)
                    .bounceRate(0.0)
                    .engagementRate(0.0)
                    .averageSessionDuration(0.0)
                    .build();
        }

        Row row = response.getRows(0);
        return ComparisonStatsResponse.SummaryStatsData.builder()
                .activeUsers(Long.parseLong(row.getMetricValues(0).getValue()))
                .sessions(Long.parseLong(row.getMetricValues(1).getValue()))
                .pageViews(Long.parseLong(row.getMetricValues(2).getValue()))
                .bounceRate(Double.parseDouble(row.getMetricValues(3).getValue()))
                .engagementRate(Double.parseDouble(row.getMetricValues(4).getValue()))
                .averageSessionDuration(Double.parseDouble(row.getMetricValues(5).getValue()))
                .build();
    }

    /**
     * 날짜 형식 변환 (YYYYMMDD -> YYYY-MM-DD)
     */
    private String formatDate(String dateStr) {
        if (dateStr.length() == 8) {
            return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        }
        return dateStr;
    }
}
