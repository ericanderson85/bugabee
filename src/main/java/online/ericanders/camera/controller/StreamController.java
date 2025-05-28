package online.ericanders.camera.controller;

import online.ericanders.camera.component.StreamMultiplexer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class StreamController {

    private final StreamMultiplexer multiplexer;

    public StreamController(StreamMultiplexer multiplexer) {
        this.multiplexer = multiplexer;
    }

    @GetMapping(value = "/stream", produces = "video/h264")
    public ResponseEntity<StreamingResponseBody> stream() {
        StreamingResponseBody responseBody = outputStream -> {
            multiplexer.addClient(outputStream);
            try {
                // Block until the client disconnects
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {
            } finally {
                multiplexer.removeClient(outputStream);
            }
        };
        return ResponseEntity.ok()
                .header("Connection", "close")
                .contentType(org.springframework.http.MediaType.valueOf("video/h264"))
                .body(responseBody);
    }
}
