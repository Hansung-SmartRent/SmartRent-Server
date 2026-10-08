package kr.ac.hansung.smartrent.support;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** 시험용 액세스 토큰 만들기(서버와 같은 키·형식) */
public final class TestTokens {

	private TestTokens() {
	}

	public static String bearer(JwtEncoder encoder, long userId, String role) {
		return bearer(encoder, userId, role, Instant.now().plusSeconds(1800));
	}

	public static String bearer(JwtEncoder encoder, long userId, String role, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("smartrent")
			.subject(String.valueOf(userId))
			.claim("role", role)
			.issuedAt(expiresAt.minusSeconds(1800))
			.expiresAt(expiresAt)
			.build();
		return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}
}
