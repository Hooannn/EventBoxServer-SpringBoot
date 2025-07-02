package com.ht.eventbox.modules.storage;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public Map uploadByBase64(String base64, String folder) throws IOException {
        return cloudinary.uploader().upload(base64, Map.of(
                "folder", folder
        ));
    }

    public Map destroyByPublicId(String publicId, String resourceType) throws IOException {
        return cloudinary.uploader().destroy(publicId, Map.of("resource_type", resourceType));
    }
}
