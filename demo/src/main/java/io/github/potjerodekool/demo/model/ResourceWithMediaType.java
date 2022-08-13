package io.github.potjerodekool.demo.model;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;

public record ResourceWithMediaType(InputStream inputStream,
                                    MediaType mimeType) {

    public byte[] bytes() throws IOException {
        return inputStream.readAllBytes();
    }
}
