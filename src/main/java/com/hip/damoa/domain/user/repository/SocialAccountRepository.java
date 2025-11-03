package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.OAuthProvider;
import com.hip.damoa.domain.user.model.SocialAccount;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    Optional<SocialAccount> findByUserAndProvider(User user, OAuthProvider provider);

    List<SocialAccount> findByUser(User user);

    long countByUser(User user);

    boolean existsByUserAndProvider(User user, OAuthProvider provider);
}
