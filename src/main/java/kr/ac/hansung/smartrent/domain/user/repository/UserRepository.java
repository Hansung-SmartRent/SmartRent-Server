package kr.ac.hansung.smartrent.domain.user.repository;

import kr.ac.hansung.smartrent.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
