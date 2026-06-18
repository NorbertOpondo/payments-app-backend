package com.app.payments.repositories;

import com.app.payments.model.SmsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SmsRepository extends JpaRepository<SmsRecord, String> {

    @Query("SELECT s FROM SmsRecord s WHERE s.status = 400 AND s.retryCount < 5 AND s.createdAt >= :since")
    List<SmsRecord> findEligibleForRetry(@Param("since") LocalDateTime since);
}
