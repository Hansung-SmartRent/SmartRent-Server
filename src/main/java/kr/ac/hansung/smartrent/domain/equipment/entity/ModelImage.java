package kr.ac.hansung.smartrent.domain.equipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 모델 사진(1~3장, 1번이 목록 썸네일) (DB 설계 3절 model_images) */
@Getter
@Entity
@Table(name = "model_images")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "model_id", nullable = false)
	private Long modelId;

	@Column(name = "image_key", nullable = false, length = 200)
	private String imageKey;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
