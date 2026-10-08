package kr.ac.hansung.smartrent.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import kr.ac.hansung.smartrent.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 로그인 응답·내 정보에 쓰는 Me 만들기. 유효 경고 수는 B2-12의 ActiveWarningCounter(그 전까지 0), 프로필 사진은 임시 주소 */
@Component
@RequiredArgsConstructor
public class MeAssembler {

	private final ActiveWarningCounter warningCounter;
	private final StorageService storageService;
	private final Clock clock;

	public MeResponse toMe(User user) {
		String key = user.getProfileImageKey();
		String url = key == null ? null : storageService.temporaryUrl(key);
		return MeResponse.of(user, url, warningCounter.count(user.getId()), LocalDateTime.now(clock));
	}
}
