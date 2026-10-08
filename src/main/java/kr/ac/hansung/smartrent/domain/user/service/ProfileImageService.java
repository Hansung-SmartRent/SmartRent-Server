package kr.ac.hansung.smartrent.domain.user.service;

import kr.ac.hansung.smartrent.domain.user.dto.MeResponse;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.storage.ImageFile;
import kr.ac.hansung.smartrent.global.storage.StorageDeletionService;
import kr.ac.hansung.smartrent.global.storage.StorageKeys;
import kr.ac.hansung.smartrent.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 사진 바꾸기·지우기(B1-08, 기획안 1장).
 * 저장소에 먼저 올리고, 성공했을 때만 DB 키를 바꿈(실패하면 STORAGE_UNAVAILABLE, DB 그대로). 옛 사진은 삭제 작업에 넘김
 */
@Service
@RequiredArgsConstructor
public class ProfileImageService {

	private final UserRepository userRepository;
	private final StorageService storageService;
	private final StorageDeletionService deletionService;
	private final MeAssembler meAssembler;
	private final TransactionTemplate transactionTemplate;

	public MeResponse change(Long userId, MultipartFile file) {
		ImageFile image = ImageFile.from(file);
		String key = StorageKeys.profile(image);
		storageService.put(key, image.content(), image.contentType());
		try {
			return transactionTemplate.execute(status -> {
				User user = load(userId);
				String old = user.getProfileImageKey();
				user.changeProfileImage(key);
				if (old != null) {
					deletionService.request(old);
				}
				return meAssembler.toMe(user);
			});
		}
		catch (RuntimeException e) {
			// DB에 남기지 못했으면 방금 올린 파일도 지울 대상
			deletionService.request(key);
			throw e;
		}
	}

	public MeResponse remove(Long userId) {
		return transactionTemplate.execute(status -> {
			User user = load(userId);
			String old = user.getProfileImageKey();
			if (old != null) {
				user.changeProfileImage(null);
				deletionService.request(old);
			}
			return meAssembler.toMe(user);
		});
	}

	private User load(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
