package online.ericanders.camera.component;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

@Component
public class StreamMultiplexer {
    private static final String RECORDINGS_DIRECTORY = "recordings";
    private static final int QUEUE_CAPACITY = 200;

    private record Frame(byte[] buffer, int length) {
    }

    private final BlockingQueue<Frame> fileQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private final Object rotationLock = new Object();
    private File currentFile;
    private BufferedOutputStream fileOut;

    private final List<OutputStream> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService clientWriteExecutor =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "ClientWriter");
                t.setDaemon(true);
                return t;
            });

    public void writeData(byte[] buffer, int offset, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(buffer, offset, copy, 0, length);
        if (!fileQueue.offer(new Frame(copy, length))) {
            System.err.println("Error writing data - Queue full");
            return;
        }

        if (!clients.isEmpty()) {
            clientWriteExecutor.submit(() -> {
                Iterator<OutputStream> clientsIterator = clients.iterator();
                while (clientsIterator.hasNext()) {
                    OutputStream os = clientsIterator.next();
                    try {
                        os.write(buffer, offset, length);
                        os.flush();
                    } catch (IOException e) {
                        clientsIterator.remove();
                        try {
                            os.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            });
        }
    }

    private void runFileWriter() {
        try {
            while (true) {
                Frame f = fileQueue.take();
                synchronized (rotationLock) {
                    fileOut.write(f.buffer, 0, f.length);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (IOException ioe) {
            System.err.println("FileWriter I/O error: " + ioe);
        }
    }

    public synchronized File rotateFile() throws IOException {
        synchronized (rotationLock) {
            if (fileOut != null) {
                fileOut.close();
            }
            File previous = currentFile;

            String fileName = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd-HH")
                    .format(java.time.Instant.now());
            currentFile = Paths.get(RECORDINGS_DIRECTORY, fileName + ".h264").toFile();
            fileOut = new BufferedOutputStream(
                    new FileOutputStream(currentFile, true), 64 * 1024);

            return previous;
        }
    }
}
