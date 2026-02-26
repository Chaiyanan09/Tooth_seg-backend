package com.chaiyanan09.toothseg.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary; // null when disabled
    private final String folder;
    private final boolean enabled;

    public CloudinaryService(
            @Value("${app.cloudinary.url:}") String cloudinaryUrl,
            @Value("${app.cloudinary.folder:toothseg}") String folder
    ) {
        this.folder = folder;

        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            this.cloudinary = null;
            this.enabled = false;
            System.out.println("[Cloudinary] disabled (app.cloudinary.url is empty)");
            return;
        }

        if (!cloudinaryUrl.startsWith("cloudinary://")) {
            // ไม่ให้ BE ล้มตอนรัน — แต่จะปิด cloudinary ไปก่อน
            this.cloudinary = null;
            this.enabled = false;
            System.out.println("[Cloudinary] disabled: Invalid CLOUDINARY_URL scheme. Expect 'cloudinary://...'");
            return;
        }

        this.cloudinary = new Cloudinary(cloudinaryUrl);
        this.enabled = true;
        System.out.println("[Cloudinary] enabled, folder=" + this.folder);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<?, ?> uploadAuthenticated(byte[] bytes, String publicId) throws Exception {
        if (!enabled) return null;

        return cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "type", "authenticated",
                "resource_type", "image",
                "folder", folder,
                "public_id", publicId,
                "overwrite", false,
                "unique_filename", false
        ));
    }

    public String signedAuthenticatedUrl(String publicId, Object version, String format) {
        if (!enabled) return null;

        return cloudinary.url()
                .secure(true)
                .type("authenticated")
                .resourceType("image")
                .format(format)
                .version(version)
                .signed(true)
                .generate(publicId);
    }
}