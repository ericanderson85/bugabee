package online.ericanders.camera.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/hls")
public class HLSController {
    private static final Path HLS_DIRECORY = Paths.get("/tmp/hls");

    @GetMapping(value = "/stream.m3u8")
    public Resource playlist() {
        return new FileSystemResource(HLS_DIRECORY.resolve("stream.m3u8"));
    }

    @GetMapping(value = "{seg:.+\\.ts}", produces = "video/mp2t")
    public Resource segment(@PathVariable String seg) {
        return new FileSystemResource(HLS_DIRECORY.resolve(seg));
    }
}
