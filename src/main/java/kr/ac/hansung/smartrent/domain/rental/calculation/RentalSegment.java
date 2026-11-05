package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.LocalDateTime;
import kr.ac.hansung.smartrent.global.exception.BusinessException;
import kr.ac.hansung.smartrent.global.exception.ErrorCode;

public record RentalSegment(LocalDateTime start, LocalDateTime end) {
	public RentalSegment {
		if (start == null || end == null || !start.isBefore(end)) {
			throw new BusinessException(ErrorCode.SEGMENT_INVALID);
		}
	}
}
