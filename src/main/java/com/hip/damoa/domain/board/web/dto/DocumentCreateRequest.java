package com.hip.damoa.domain.board.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Document 게시글 생성 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreateRequest {

    @Schema(description = "제목", example = "병원 인테리어 설계도면 공유")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "내용", example = "50평 규모 치과 인테리어 설계도면입니다. 대기실, 진료실, 상담실 포함되어 있습니다.")
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    // ========== 파일 설정 (두 가지 방식 지원) ==========

    @Schema(description = """
            파일 목록 (개별 가격 설정 방식 - 권장)

            각 파일별로 유료/무료 및 가격을 개별 설정할 수 있습니다.
            이 필드를 사용하면 fileUuids, isPaid, price 필드는 무시됩니다.
            """,
            example = """
            [
              {"uuid": "550e8400-e29b-41d4-a716-446655440000", "isPaid": true, "price": 5000},
              {"uuid": "550e8400-e29b-41d4-a716-446655440001", "isPaid": true, "price": 3000},
              {"uuid": "550e8400-e29b-41d4-a716-446655440002", "isPaid": false, "price": 0}
            ]
            """)
    @Valid
    private List<DocumentFileRequest> files;

    @Schema(description = """
            [레거시] 파일 UUID 목록 (일괄 가격 설정 방식)

            모든 파일에 동일한 가격을 적용합니다.
            files 필드가 있으면 이 필드는 무시됩니다.
            """,
            example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]")
    private List<String> fileUuids;

    @Schema(description = "썸네일 이미지 UUID (선택)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String thumbnailUuid;

    @Schema(description = "[레거시] 유료 파일 여부 (일괄 적용) - files 사용 시 무시됨", example = "false")
    private Boolean isPaid;

    @Schema(description = "[레거시] 가격 (원) - 일괄 적용, files 사용 시 무시됨", example = "0")
    private Integer price;

    // ========== 기타 설정 ==========

    @Schema(description = "필터 옵션 ID 목록", example = "[1, 2, 3]")
    private List<Long> filterOptionIds;

    @Schema(description = "태그", example = "[\"치과\", \"50평\", \"모던\"]")
    private String[] tags;

    @Schema(description = "즉시 게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "비공개 여부", example = "false")
    private Boolean isPrivate;

    // ========== Helper Methods (내부용, Swagger에서 숨김) ==========

    /**
     * 파일 UUID 목록 반환 (files 또는 fileUuids에서)
     */
    @JsonIgnore
    public List<String> getEffectiveFileUuids() {
        if (files != null && !files.isEmpty()) {
            return files.stream()
                    .map(DocumentFileRequest::getUuid)
                    .collect(Collectors.toList());
        }
        return fileUuids;
    }

    /**
     * 개별 가격 방식인지 확인
     */
    @JsonIgnore
    public boolean hasIndividualPricing() {
        return files != null && !files.isEmpty();
    }

    /**
     * 파일별 가격 맵 반환 (uuid -> price)
     */
    @JsonIgnore
    public Map<String, Integer> getFilePriceMap() {
        Map<String, Integer> priceMap = new HashMap<>();

        if (files != null && !files.isEmpty()) {
            // 개별 가격 방식
            for (DocumentFileRequest file : files) {
                if (Boolean.TRUE.equals(file.getIsPaid()) && file.getPrice() != null && file.getPrice() > 0) {
                    priceMap.put(file.getUuid(), file.getPrice());
                }
            }
        } else if (fileUuids != null && Boolean.TRUE.equals(isPaid) && price != null && price > 0) {
            // 일괄 가격 방식
            for (String uuid : fileUuids) {
                priceMap.put(uuid, price);
            }
        }

        return priceMap;
    }

    /**
     * type_data JSONB 생성
     */
    @JsonIgnore
    public Map<String, Object> toTypeData() {
        List<String> effectiveUuids = getEffectiveFileUuids();
        Map<String, Integer> priceMap = getFilePriceMap();

        Map<String, Object> typeData = new HashMap<>();
        typeData.put("files", effectiveUuids != null ? effectiveUuids : List.of());
        typeData.put("thumbnail", thumbnailUuid != null ? thumbnailUuid : "");

        // 개별 가격 정보 저장
        if (hasIndividualPricing()) {
            typeData.put("individualPricing", true);
            typeData.put("filePrices", priceMap);
        } else {
            typeData.put("isPaid", isPaid != null ? isPaid : false);
            typeData.put("price", price != null ? price : 0);
        }

        return typeData;
    }
}
