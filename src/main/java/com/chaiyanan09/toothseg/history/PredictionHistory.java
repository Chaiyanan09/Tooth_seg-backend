package com.chaiyanan09.toothseg.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Table "prediction_history" (created by Flyway: db/migration/V1__init_schema.sql).
 *
 * Keep the database light:
 * - store only metadata + Cloudinary refs (+ compact missing/present lists)
 * - DO NOT store huge fields like overlay base64 / raw ML json
 */
@Entity
@Table(name = "prediction_history")
public class PredictionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK -> users.id (ON DELETE CASCADE), index (user_id, created_at DESC)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "model_name", nullable = false)
    private String modelName = "HTC_SwinT_FPN";

    // path ของไฟล์ที่ FE ส่งมา (รองรับเลือก folder)
    @Column(name = "path")
    private String path;

    @Column(name = "inference_ms", nullable = false)
    private long inferenceMs;

    // ===== Compact result (optional) =====
    // presentFdi: ["11","12",...]  -> PostgreSQL text[]
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "present_fdi", columnDefinition = "text[]")
    private List<String> presentFdi;

    // missingFdi: ["18","28",...]  -> PostgreSQL text[]
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "missing_fdi", columnDefinition = "text[]")
    private List<String> missingFdi;

    // optional: score map เพื่อเอาไปโชว์ confidence แบบเบา ๆ
    // เช่น {"11":0.98, "12":0.95, ...}  -> PostgreSQL jsonb
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "present_top_score_map", columnDefinition = "jsonb")
    private Map<String, Double> presentTopScoreMap;

    // ===== Cloudinary (authenticated/private) =====
    @Column(name = "input_public_id")
    private String inputPublicId;
    @Column(name = "input_version")
    private Long inputVersion;
    @Column(name = "input_format")
    private String inputFormat;

    @Column(name = "overlay_public_id")
    private String overlayPublicId;
    @Column(name = "overlay_version")
    private Long overlayVersion;
    @Column(name = "overlay_format")
    private String overlayFormat; // usually "png"

    // ===== Transient: ส่งให้ FE แต่ไม่เก็บลง DB =====
    @Transient
    private String inputUrl;

    @Transient
    private String overlayUrl;

    // fallback (เฉพาะตอน cloudinary ปิด) — ไม่เก็บลง DB
    @Transient
    private String overlayPngBase64;

    // parsed instances (optional) — ไม่เก็บลง DB
    @Transient
    private List<Instance> instances;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getModelName() { return modelName; }
    public String getPath() { return path; }
    public long getInferenceMs() { return inferenceMs; }

    public List<String> getPresentFdi() { return presentFdi; }
    public List<String> getMissingFdi() { return missingFdi; }
    public Map<String, Double> getPresentTopScoreMap() { return presentTopScoreMap; }

    public String getInputPublicId() { return inputPublicId; }
    public Long getInputVersion() { return inputVersion; }
    public String getInputFormat() { return inputFormat; }

    public String getOverlayPublicId() { return overlayPublicId; }
    public Long getOverlayVersion() { return overlayVersion; }
    public String getOverlayFormat() { return overlayFormat; }

    public String getInputUrl() { return inputUrl; }
    public String getOverlayUrl() { return overlayUrl; }

    public String getOverlayPngBase64() { return overlayPngBase64; }
    public List<Instance> getInstances() { return instances; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public void setPath(String path) { this.path = path; }
    public void setInferenceMs(long inferenceMs) { this.inferenceMs = inferenceMs; }

    public void setPresentFdi(List<String> presentFdi) { this.presentFdi = presentFdi; }
    public void setMissingFdi(List<String> missingFdi) { this.missingFdi = missingFdi; }
    public void setPresentTopScoreMap(Map<String, Double> presentTopScoreMap) { this.presentTopScoreMap = presentTopScoreMap; }

    public void setInputPublicId(String inputPublicId) { this.inputPublicId = inputPublicId; }
    public void setInputVersion(Long inputVersion) { this.inputVersion = inputVersion; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

    public void setOverlayPublicId(String overlayPublicId) { this.overlayPublicId = overlayPublicId; }
    public void setOverlayVersion(Long overlayVersion) { this.overlayVersion = overlayVersion; }
    public void setOverlayFormat(String overlayFormat) { this.overlayFormat = overlayFormat; }

    public void setInputUrl(String inputUrl) { this.inputUrl = inputUrl; }
    public void setOverlayUrl(String overlayUrl) { this.overlayUrl = overlayUrl; }

    public void setOverlayPngBase64(String overlayPngBase64) { this.overlayPngBase64 = overlayPngBase64; }
    public void setInstances(List<Instance> instances) { this.instances = instances; }

    public static class Instance {
        private String fdi;
        private double score;
        private double[] bboxXyxy;

        public String getFdi() { return fdi; }
        public void setFdi(String fdi) { this.fdi = fdi; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public double[] getBboxXyxy() { return bboxXyxy; }
        public void setBboxXyxy(double[] bboxXyxy) { this.bboxXyxy = bboxXyxy; }
    }
}
