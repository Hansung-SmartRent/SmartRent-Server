package kr.ac.hansung.smartrent.domain.board.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.ac.hansung.smartrent.domain.board.dto.RequestItemResponse;
import kr.ac.hansung.smartrent.domain.board.entity.BoardRequest;
import kr.ac.hansung.smartrent.domain.board.entity.Inquiry;
import kr.ac.hansung.smartrent.domain.board.entity.PurchaseRequest;
import kr.ac.hansung.smartrent.domain.board.entity.RequestKind;
import kr.ac.hansung.smartrent.domain.board.repository.InquiryRepository;
import kr.ac.hansung.smartrent.domain.board.repository.PurchaseRequestRepository;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.BoardAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의·구매 요청(B1-23, 기획안 11장). 두 표의 모양이 같아 종류(RequestKind)만 바꿔 같은 규칙으로 처리합니다.
 * 학생은 쓰기와 자기 목록, 관리자는 전체 목록(답변 대기 먼저)과 답변. 새 요청은 관리자에게 REQUEST, 답변은 쓴 학생에게 RESPONSE 알림.
 */
@Service
@RequiredArgsConstructor
public class BoardRequestService {

	private final InquiryRepository inquiryRepository;
	private final PurchaseRequestRepository purchaseRepository;
	private final UserRepository userRepository;
	private final BoardAlert boardAlert;
	private final Clock clock;

	@Transactional
	public RequestItemResponse write(RequestKind kind, Long userId, String title, String body) {
		User user = student(userId);
		BoardRequest saved = switch (kind) {
			case INQUIRY -> inquiryRepository.saveAndFlush(Inquiry.builder().userId(userId).title(title.trim()).body(body.trim()).build());
			case PURCHASE_REQUEST -> purchaseRepository.saveAndFlush(
				PurchaseRequest.builder().userId(userId).title(title.trim()).body(body.trim()).build());
		};
		boardAlert.newRequest(kind.name(), saved.getId(), saved.getTitle());
		return RequestItemResponse.of(saved, user.getName());
	}

	/** 내가 쓴 것만, 최신순 */
	@Transactional(readOnly = true)
	public List<RequestItemResponse> mine(RequestKind kind, Long userId) {
		User user = student(userId);
		List<? extends BoardRequest> found = switch (kind) {
			case INQUIRY -> inquiryRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
			case PURCHASE_REQUEST -> purchaseRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
		};
		return found.stream().map(r -> RequestItemResponse.of(r, user.getName())).toList();
	}

	/** 관리자 목록: answered로 거른 뒤 답변 대기 먼저, 그 안에서는 최신순으로 쪽을 나눔 */
	@Transactional(readOnly = true)
	public PageResponse<RequestItemResponse> list(RequestKind kind, Boolean answered, int page, int size) {
		List<? extends BoardRequest> all = switch (kind) {
			case INQUIRY -> inquiryRepository.findAll();
			case PURCHASE_REQUEST -> purchaseRepository.findAll();
		};
		Map<Long, String> names = userRepository.findAllById(all.stream().map(BoardRequest::getUserId).distinct().toList()).stream()
			.collect(Collectors.toMap(User::getId, u -> u.getName() == null ? "" : u.getName()));
		List<RequestItemResponse> items = all.stream()
			.filter(r -> answered == null || (r.getRespondedAt() != null) == answered)
			.sorted(Comparator.comparing((BoardRequest r) -> r.getRespondedAt() != null)
				.thenComparing(BoardRequest::getCreatedAt, Comparator.reverseOrder())
				.thenComparing(BoardRequest::getId, Comparator.reverseOrder()))
			.map(r -> RequestItemResponse.of(r, emptyToNull(names.get(r.getUserId()))))
			.toList();
		return PageResponse.of(items, page, size);
	}

	/** 답변(다시 답하면 내용과 시각을 바꿈). 없는 id는 404 */
	@Transactional
	public RequestItemResponse answer(RequestKind kind, Long id, String response) {
		BoardRequest request = switch (kind) {
			case INQUIRY -> inquiryRepository.findById(id).map(Function.<BoardRequest>identity()).orElse(null);
			case PURCHASE_REQUEST -> purchaseRepository.findById(id).map(Function.<BoardRequest>identity()).orElse(null);
		};
		if (request == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND);
		}
		request.answer(response.trim(), LocalDateTime.now(clock));
		boardAlert.answered(request.getUserId(), kind.name(), request.getId(), request.getTitle());
		String name = userRepository.findById(request.getUserId()).map(User::getName).orElse(null);
		return RequestItemResponse.of(request, name);
	}

	/** 쓰기와 내 목록은 학생만(권한: 학생) */
	private User student(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getRole() != Role.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return user;
	}

	private static String emptyToNull(String s) {
		return s == null || s.isEmpty() ? null : s;
	}
}
