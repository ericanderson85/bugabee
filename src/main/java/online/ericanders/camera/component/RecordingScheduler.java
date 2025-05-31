package online.ericanders.camera.component;

import online.ericanders.camera.service.AzureBlobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

@Component
public class RecordingScheduler {
    private final StreamMultiplexer multiplexer;
    private final AzureBlobService blobService;
    private final ThreadPoolExecutor uploadExecutor =
            new ThreadPoolExecutor(
                    1,
                    2,
                    60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(4),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );


    public RecordingScheduler(StreamMultiplexer multiplexer, AzureBlobService blobService) {
        this.multiplexer = multiplexer;
        this.blobService = blobService;
    }

    // At the start of each hour
    @Scheduled(cron = "0 0 * * * *")
    public void uploadRecording() {
        try {
            File finished = multiplexer.rotateFile();
            if (finished != null && finished.exists()) {
                uploadExecutor.submit(() -> uploadAndCleanup(finished));
            }
        } catch (Exception e) {
            System.err.println("Error while uploading recording: " + e.getMessage());
        }
    }

    private void uploadAndCleanup(File file) {
        try {
            String ignored = blobService.uploadFile(file);
        } catch (IOException e) {
            System.err.println("Error while uploading file: " + e.getMessage());
        }
    }
}
