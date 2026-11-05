package kr.ac.hansung.smartrent.domain.rental.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import kr.ac.hansung.smartrent.domain.equipment.entity.EquipmentModel;
import kr.ac.hansung.smartrent.domain.operation.entity.OperatingHours;
import kr.ac.hansung.smartrent.domain.rental.calculation.RentalCalendar;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RentalCalendarReader {
	private final EntityManager em;
	public RentalCalendarReader(EntityManager em) { this.em = em; }

	public RentalCalendar read(EquipmentModel model) {
		var changes = em.createQuery("select h from OperatingHours h where h.modelId = :id", OperatingHours.class)
			.setParameter("id", model.getId()).getResultList();
		var holidays = em.createQuery("select h from Holiday h", kr.ac.hansung.smartrent.domain.operation.entity.Holiday.class)
			.getResultList();
		return new RentalCalendar() {
			public String closedReason(LocalDate date) {
				if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) return "주말";
				return holidays.stream().filter(h -> h.getDate().equals(date) && !h.isOpenOverride())
					.map(h -> h.getName()).findFirst().orElse(null);
			}
			public boolean isClosed(LocalDate date) {
				return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY
					|| holidays.stream().anyMatch(h -> h.getDate().equals(date) && !h.isOpenOverride());
			}
			public Hours hours(LocalDate date) {
				var specific = changes.stream().filter(h -> date.equals(h.getDate())).findFirst();
				var selected = specific.or(() -> changes.stream().filter(h -> h.getDate() == null).findFirst());
				return selected.map(h -> new Hours(h.getOpenTime(), h.getCloseTime()))
					.orElseGet(() -> new Hours(model.getOpenTime(), model.getCloseTime()));
			}
		};
	}
}
