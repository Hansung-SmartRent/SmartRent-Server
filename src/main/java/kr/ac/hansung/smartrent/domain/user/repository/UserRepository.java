package kr.ac.hansung.smartrent.domain.user.repository;

import java.util.Optional;

import kr.ac.hansung.smartrent.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
}
