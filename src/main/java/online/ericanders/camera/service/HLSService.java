package online.ericanders.camera.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class HLSService implements InitializingBean, DisposableBean {
    private static final Path HLS_DIRECORY = Paths.get("/tmp/hls");
    private static final String[] HLS_COMMAND = {
            "ffmpeg",
            "-y",
            "-fflags", "+genpts",
            "-f", "h264",
            "-r", "60",
            "-i", "pipe:0",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-tune", "zerolatency",
            "-g", "60",
            "-sc_threshold", "0",
            "-f", "hls",
            "-hls_time", "4",
            "-hls_list_size", "5",
            "-hls_flags", "delete_segments+append_list",
            HLS_DIRECORY.resolve("stream.m3u8").toString()
    };

    private Process hlsProcess;
    private OutputStream ffmpegInputStream;
    private volatile boolean running = false;
    private final Object writeLock = new Object();

    @Override
    public void afterPropertiesSet() throws Exception {
        Files.createDirectories(HLS_DIRECORY);
        startHLSProcess();
    }

    @Override
    public void destroy() {
        if (hlsProcess != null && hlsProcess.isAlive()) {
            hlsProcess.destroy();
        }

        if (ffmpegInputStream != null) {
            try {
                ffmpegInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void writeData(byte[] buffer, int offset, int length) {
        if (!running || ffmpegInputStream == null) {
            return;
        }

        synchronized (writeLock) {
            try {
                ffmpegInputStream.write(buffer, offset, length);
                ffmpegInputStream.flush();
            } catch (IOException e) {
                System.err.println("Error writing to HLS process: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void startHLSProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(HLS_COMMAND)
//                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        hlsProcess = pb.start();
        ffmpegInputStream = hlsProcess.getOutputStream();
        running = true;

        Thread processMonitor = new Thread(() -> {
            try {
                int exitCode = hlsProcess.waitFor();
                if (running) {
                    System.err.println("HLS process exited with code: " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "HLS-ProcessMonitor");

        processMonitor.setDaemon(true);
        processMonitor.start();
    }

}
