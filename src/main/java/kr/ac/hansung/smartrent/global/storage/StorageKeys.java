package kr.ac.hansung.smartrent.global.storage;

import java.util.UUID;

/** 저장소 안 폴더(접두어) 나누기: student-id/, profile/, model/ (외부 연결 5절). 파일 이름에 학생 정보를 넣지 않습니다 */
public final class StorageKeys {

	private StorageKeys() {
	}

	public static String profile(ImageFile image) {
		return "profile/" + UUID.randomUUID() + "." + image.extension();
	}

	public static String studentId(ImageFile image) {
		return "student-id/" + UUID.randomUUID() + "." + image.extension();
	}

	public static String model(Long modelId, ImageFile image) {
		return "model/" + modelId + "/" + UUID.randomUUID() + "." + image.extension();
	}
}
