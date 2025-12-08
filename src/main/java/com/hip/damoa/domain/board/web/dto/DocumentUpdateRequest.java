package com.hip.damoa.domain.board.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
 * Document 게시글 수정 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateRequest {

    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    private Long categoryId;

    // ========== 파일 설정 (두 가지 방식 지원) ==========

    @Schema(description = """
            파일 목록 (개별 가격 설정 방식 - 권장)

            각 파일별로 유료/무료 및 가격을 개별 설정할 수 있습니다.
            이 필드를 사용하면 fileUuids, isPaid, price 필드는 무시됩니다.
            """)
    @Valid
    private List<DocumentFileRequest> files;

    @Schema(description = "[레거시] 파일 UUID 목록 (일괄 가격 설정 방식)")
    @Size(min = 1, message = "파일은 최소 1개 이상 필요합니다")
    private List<String> fileUuids;

    private String thumbnailUuid;

    @Schema(description = "[레거시] 유료 파일 여부 (일괄 적용)")
    private Boolean isPaid;

    @Schema(description = "[레거시] 가격 (원) - 일괄 적용")
    private Integer price;

    private List<Long> filterOptionIds;

    private String[] tags;

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
            for (DocumentFileRequest file : files) {
                if (Boolean.TRUE.equals(file.getIsPaid()) && file.getPrice() != null && file.getPrice() > 0) {
                    priceMap.put(file.getUuid(), file.getPrice());
                }
            }
        } else if (fileUuids != null && Boolean.TRUE.equals(isPaid) && price != null && price > 0) {
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
        if (effectiveUuids == null) {
            return null;
        }

        Map<String, Integer> priceMap = getFilePriceMap();

        Map<String, Object> typeData = new HashMap<>();
        typeData.put("files", effectiveUuids);
        typeData.put("thumbnail", thumbnailUuid != null ? thumbnailUuid : "");

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
