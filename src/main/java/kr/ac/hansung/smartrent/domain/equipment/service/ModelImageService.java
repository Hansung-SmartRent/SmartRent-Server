package kr.ac.hansung.smartrent.domain.equipment.service;

import java.util.List;

import kr.ac.hansung.smartrent.domain.equipment.dto.AdminModelDetailResponse;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.entity.ModelImage;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentModelRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.equipment.repository.ModelImageRepository;
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

/** 관리자 모델 사진 올리기(B1-08): 모델당 1~3장, 1번이 목록 썸네일. 저장소에 먼저 올리고 성공하면 DB에 남김 */
@Service
@RequiredArgsConstructor
public class ModelImageService {

	public static final int MAX_IMAGES = 3;

	private final EquipmentModelRepository modelRepository;
	private final ModelImageRepository imageRepository;
	private final EquipmentUnitRepository unitRepository;
	private final StorageService storageService;
	private final StorageDeletionService deletionService;
	private final TransactionTemplate transactionTemplate;

	public AdminModelDetailResponse add(Long modelId, MultipartFile file) {
		EquipmentModel model = modelRepository.findById(modelId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		if (imageRepository.countByModelId(modelId) >= MAX_IMAGES) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "모델 사진은 " + MAX_IMAGES + "장까지 올릴 수 있습니다.");
		}
		ImageFile image = ImageFile.from(file);
		String key = StorageKeys.model(modelId, image);
		storageService.put(key, image.content(), image.contentType());
		try {
			return transactionTemplate.execute(status -> {
				long count = imageRepository.countByModelId(modelId);
				if (count >= MAX_IMAGES) {
					throw new BusinessException(ErrorCode.VALIDATION_ERROR, "모델 사진은 " + MAX_IMAGES + "장까지 올릴 수 있습니다.");
				}
				imageRepository.save(ModelImage.builder().modelId(modelId).imageKey(key).sortOrder((int) count + 1).build());
				return detail(model);
			});
		}
		catch (RuntimeException e) {
			deletionService.request(key);
			throw e;
		}
	}

	private AdminModelDetailResponse detail(EquipmentModel model) {
		List<String> urls = imageRepository.findAllByModelIdOrderBySortOrderAsc(model.getId()).stream()
			.map(i -> storageService.temporaryUrl(i.getImageKey())).toList();
		return AdminModelDetailResponse.of(model, urls, unitRepository.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId()));
	}
}
