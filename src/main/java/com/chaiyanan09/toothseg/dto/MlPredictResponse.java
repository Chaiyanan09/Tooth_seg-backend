package com.chaiyanan09.toothseg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MlPredictResponse {

    @JsonProperty("overlay_png_base64")
    private String overlayPngBase64;

    @JsonProperty("instances")
    private List<InstanceItem> instances;

    @JsonProperty("inference_ms")
    private long inferenceMs;

    public String getOverlayPngBase64() {
        return overlayPngBase64;
    }

    public void setOverlayPngBase64(String overlayPngBase64) {
        this.overlayPngBase64 = overlayPngBase64;
    }

    public List<InstanceItem> getInstances() {
        return instances;
    }

    public void setInstances(List<InstanceItem> instances) {
        this.instances = instances;
    }

    public long getInferenceMs() {
        return inferenceMs;
    }

    public void setInferenceMs(long inferenceMs) {
        this.inferenceMs = inferenceMs;
    }

    public static class InstanceItem {
        private String fdi;
        private double score;

        @JsonProperty("bbox_xyxy")
        private double[] bboxXyxy;

        public String getFdi() {
            return fdi;
        }

        public void setFdi(String fdi) {
            this.fdi = fdi;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double[] getBboxXyxy() {
            return bboxXyxy;
        }

        public void setBboxXyxy(double[] bboxXyxy) {
            this.bboxXyxy = bboxXyxy;
        }
    }
}