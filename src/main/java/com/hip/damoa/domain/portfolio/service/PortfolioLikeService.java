package com.hip.damoa.domain.portfolio.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.company.repository.CompanyPortfolioRepository;
import com.hip.damoa.domain.portfolio.model.PortfolioLike;
import com.hip.damoa.domain.portfolio.repository.PortfolioLikeRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 좋아요 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioLikeService {

    private final PortfolioLikeRepository likeRepository;
    private final CompanyPortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요 토글 (추가/제거)
     *
     * @return true: 좋아요 추가됨, false: 좋아요 제거됨
     */
    @Transactional
    public boolean toggleLike(UUID portfolioUuid, String userEmail) {
        log.info("포트폴리오 좋아요 토글: portfolioUuid={}, userEmail={}", portfolioUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyPortfolio portfolio = portfolioRepository.findByUuid(portfolioUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        boolean exists = likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());

        if (exists) {
            // 좋아요 제거
            likeRepository.deleteByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            portfolio.decrementLikeCount();
            portfolioRepository.save(portfolio);
            log.info("좋아요 제거됨: portfolioId={}, userId={}", portfolio.getId(), user.getId());
            return false;
        } else {
            // 좋아요 추가
            PortfolioLike like = PortfolioLike.of(portfolio, user);
            likeRepository.save(like);
            portfolio.incrementLikeCount();
            portfolioRepository.save(portfolio);
            log.info("좋아요 추가됨: portfolioId={}, userId={}", portfolio.getId(), user.getId());
            return true;
        }
    }

    /**
     * 좋아요 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(UUID portfolioUuid, String userEmail) {
        if (userEmail == null) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        CompanyPortfolio portfolio = portfolioRepository.findByUuid(portfolioUuid).orElse(null);
        if (portfolio == null) {
            return false;
        }

        return likeRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
    }

    /**
     * 사용자의 좋아요 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<PortfolioLike> getMyLikes(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return likeRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    /**
     * 사용자가 좋아요한 포트폴리오 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedPortfolioIds(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return List.of();
        }
        return likeRepository.findPortfolioIdsByUserId(user.getId());
    }
}
