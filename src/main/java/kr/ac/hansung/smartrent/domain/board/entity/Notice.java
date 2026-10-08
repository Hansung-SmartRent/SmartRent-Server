package kr.ac.hansung.smartrent.domain.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.hansung.smartrent.global.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자가 쓰는 공지 (DB 설계 3절 notices) */
@Getter
@Entity
@Table(name = "notices")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notice extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "body", nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "author_id", nullable = false)
	private Long authorId;
}
