package ch.insurtech.platform.claim.infrastructure.storage;

import ch.insurtech.platform.claim.domain.port.DocumentStoragePort;
import ch.insurtech.platform.shared.domain.exception.ExternalServiceException;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@Profile("azure")
public class AzureBlobStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageAdapter.class);

    private final BlobContainerClient containerClient;

    public AzureBlobStorageAdapter(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    @Override
    public String store(MultipartFile file, String claimId) {
        String blobName = claimId + "/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        try {
            containerClient.getBlobClient(blobName)
                    .upload(BinaryData.fromBytes(file.getBytes()), true);
            log.info("Document uploaded to Azure Blob Storage: {}", blobName);
            return blobName;
        } catch (IOException e) {
            throw new ExternalServiceException("AzureBlobStorage",
                    "Failed to read document bytes for claim " + claimId, e);
        } catch (BlobStorageException e) {
            throw new ExternalServiceException("AzureBlobStorage",
                    "Upload failed for claim " + claimId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            containerClient.getBlobClient(storageKey).deleteIfExists();
            log.debug("Deleted blob: {}", storageKey);
        } catch (BlobStorageException e) {
            log.warn("Could not delete blob {}: {}", storageKey, e.getMessage());
        }
    }

    private String sanitize(String filename) {
        return filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
    }
}
