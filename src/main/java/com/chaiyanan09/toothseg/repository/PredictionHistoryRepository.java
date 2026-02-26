package com.chaiyanan09.toothseg.repository;

import com.chaiyanan09.toothseg.history.PredictionHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionHistoryRepository extends MongoRepository<PredictionHistory, String> {
    List<PredictionHistory> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PredictionHistory> findByIdAndUserId(String id, String userId);
}