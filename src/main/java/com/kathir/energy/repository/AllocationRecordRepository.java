package com.kathir.energy.repository;
import com.kathir.energy.model.AllocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AllocationRecordRepository extends JpaRepository<AllocationRecord, Long> {
    List<AllocationRecord> findAllByOrderByIntervalNumberAscIdAsc();
}
