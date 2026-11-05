package kr.ac.hansung.smartrent.domain.rental.service;

import java.time.Clock;
import java.util.List;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.equipment.repository.EquipmentUnitRepository;
import kr.ac.hansung.smartrent.domain.rental.calculation.*;
import kr.ac.hansung.smartrent.domain.rental.entity.RentalStatus;
import kr.ac.hansung.smartrent.domain.rental.repository.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RentalCapacityService {
	private final EquipmentUnitRepository units;
	private final RentalRepository rentals;
	private final RentalCapacityCalculator calculator;

	public RentalCapacityService(EquipmentUnitRepository units, RentalRepository rentals, Clock clock) {
		this.units = units;
		this.rentals = rentals;
		this.calculator = new RentalCapacityCalculator(clock);
	}

	/** 쓰기 호출자는 같은 트랜잭션에서 모델을 먼저 잠근다(DB 설계 6절). */
	public int available(EquipmentModel model, RentalSegment segment) {
		return calculator.available(model.isCarryOut(), units.findAllByModelIdAndDeletedAtIsNullOrderByIdAsc(model.getId()),
			rentals.findAllByModelIdAndStatusIn(model.getId(), List.of(RentalStatus.RESERVED, RentalStatus.RENTING)), segment);
	}
}
