package com.chaiyanan09.toothseg.repository;

import com.chaiyanan09.toothseg.history.PredictionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionHistoryRepository extends JpaRepository<PredictionHistory, UUID> {
    List<PredictionHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<PredictionHistory> findByIdAndUserId(UUID id, UUID userId);
}
