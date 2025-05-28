package online.ericanders.camera.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class CameraService {
    private static final String[] LIBCAMERA_COMMAND = {
            "libcamera-vid",
            "-t", "0",            // unlimited time
            "-o", "-",            // output to stdout
            "--width", "1920",
            "--height", "1080",
            "--framerate", "60",
            "--codec", "h264",
            "--nopreview"         // don't display to the pi
    };

//    private static final String[] LIBCAMERA_COMMAND = {
//            "ffmpeg",
//            "-f", "lavfi",
//            "-i", "testsrc2=size=1920x1080:rate=60",
//            "-c:v", "libx264",
//            "-preset", "ultrafast",
//            "-tune", "zerolatency",
//            "-f", "h264",
//            "-"
//    };

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
