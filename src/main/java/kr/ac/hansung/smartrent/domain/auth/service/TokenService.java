package kr.ac.hansung.smartrent.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import kr.ac.hansung.smartrent.domain.auth.dto.AuthTokensResponse;
import kr.ac.hansung.smartrent.domain.auth.entity.RefreshToken;
import kr.ac.hansung.smartrent.domain.auth.repository.RefreshTokenRepository;
import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import kr.ac.hansung.smartrent.global.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 액세스 토큰(JWT 30분)과 리프레시 토큰(임의 문자열 14일) 발급.
 * 리프레시 토큰은 원문을 저장하지 않고 SHA-256 해시만 refresh_tokens에 둡니다. 새로 받을 때마다 옛 토큰은 지우고 새로 줍니다.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final ActiveWarningCounter warningCounter;
	private final Clock clock;

	@Transactional
	public AuthTokensResponse issue(User user) {
		Instant now = clock.instant();
		long accessSeconds = properties.accessMinutes() * 60;
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("smartrent")
			.subject(String.valueOf(user.getId()))
			.claim("role", user.getRole().name())
			.issuedAt(now)
			.expiresAt(now.plusSeconds(accessSeconds))
			.build();
		String access = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		byte[] raw = new byte[32];
		RANDOM.nextBytes(raw);
		String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		refreshTokenRepository.save(RefreshToken.builder()
			.userId(user.getId())
			.tokenHash(hash(refresh))
			.expiresAt(LocalDateTime.now(clock).plusDays(properties.refreshDays()))
			.build());

		MeResponse me = MeResponse.of(user, warningCounter.count(user.getId()), LocalDateTime.now(clock));
		return new AuthTokensResponse(access, refresh, accessSeconds, me);
	}

	/** 저장된 토큰과 맞지 않거나, 지워졌거나, 기한이 지났으면 UNAUTHORIZED */
	@Transactional
	public AuthTokensResponse refresh(String rawRefreshToken) {
		RefreshToken saved = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		refreshTokenRepository.delete(saved);
		if (!saved.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		User user = userRepository.findById(saved.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		return issue(user);
	}

	/** 로그아웃: 그 사용자의 토큰이면 지움. 이미 없으면 아무것도 안 함 */
	@Transactional
	public void revoke(Long userId, String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
			.filter(t -> t.getUserId().equals(userId))
			.ifPresent(refreshTokenRepository::delete);
	}

	static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
