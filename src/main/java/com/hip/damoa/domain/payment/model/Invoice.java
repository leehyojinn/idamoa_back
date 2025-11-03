package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 세금계산서/영수증
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_payment_id", columnList = "payment_id"),
    @Index(name = "idx_invoices_status", columnList = "status")
})
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "invoice_type", nullable = false, length = 20)
    private String invoiceType; // TAX_INVOICE, RECEIPT, CASH_RECEIPT

    @Column(name = "invoice_number", unique = true, length = 100)
    private String invoiceNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ISSUED, CANCELLED

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_business_number", length = 50)
    private String recipientBusinessNumber;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public void issue() {
        this.status = "ISSUED";
        this.issuedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.cancelledAt = LocalDateTime.now();
    }
}
