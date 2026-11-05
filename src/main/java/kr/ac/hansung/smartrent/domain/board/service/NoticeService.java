package kr.ac.hansung.smartrent.domain.board.service;

import kr.ac.hansung.smartrent.domain.board.dto.NoticeResponse;
import kr.ac.hansung.smartrent.domain.board.entity.Notice;
import kr.ac.hansung.smartrent.domain.board.repository.NoticeRepository;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.NoticeAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공지(B1-22, 기획안 11장). 학생은 최신순으로 보고, 관리자는 쓰기·수정·삭제. 새 공지는 학생에게 NOTICE 알림 */
@Service
@RequiredArgsConstructor
public class NoticeService {

	private final NoticeRepository noticeRepository;
	private final NoticeAlert noticeAlert;

	@Transactional(readOnly = true)
	public PageResponse<NoticeResponse> list(int page, int size) {
		Page<Notice> found = noticeRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
		return new PageResponse<>(found.map(NoticeResponse::of).getContent(),
			new PageResponse.Meta(page, size, found.getTotalElements(), found.getTotalPages()));
	}

	@Transactional
	public NoticeResponse write(String title, String body, Long adminId) {
		Notice notice = noticeRepository.saveAndFlush(Notice.builder().title(title.trim()).body(body.trim()).authorId(adminId).build());
		noticeAlert.toStudents(notice.getId(), notice.getTitle(), notice.getBody());
		return NoticeResponse.of(notice);
	}

	@Transactional
	public NoticeResponse edit(Long id, String title, String body) {
		Notice notice = load(id);
		notice.edit(title.trim(), body.trim());
		return NoticeResponse.of(noticeRepository.saveAndFlush(notice));
	}

	@Transactional
	public void delete(Long id) {
		noticeRepository.delete(load(id));
	}

	private Notice load(Long id) {
		return noticeRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
	}
}
