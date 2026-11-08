package kr.ac.hansung.smartrent.domain.user.service;

import kr.ac.hansung.smartrent.domain.auth.repository.LoginFailureRepository;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.domain.warning.service.RetainedWarningService;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴(B1-09, 기획안 1장, DB 설계 4절). 한 트랜잭션으로 처리합니다.
 * 1) B2-17 경고 보관 서비스: 대여 중이면 WITHDRAW_BLOCKED_HELD, 아니면 수령 전 예약을 B2-05 취소 서비스로 취소하고 경고를 6개월 보관
 * 2) 프로필·학생증 사진은 B1-08 삭제 작업에 넘김
 * 3) 로그인 실패 기록(이메일 기준)을 지우고 계정을 지움. 대여 기록·알림·문의·구매 요청·설정·푸시 토큰·리프레시 토큰은 DB의 ON DELETE CASCADE로 함께 지워짐
 */
@Service
@RequiredArgsConstructor
public class WithdrawService {

	private final UserRepository userRepository;
	private final RetainedWarningService retainedWarningService;
	private final StorageDeletionService deletionService;
	private final LoginFailureRepository loginFailureRepository;

	@Transactional
	public void withdraw(Long userId) {
		retainedWarningService.retain(userId);
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getProfileImageKey() != null) {
			deletionService.request(user.getProfileImageKey());
		}
		if (user.getStudentIdImageKey() != null) {
			deletionService.request(user.getStudentIdImageKey());
		}
		loginFailureRepository.deleteById(user.getEmail());
		userRepository.delete(user);
	}
}
