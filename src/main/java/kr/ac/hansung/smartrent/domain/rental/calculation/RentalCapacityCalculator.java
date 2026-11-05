package kr.ac.hansung.smartrent.domain.rental.calculation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentUnit;
import kr.ac.hansung.smartrent.domain.equipment.entity.UnitCondition;
import kr.ac.hansung.smartrent.domain.rental.entity.Rental;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;

public final class RentalCapacityCalculator {
	private final Clock clock;

	public RentalCapacityCalculator(Clock clock) {
		this.clock = clock.withZone(ZoneId.of("Asia/Seoul"));
	}

	public int available(boolean carryOut, List<EquipmentUnit> units, List<Rental> rentals, RentalSegment ask) {
		var normalIds = units.stream().filter(u -> u.getCondition() == UnitCondition.NORMAL && u.getDeletedAt() == null)
			.map(EquipmentUnit::getId).collect(java.util.stream.Collectors.toSet());
		var events = new TreeMap<LocalDateTime, Integer>();
		var now = LocalDateTime.now(clock);
		for (Rental rental : rentals) {
			if (rental.getStatus() != RentalStatus.RESERVED && rental.getStatus() != RentalStatus.RENTING) continue;
			if (rental.getUnitId() != null && !normalIds.contains(rental.getUnitId())) continue;
			LocalDateTime start = rental.getStatus() == RentalStatus.RENTING ? rental.getPickedAt() : rental.getStartAt();
			boolean overdue = rental.getStatus() == RentalStatus.RENTING && rental.getReturnedAt() == null
				&& rental.getEndAt().isBefore(now);
			LocalDateTime end = overdue ? ask.end() : rental.getEndAt();
			if (carryOut) {
				start = start.minusHours(1);
				if (!overdue) end = end.plusHours(1);
			}
			start = start.isBefore(ask.start()) ? ask.start() : start;
			end = end.isAfter(ask.end()) ? ask.end() : end;
			if (!start.isBefore(end)) continue;
			events.merge(start, 1, Integer::sum);
			events.merge(end, -1, Integer::sum);
		}
		int current = 0, peak = 0;
		for (int delta : events.values()) {
			current += delta;
			peak = Math.max(peak, current);
		}
		return Math.max(0, normalIds.size() - peak);
	}
}
