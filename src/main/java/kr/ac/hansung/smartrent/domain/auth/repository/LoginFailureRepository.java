package kr.ac.hansung.smartrent.domain.auth.repository;

import kr.ac.hansung.smartrent.domain.auth.entity.LoginFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, String> {
}
