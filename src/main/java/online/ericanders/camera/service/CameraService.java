package online.ericanders.camera.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class CameraService {
//    private static final String[] LIBCAMERA_COMMAND = {
//            "libcamera-vid",
//            "-t", "0",
//            "--width", "1920",
//            "--height", "1080",
//            "--framerate", "60",
//            "--codec", "h264",
//            "--inline-headers",
//            "-o", "-"
//    };

    private static final String[] LIBCAMERA_COMMAND = {
            "ffmpeg",
            "-f", "lavfi",
            "-i", "testsrc2=size=1920x1080:rate=60",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-tune", "zerolatency",
            "-x264-params", "keyint=60:scenecut=0:repeat-headers=1",
            "-f", "h264",
            "-"
    };


    private Process libcameraProcess;

    public InputStream getCameraStream() throws IOException {
        if (libcameraProcess != null && libcameraProcess.isAlive()) {
            libcameraProcess.destroy();
        }

        libcameraProcess = new ProcessBuilder(LIBCAMERA_COMMAND)
                .redirectErrorStream(true)
                .start();

        return libcameraProcess.getInputStream();
    }

    public void stop() {
        if (libcameraProcess != null && libcameraProcess.isAlive()) {
            libcameraProcess.destroy();
        }
    }


}
