package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 결제 수단 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_user_id", columnList = "user_id")
})
public class PaymentMethod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "method_type", nullable = false, length = 50)
    private String methodType; // CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, KAKAOPAY, NAVERPAY

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "card_company", length = 50)
    private String cardCompany;

    @Column(name = "card_number_masked", length = 20)
    private String cardNumberMasked;

    @Column(name = "card_nickname", length = 100)
    private String cardNickname;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "account_number_masked", length = 50)
    private String accountNumberMasked;

    @Type(JsonBinaryType.class)
    @Column(name = "gateway_data", columnDefinition = "jsonb")
    private Map<String, Object> gatewayData;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
