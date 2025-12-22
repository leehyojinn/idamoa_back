package com.hip.damoa.domain.portfolio.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.company.repository.CompanyPortfolioRepository;
import com.hip.damoa.domain.portfolio.model.PortfolioBookmark;
import com.hip.damoa.domain.portfolio.repository.PortfolioBookmarkRepository;
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
 * 포트폴리오 북마크 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioBookmarkService {

    private final PortfolioBookmarkRepository bookmarkRepository;
    private final CompanyPortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 토글 (추가/제거)
     *
     * @return true: 북마크 추가됨, false: 북마크 제거됨
     */
    @Transactional
    public boolean toggleBookmark(UUID portfolioUuid, String userEmail) {
        log.info("포트폴리오 북마크 토글: portfolioUuid={}, userEmail={}", portfolioUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyPortfolio portfolio = portfolioRepository.findByUuid(portfolioUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        boolean exists = bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());

        if (exists) {
            // 북마크 제거
            bookmarkRepository.deleteByPortfolioIdAndUserId(portfolio.getId(), user.getId());
            portfolio.decrementBookmarkCount();
            portfolioRepository.save(portfolio);
            log.info("북마크 제거됨: portfolioId={}, userId={}", portfolio.getId(), user.getId());
            return false;
        } else {
            // 북마크 추가
            PortfolioBookmark bookmark = PortfolioBookmark.of(portfolio, user);
            bookmarkRepository.save(bookmark);
            portfolio.incrementBookmarkCount();
            portfolioRepository.save(portfolio);
            log.info("북마크 추가됨: portfolioId={}, userId={}", portfolio.getId(), user.getId());
            return true;
        }
    }

    /**
     * 북마크 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(UUID portfolioUuid, String userEmail) {
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

        return bookmarkRepository.existsByPortfolioIdAndUserId(portfolio.getId(), user.getId());
    }

    /**
     * 사용자의 북마크 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<PortfolioBookmark> getMyBookmarks(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    /**
     * 사용자가 북마크한 포트폴리오 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getBookmarkedPortfolioIds(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return List.of();
        }
        return bookmarkRepository.findPortfolioIdsByUserId(user.getId());
    }
}
