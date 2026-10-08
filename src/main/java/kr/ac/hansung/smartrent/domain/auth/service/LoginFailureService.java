package kr.ac.hansung.smartrent.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.auth.entity.LoginFailure;
import kr.ac.hansung.smartrent.domain.auth.repository.LoginFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 실패 기록(login_failures). 실패하면 바로 401을 던지므로, 그 오류로 기록까지 되돌려지지 않게 따로 저장합니다.
 */
@Service
@RequiredArgsConstructor
public class LoginFailureService {

	private final LoginFailureRepository repository;
	private final Clock clock;

	/** 지금 막혀 있는지. 잠금 시간이 지났으면 기록을 지우고 처음부터 셉니다 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean isLocked(String email) {
		LocalDateTime now = LocalDateTime.now(clock);
		return repository.findById(email).map(f -> {
			if (f.lockExpired(now)) {
				repository.delete(f);
				return false;
			}
			return f.isLocked(now);
		}).orElse(false);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailure(String email) {
		LoginFailure failure = repository.findById(email).orElseGet(() -> LoginFailure.first(email));
		failure.recordFailure(LocalDateTime.now(clock));
		repository.save(failure);
	}

	/** 로그인에 성공하면 기록을 지움 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void clear(String email) {
		repository.deleteById(email);
	}
}
