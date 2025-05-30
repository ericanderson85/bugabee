package online.ericanders.camera.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class AzureBlobService {

    private final BlobContainerClient containerClient;

    public AzureBlobService(
            @Value("${spring.cloud.azure.storage.connection-string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.container-name}") String containerName
    ) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(containerName);
    }

    public String uploadFile(File file) throws IOException {
        try (FileInputStream fileStream = new FileInputStream(file)) {
            BlobClient blob = containerClient.getBlobClient(file.getName());
            blob.upload(fileStream, file.length());
            boolean ignored = file.delete();
            return blob.getBlobUrl();
        }
    }
}
