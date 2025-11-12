package com.hip.damoa.domain.file.model;

import com.hip.damoa.domain.payment.model.Payment;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 파일 다운로드 로그 엔티티
 *
 * 파일 다운로드 내역을 추적합니다
 */
@Entity
@Table(name = "file_downloads", indexes = {
    @Index(name = "idx_file_downloads_file_id", columnList = "file_id"),
    @Index(name = "idx_file_downloads_user_id", columnList = "user_id"),
    @Index(name = "idx_file_downloads_file_user", columnList = "file_id, user_id"),
    @Index(name = "idx_file_downloads_payment_id", columnList = "payment_id"),
    @Index(name = "idx_file_downloads_is_free", columnList = "is_free")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referer", columnDefinition = "TEXT")
    private String referer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree = true;

    @Column(name = "price_paid", nullable = false)
    private Integer pricePaid = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FileDownload(File file, User user, String ipAddress, String userAgent, String referer,
                        Payment payment, Boolean isFree, Integer pricePaid) {
        this.file = file;
        this.user = user;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.payment = payment;
        this.isFree = isFree != null ? isFree : (payment == null);
        this.pricePaid = pricePaid != null ? pricePaid : 0;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isFree == null) {
            isFree = (payment == null);
        }
    }

    // 비즈니스 메서드
    public boolean isPaid() {
        return !isFree && pricePaid > 0;
    }

    public boolean hasPayment() {
        return payment != null;
    }

    public static FileDownload createFreeDownload(File file, User user, String ipAddress,
                                                   String userAgent, String referer) {
        return FileDownload.builder()
                .file(file)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referer(referer)
                .isFree(true)
                .pricePaid(0)
                .build();
    }

    public static FileDownload createPaidDownload(File file, User user, String ipAddress,
                                                   String userAgent, String referer,
                                                   Payment payment, Integer pricePaid) {
        return FileDownload.builder()
                .file(file)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referer(referer)
                .payment(payment)
                .isFree(false)
                .pricePaid(pricePaid)
                .build();
    }
}
