package kr.ac.hansung.smartrent.global.security;

import java.time.Clock;
import java.time.LocalDateTime;

import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 예약·대여·연장 전에 학생 상태 확인: 승인 → 정지 순서로 봅니다 */
@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

	private final UserRepository userRepository;
	private final Clock clock;

	public void check(Long userId) {
		check(userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED)));
	}

	public void check(User user) {
		if (user.getApproval() != Approval.APPROVED) {
			throw new BusinessException(ErrorCode.NOT_APPROVED);
		}
		if (user.isSuspended(LocalDateTime.now(clock))) {
			throw new BusinessException(ErrorCode.SUSPENDED);
		}
	}
}
