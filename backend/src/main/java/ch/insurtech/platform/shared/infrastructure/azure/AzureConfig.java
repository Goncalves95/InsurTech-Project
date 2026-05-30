package ch.insurtech.platform.shared.infrastructure.azure;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("azure")
public class AzureConfig {

    @Value("${azure.document-intelligence.endpoint}")
    private String diEndpoint;

    @Value("${azure.document-intelligence.key}")
    private String diKey;

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Bean
    public DocumentAnalysisClient documentAnalysisClient() {
        return new DocumentAnalysisClientBuilder()
                .endpoint(diEndpoint)
                .credential(new AzureKeyCredential(diKey))
                .buildClient();
    }

    @Bean
    public BlobContainerClient blobContainerClient() {
        BlobContainerClient container = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient()
                .getBlobContainerClient(containerName);

        if (!container.exists()) {
            container.create();
        }
        return container;
    }
}
