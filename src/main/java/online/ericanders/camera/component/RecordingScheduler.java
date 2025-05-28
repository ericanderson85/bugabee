package online.ericanders.camera.component;

import online.ericanders.camera.service.AzureBlobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class RecordingScheduler {
    private final StreamMultiplexer multiplexer;
    private final AzureBlobService blobService;

    public RecordingScheduler(StreamMultiplexer multiplexer, AzureBlobService blobService) {
        this.multiplexer = multiplexer;
        this.blobService = blobService;
    }

    // At the start of each hour
    @Scheduled(cron = "0 0 * * * *")
    public void uploadRecording() {
        try {
            File finished = multiplexer.rotateFile();
            if (finished.exists()) {
                String url = blobService.uploadFile(finished);
                System.out.println("Uploaded file " + finished.getName() + ": " + url);
            }
        } catch (Exception e) {
            System.err.println("Error while uploading recording: " + e.getMessage());
        }
    }
}
