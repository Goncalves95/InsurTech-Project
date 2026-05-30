package ch.insurtech.platform.claim.domain.port;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStoragePort {

    /**
     * Stores the document and returns a unique storage key for later retrieval.
     */
    String store(MultipartFile file, String claimId);

    void delete(String storageKey);
}
