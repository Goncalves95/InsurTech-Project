package ch.insurtech.platform.claim.infrastructure.storage;

import ch.insurtech.platform.claim.domain.port.DocumentStoragePort;
import ch.insurtech.platform.shared.domain.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local filesystem storage adapter — active on dev/test profiles only.
 * Replace with AzureBlobStorageAdapter or S3StorageAdapter in production.
 */
@Component
@Profile({"default", "dev", "test"})
public class LocalDocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorageAdapter.class);
    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("java.io.tmpdir"), "insurtech-documents");

    @Override
    public String store(MultipartFile file, String claimId) {
        try {
            Files.createDirectories(STORAGE_ROOT);
            String fileName = claimId + "_" + file.getOriginalFilename();
            Path destination = STORAGE_ROOT.resolve(fileName);
            file.transferTo(destination);
            log.debug("Document stored locally at {}", destination);
            return fileName;
        } catch (IOException e) {
            throw new ExternalServiceException("LocalDocumentStorage", "Failed to store document for claim " + claimId, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(STORAGE_ROOT.resolve(storageKey));
        } catch (IOException e) {
            log.warn("Failed to delete document {}: {}", storageKey, e.getMessage());
        }
    }
}
