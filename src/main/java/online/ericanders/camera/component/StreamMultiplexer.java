package online.ericanders.camera.component;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StreamMultiplexer {
    private static final String RECORDINGS_DIRECTORY = "recordings";

    private final List<OutputStream> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService clientWriteExecutor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "ClientWriter");
                t.setDaemon(true);
                return t;
            }
    );

    private volatile FileChannel currentFileChannel;
    private volatile File currentFile;
    private final Object fileRotationLock = new Object();

    public void writeData(byte[] buffer, int offset, int length) {
        CompletableFuture.runAsync(() -> writeToFile(buffer, offset, length));

        if (!clients.isEmpty()) {
            clientWriteExecutor.submit(() -> writeToClients(buffer, offset, length));
        }
    }

    private void writeToFile(byte[] buffer, int offset, int length) {
        synchronized (fileRotationLock) {
            try {
                if (currentFileChannel != null && currentFileChannel.isOpen()) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
                    int ignored = currentFileChannel.write(byteBuffer);
                }
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
    }

    private void writeToClients(byte[] buffer, int offset, int length) {
        if (clients.isEmpty()) {
            return;
        }

        Iterator<OutputStream> iterator = clients.iterator();
        while (iterator.hasNext()) {
            OutputStream client = iterator.next();
            try {
                client.write(buffer, offset, length);
                client.flush();
            } catch (IOException e) {
                iterator.remove();
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public synchronized File rotateFile() throws IOException {
        synchronized (fileRotationLock) {
            if (currentFileChannel != null) {
                currentFileChannel.close();
            }
            File previousFile = currentFile;
            currentFile = createNewOutputFile();
            currentFileChannel = FileChannel.open(
                    previousFile.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );

            return previousFile;
        }
    }

    private File createNewOutputFile() {
        String fileName = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH")
                .format(java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()));
        return new File(RECORDINGS_DIRECTORY, fileName + ".h264");
    }
}
