package online.ericanders.camera.component;

import jakarta.annotation.PostConstruct;
import online.ericanders.camera.service.CameraService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class StreamMultiplexer {
    public static final int BUFFER_SIZE = 1024;
    private static final String RECORDINGS_DIRECTORY = "recordings";

    private final CameraService cameraService;
    private final List<OutputStream> clients;
    private OutputStream currentFileStream;
    private File currentFile;

    public StreamMultiplexer(CameraService cameraService) {
        this.cameraService = cameraService;
        this.clients = new CopyOnWriteArrayList<>();
    }

    @PostConstruct
    public void start() throws IOException {
        rotateFile();

        InputStream cameraInput = cameraService.getCameraStream();
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            try {
                while ((length = cameraInput.read(buffer)) != -1) {
                    // Write to disk
                    synchronized (this) {
                        currentFileStream.write(buffer, 0, length);
                    }

                    // Stream to clients
                    for (OutputStream client : clients) {
                        try {
                            client.write(buffer, 0, length);
                            client.flush();
                        } catch (IOException e) {
                            clients.remove(client);
                        }
                    }

                }
            } catch (IOException ignored) {
            }
        }, "CameraPump").start();
    }

    public synchronized File rotateFile() throws IOException {
        if (currentFileStream != null) {
            currentFileStream.close();
        }
        File previousFile = currentFile;

        currentFile = createNewOutputFile();
        currentFileStream = new FileOutputStream(currentFile);

        return previousFile;
    }

    public void addClient(OutputStream stream) {
        clients.add(stream);
    }

    public void removeClient(OutputStream stream) {
        clients.remove(stream);
    }

    private File createNewOutputFile() {
        String fileName = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH")
                .format(java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()));
        return new File(RECORDINGS_DIRECTORY,fileName + ".h264");
    }
}
