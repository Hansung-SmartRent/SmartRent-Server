package kr.ac.hansung.smartrent.domain.rental.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
public record AdminRentalResponse(@JsonUnwrapped RentalResponse rental,Student student) {
 public record Student(Long id,String name,String studentNumber) { }
}
