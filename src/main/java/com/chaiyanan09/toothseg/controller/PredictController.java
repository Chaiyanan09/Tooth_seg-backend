package com.chaiyanan09.toothseg.controller;

import com.chaiyanan09.toothseg.client.MlClient;
import com.chaiyanan09.toothseg.history.PredictionHistory;
import com.chaiyanan09.toothseg.repository.PredictionHistoryRepository;
import com.chaiyanan09.toothseg.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class PredictController {

    // canonical 32 FDI teeth (string)
    private static final List<String> FDI32 = List.of(
            "11","12","13","14","15","16","17","18",
            "21","22","23","24","25","26","27","28",
            "31","32","33","34","35","36","37","38",
            "41","42","43","44","45","46","47","48"
    );

    private final MlClient mlClient;
    private final PredictionHistoryRepository historyRepo;
    private final CloudinaryService cloudinary;

    public PredictController(MlClient mlClient,
                             PredictionHistoryRepository historyRepo,
                             CloudinaryService cloudinary) {
        this.mlClient = mlClient;
        this.historyRepo = historyRepo;
        this.cloudinary = cloudinary;
    }

    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PredictionHistory predict(
            Authentication auth,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path
    ) throws Exception {

        System.out.println("[BE] upload name=" + (file == null ? "null" : file.getOriginalFilename())
                + " size=" + (file == null ? -1 : file.getSize())
                + " empty=" + (file == null || file.isEmpty()));

        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty or missing");
        }

        String userId = extractUserId(auth);

        String safePath = sanitizePath(path);
        if (safePath == null || safePath.isBlank()) {
            safePath = sanitizePath(file.getOriginalFilename());
        }
        if (safePath == null || safePath.isBlank()) {
            safePath = "upload.png";
        }

        String inputExt = fileExtLower(safePath);
        if (inputExt == null) inputExt = "png";

        // 1) เรียก ML
        String rawJson = mlClient.predictRawJson(file);

        String overlayB64 = normalizeBase64(extractJsonString(rawJson, "overlay_png_base64"));
        Long inferenceMs = extractJsonLong(rawJson, "inference_ms");
        List<PredictionHistory.Instance> instances = extractInstances(rawJson);

        if (inferenceMs == null) inferenceMs = 0L;

        // 2) compact present/missing (DB เบา)
        Map<String, Double> topScore = computeTopScoreMap(instances);
        List<String> presentFdi = new ArrayList<>(topScore.keySet());
        Collections.sort(presentFdi);
        List<String> missingFdi = computeMissingFdi(presentFdi);

        // 3) เตรียม history record
        PredictionHistory h = new PredictionHistory();
        h.setUserId(userId);
        h.setCreatedAt(Instant.now());
        h.setPath(safePath);
        h.setInferenceMs(inferenceMs);
        h.setPresentFdi(presentFdi);
        h.setMissingFdi(missingFdi);
        h.setPresentTopScoreMap(topScore);

        // instances ส่งให้ FE ได้ แต่ไม่เก็บลง DB (@Transient)
        h.setInstances(instances);

        // 4) ถ้ามี Cloudinary -> upload input + overlay เป็น authenticated
        if (cloudinary.isEnabled()) {
            // input publicId: <userId>/input/<path without ext>
            String baseId = removeExt(safePath);
            // ทำให้ไม่ชนกัน ถ้าทายไฟล์เดิมซ้ำ
            String runId = Long.toString(Instant.now().toEpochMilli(), 36);
            try {
                String inputPublicId = safePublicId(userId, "input", baseId + "__" + runId);
                Map<?, ?> inRes = cloudinary.uploadAuthenticated(file.getBytes(), inputPublicId);
                if (inRes != null) {
                    String pid = asString(inRes.get("public_id"), inputPublicId);
                    Long ver = asLong(inRes.get("version"));
                    String fmt = asString(inRes.get("format"), inputExt);

                    h.setInputPublicId(pid);
                    h.setInputVersion(ver);
                    h.setInputFormat(fmt);
                    h.setInputUrl(cloudinary.signedAuthenticatedUrl(pid, ver, fmt));
                }
            } catch (Exception e) {
                System.out.println("[Cloudinary] input upload failed: " + e.getMessage());
            }

            // overlay publicId: <userId>/predict/<path without ext>  (format png)
            if (overlayB64 != null && !overlayB64.isBlank()) {
                try {
                    byte[] overlayBytes = Base64.getDecoder().decode(overlayB64);

                    String overlayPublicId = safePublicId(userId, "predict", baseId + "__" + runId);
                    Map<?, ?> outRes = cloudinary.uploadAuthenticated(overlayBytes, overlayPublicId);
                    if (outRes != null) {
                        String pid = asString(outRes.get("public_id"), overlayPublicId);
                        Long ver = asLong(outRes.get("version"));
                        String fmt = asString(outRes.get("format"), "png");

                        h.setOverlayPublicId(pid);
                        h.setOverlayVersion(ver);
                        h.setOverlayFormat(fmt);
                        h.setOverlayUrl(cloudinary.signedAuthenticatedUrl(pid, ver, fmt));
                    }

                    // ไม่ต้องเก็บ base64 ลง DB (และใน object ก็ไม่จำเป็นแล้ว) ถ้า upload สำเร็จ
                    h.setOverlayPngBase64(null);
                } catch (Exception e) {
                    // สำคัญ: อย่าให้ cloudinary fail แล้วทั้ง predict fail
                    System.out.println("[Cloudinary] overlay upload failed: " + e.getMessage());
                    h.setOverlayPngBase64(overlayB64); // ส่งให้ FE ใช้แสดงผลชั่วคราว
                }
            }

        } else {
            // fallback: ส่ง base64 ให้ FE ใช้แสดง (แต่ field นี้ @Transient ไม่เก็บลง DB)
            h.setOverlayPngBase64(overlayB64);
        }

        // 5) save DB (เบา ๆ: metadata + cloud refs + present/missing)
        PredictionHistory saved = historyRepo.save(h);

        // 6) เติม url ใน response อีกที (กันกรณี save แล้ว id เปลี่ยน)
        if (cloudinary.isEnabled()) {
            fillSignedUrls(saved);
        }

        return saved;
    }

    @GetMapping("/history")
    public List<PredictionHistory> history(Authentication auth) {
        String userId = extractUserId(auth);
        List<PredictionHistory> list = historyRepo.findByUserIdOrderByCreatedAtDesc(userId);

        if (cloudinary.isEnabled()) {
            for (PredictionHistory h : list) {
                fillSignedUrls(h);
                // history list เบา ๆ อยู่แล้ว (เราไม่เก็บ raw json)
            }
        }
        return list;
    }

    @GetMapping("/history/{id}")
    public PredictionHistory historyOne(Authentication auth, @PathVariable String id) {
        String userId = extractUserId(auth);
        PredictionHistory h = historyRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History not found"));

        if (cloudinary.isEnabled()) {
            fillSignedUrls(h);
        }
        return h;
    }

    // ===== helpers =====

    private void fillSignedUrls(PredictionHistory h) {
        if (h.getInputPublicId() != null && h.getInputVersion() != null && h.getInputFormat() != null) {
            h.setInputUrl(cloudinary.signedAuthenticatedUrl(h.getInputPublicId(), h.getInputVersion(), h.getInputFormat()));
        }
        if (h.getOverlayPublicId() != null && h.getOverlayVersion() != null && h.getOverlayFormat() != null) {
            h.setOverlayUrl(cloudinary.signedAuthenticatedUrl(h.getOverlayPublicId(), h.getOverlayVersion(), h.getOverlayFormat()));
        }
    }

    private String extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof String s) return s;
        return auth.getName();
    }

    private static String sanitizePath(String p) {
        if (p == null) return null;
        String s = p.replace('\\', '/').trim();
        while (s.startsWith("/")) s = s.substring(1);
        // กัน path traversal
        while (s.contains("..")) s = s.replace("..", "");
        // กัน empty
        if (s.isBlank()) return null;
        return s;
    }

    private static String removeExt(String p) {
        int i = p.lastIndexOf('.');
        if (i < 0) return p;
        return p.substring(0, i);
    }

    private static String fileExtLower(String p) {
        int i = p.lastIndexOf('.');
        if (i < 0 || i == p.length() - 1) return null;
        return p.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    // สร้าง publicId ที่แยกตาม userId + type + path
    private static String safePublicId(String userId, String type, String pathNoExt) {
        String s = (pathNoExt == null ? "file" : pathNoExt).replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        while (s.contains("..")) s = s.replace("..", "");
        // ตัวอย่าง: <userId>/predict/caseA/img/7
        return userId + "/" + type + "/" + s;
    }

    private static String asString(Object o, String def) {
        if (o == null) return def;
        return String.valueOf(o);
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    // -------- tiny JSON extractors (no Jackson dependency) --------
    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private static Long extractJsonLong(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;

        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        int start = p;

        // รองรับเลขแบบ 1234 หรือ 1234.0
        while (p < json.length() && (Character.isDigit(json.charAt(p)) || json.charAt(p) == '.')) p++;
        if (start == p) return null;

        String num = json.substring(start, p);
        try {
            if (num.contains(".")) return (long) Double.parseDouble(num);
            return Long.parseLong(num);
        } catch (Exception e) {
            return null;
        }
    }

    // parse instances array แบบง่าย ๆ (ไม่พึ่ง lib)
    private static List<PredictionHistory.Instance> extractInstances(String json) {
        try {
            int k = json.indexOf("\"instances\"");
            if (k < 0) return List.of();
            int lb = json.indexOf('[', k);
            if (lb < 0) return List.of();
            int rb = findMatchingBracket(json, lb);
            if (rb < 0) return List.of();

            String arr = json.substring(lb + 1, rb).trim();
            if (arr.isEmpty()) return List.of();

            List<String> objects = splitTopLevelObjects(arr);
            List<PredictionHistory.Instance> out = new ArrayList<>();

            for (String obj : objects) {
                String fdi = extractJsonString(obj, "fdi");
                Double score = extractJsonDouble(obj, "score");
                double[] bbox = extractJsonDoubleArray(obj, "bbox_xyxy");

                if (fdi == null) continue;

                PredictionHistory.Instance inst = new PredictionHistory.Instance();
                inst.setFdi(fdi);
                inst.setScore(score == null ? 0.0 : score);
                inst.setBboxXyxy(bbox);
                out.add(inst);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static int findMatchingBracket(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static List<String> splitTopLevelObjects(String s) {
        List<String> res = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    res.add(s.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return res;
    }

    private static Double extractJsonDouble(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;

        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        int start = p;

        while (p < json.length() && (Character.isDigit(json.charAt(p)) || json.charAt(p) == '.' || json.charAt(p) == '-')) p++;
        if (start == p) return null;

        try {
            return Double.parseDouble(json.substring(start, p));
        } catch (Exception e) {
            return null;
        }
    }

    private static double[] extractJsonDoubleArray(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;

        int lb = json.indexOf('[', colon);
        if (lb < 0) return null;
        int rb = findMatchingBracket(json, lb);
        if (rb < 0) return null;

        String inside = json.substring(lb + 1, rb).trim();
        if (inside.isEmpty()) return new double[0];

        String[] parts = inside.split(",");
        double[] arr = new double[parts.length];
        for (int idx = 0; idx < parts.length; idx++) {
            try { arr[idx] = Double.parseDouble(parts[idx].trim()); }
            catch (Exception e) { arr[idx] = 0.0; }
        }
        return arr;
    }

    // ===== compact present/missing helpers =====

    private static Map<String, Double> computeTopScoreMap(List<PredictionHistory.Instance> instances) {
        Map<String, Double> map = new HashMap<>();
        if (instances == null) return map;
        for (PredictionHistory.Instance inst : instances) {
            if (inst == null || inst.getFdi() == null) continue;
            String fdi = inst.getFdi().trim();
            double score = inst.getScore();
            Double prev = map.get(fdi);
            if (prev == null || score > prev) map.put(fdi, score);
        }
        return map;
    }

    private static List<String> computeMissingFdi(List<String> presentFdi) {
        Set<String> present = new HashSet<>(presentFdi == null ? List.of() : presentFdi);
        List<String> missing = new ArrayList<>();
        for (String fdi : FDI32) {
            if (!present.contains(fdi)) missing.add(fdi);
        }
        return missing;
    }

    /**
     * Some ML servers may return base64 with prefix like "data:image/png;base64,....".
     */
    private static String normalizeBase64(String b64) {
        if (b64 == null) return null;
        String s = b64.trim();
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma >= 0) {
            s = s.substring(comma + 1);
        }
        return s;
    }
}