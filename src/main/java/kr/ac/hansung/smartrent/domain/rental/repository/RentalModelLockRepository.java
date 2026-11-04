package kr.ac.hansung.smartrent.domain.rental.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RentalModelLockRepository extends Repository<EquipmentModel, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from EquipmentModel m where m.id = :id")
	Optional<EquipmentModel> lockById(@Param("id") Long id);
}
