package com.hip.damoa.domain.user.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.jwt.JwtTokenProvider;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.domain.user.web.dto.UserSignupRequest;
import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    @Transactional
    public TokenInfo login(String email, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        return jwtTokenProvider.generateToken(authentication);
    }

    public String startSignup(UserSignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 예시, 실제로는 이메일 중복 에러 코드 사용
        }

        String signupToken = UUID.randomUUID().toString();
        redisService.setData(signupToken, request, Duration.ofMinutes(10));
        return signupToken;
    }

    @Transactional
    public Long completeSignup(String signupToken) {
        Object cachedData = redisService.getData(signupToken);

        if (cachedData == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 예시, 실제로는 만료 또는 잘못된 토큰 에러 코드 사용
        }

        UserSignupRequest signupRequest = (UserSignupRequest) cachedData;
        User user = signupRequest.toEntity(passwordEncoder);
        userRepository.save(user);

        redisService.deleteData(signupToken);

        return user.getId();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 유저를 찾을 수 없습니다."));
    }
}
