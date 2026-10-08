package kr.ac.hansung.smartrent.global.storage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageDeletionJobRepository extends JpaRepository<StorageDeletionJob, Long> {

	List<StorageDeletionJob> findAllByStorageModeOrderByIdAsc(StorageMode storageMode);
}
