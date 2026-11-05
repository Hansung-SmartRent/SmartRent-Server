package kr.ac.hansung.smartrent.domain.user.service;

import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.AccountAlert;
import kr.ac.hansung.smartrent.global.storage.ImageFile;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionService;
import kr.ac.hansung.smartrent.global.storage.StorageKeys;
import kr.ac.hansung.smartrent.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 학생증 제출·재제출(B1-07, 기획안 1장). 학생이 앱에서 자른 사진 한 장을 공통 업로드 규칙(jpg·png, 10MB)만 보고 저장하고
 * approval을 NONE·REJECTED에서 PENDING으로 바꿉니다(DB 설계 7절). 사진을 먼저 올리고, DB에 남기지 못하면 방금 올린 사진을 지울 대상으로 넘깁니다.
 */
@Service
@RequiredArgsConstructor
public class StudentIdService {

	private final UserRepository userRepository;
	private final StorageService storageService;
	private final StorageDeletionService deletionService;
	private final MeAssembler meAssembler;
	private final AccountAlert accountAlert;
	private final TransactionTemplate transactionTemplate;

	public MeResponse submit(Long userId, String name, String studentNumber, MultipartFile file) {
		String trimmedName = name.trim();
		String number = studentNumber.trim();
		check(load(userId), number);
		ImageFile image = ImageFile.from(file);
		String key = StorageKeys.studentId(image);
		storageService.put(key, image.content(), image.contentType());
		MeResponse me;
		try {
			me = transactionTemplate.execute(status -> {
				User user = load(userId);
				check(user, number);
				String old = user.getStudentIdImageKey();
				user.submitStudentId(trimmedName, number, key);
				if (old != null) {
					deletionService.request(old);
				}
				return meAssembler.toMe(userRepository.saveAndFlush(user));
			});
		}
		catch (RuntimeException e) {
			deletionService.request(key);
			throw e;
		}
		accountAlert.toAdmins("학생증 승인 요청", trimmedName + " 학생이 학생증을 제출했습니다.", userId);
		return me;
	}

	/** 학생만, 미제출·반려 상태에서만, 다른 계정이 쓰는 학번은 거절 */
	private void check(User user, String studentNumber) {
		if (user.getRole() != Role.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (user.getApproval() != Approval.NONE && user.getApproval() != Approval.REJECTED) {
			throw new BusinessException(ErrorCode.APPROVAL_NOT_PENDING);
		}
		userRepository.findByStudentNumber(studentNumber)
			.filter(other -> !other.getId().equals(user.getId()))
			.ifPresent(other -> {
				throw new BusinessException(ErrorCode.STUDENT_NUMBER_DUPLICATE);
			});
	}

	private User load(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
