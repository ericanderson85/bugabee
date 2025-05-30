package online.ericanders.camera.service;

import online.ericanders.camera.component.StreamMultiplexer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class CameraStreamManager implements InitializingBean, DisposableBean {
    private static final int BUFFER_SIZE = 8 * 1024;
    private final CameraService cameraService;
    private final StreamMultiplexer streamMultiplexer;
    private final HLSService hlsService;
    private volatile boolean running = false;

    public CameraStreamManager(CameraService cameraService, StreamMultiplexer streamMultiplexer, HLSService hlsService) {
        this.cameraService = cameraService;
        this.streamMultiplexer = streamMultiplexer;
        this.hlsService = hlsService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        startCameraStream();
    }

    @Override
    public void destroy() throws Exception {
        running = false;
    }

    private void startCameraStream() throws IOException {
        InputStream cameraStream = cameraService.getCameraStream();
        running = true;

        Thread streamDistributor = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            try {
                while (running && (length = cameraStream.read(buffer)) != -1) {
                    streamMultiplexer.writeData(buffer, 0, length);
                    hlsService.writeData(buffer, 0, length);
                }
            } catch (IOException ignored) {
            }
        }, "CameraStreamDistributor");

        streamDistributor.start();
    }
}
