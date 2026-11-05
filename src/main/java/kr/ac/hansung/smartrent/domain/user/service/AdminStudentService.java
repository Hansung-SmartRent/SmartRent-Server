package kr.ac.hansung.smartrent.domain.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import kr.ac.hansung.smartrent.domain.user.dto.AdminStudentDetailResponse;
import kr.ac.hansung.smartrent.domain.user.dto.AdminStudentResponse;
import kr.ac.hansung.smartrent.domain.user.dto.PageResponse;
import kr.ac.hansung.smartrent.domain.user.entity.Approval;
import kr.ac.hansung.smartrent.domain.user.entity.Role;
import kr.ac.hansung.smartrent.domain.user.entity.User;
import kr.ac.hansung.smartrent.domain.user.repository.UserRepository;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;
import kr.ac.hansung.smartrent.global.port.ActiveWarningCounter;
import kr.ac.hansung.smartrent.global.port.StudentHistoryReader;
import kr.ac.hansung.smartrent.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자: 학생 목록·상세(B1-14). 거르기(승인 상태, 유효 경고 있음, 현재 정지, 검색어)는 모두 AND로 전체 학생에 먼저 적용하고
 * 그 결과를 쪽으로 나눕니다(API 명세 3절). 최근 가입한 학생부터.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentService {

	private final UserRepository userRepository;
	private final ActiveWarningCounter warningCounter;
	private final StudentHistoryReader historyReader;
	private final StorageService storageService;
	private final Clock clock;

	public PageResponse<AdminStudentResponse> list(Approval approval, String q, Boolean hasWarnings, Boolean suspended,
		int page, int size) {
		LocalDateTime now = LocalDateTime.now(clock);
		String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
		List<AdminStudentResponse> all = userRepository.findAllByRole(Role.STUDENT).stream()
			.sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(User::getId, Comparator.reverseOrder()))
			.filter(u -> approval == null || u.getApproval() == approval)
			.filter(u -> needle == null || contains(u.getName(), needle) || contains(u.getStudentNumber(), needle)
				|| contains(u.getEmail(), needle))
			.filter(u -> suspended == null || u.isSuspended(now) == suspended)
			.map(u -> AdminStudentResponse.of(u, warningCounter.count(u.getId()), now))
			.filter(s -> hasWarnings == null || (s.activeWarningCount() > 0) == hasWarnings)
			.toList();
		return PageResponse.of(all, page, size);
	}

	/** 학생증 사진 주소는 승인 대기 중일 때만(승인·반려하면 사진을 지움, 기획안 1장) */
	public AdminStudentDetailResponse detail(Long id) {
		User user = userRepository.findById(id).filter(u -> u.getRole() == Role.STUDENT)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		AdminStudentResponse summary = AdminStudentResponse.of(user, warningCounter.count(id), LocalDateTime.now(clock));
		String url = user.getApproval() == Approval.PENDING && user.getStudentIdImageKey() != null
			? storageService.temporaryUrl(user.getStudentIdImageKey()) : null;
		return AdminStudentDetailResponse.of(summary, url, historyReader.warnings(id), historyReader.rentals(id));
	}

	private static boolean contains(String field, String needle) {
		return field != null && field.toLowerCase(Locale.ROOT).contains(needle);
	}
}
