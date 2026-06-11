package org.iosmcn.ims.n5.utility;



import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamRequestBody extends RequestBody {
    private final MediaType contentType;
    private final byte[] content;

    public InputStreamRequestBody(MediaType contentType, InputStream inputStream) throws IOException {
        this.contentType = contentType;
        this.content = inputStream.readAllBytes();
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = byteArrayInputStream.read(buffer)) != -1) {
                sink.write(buffer, 0, bytesRead);
            }
        }
    }

    public long contentLength() throws IOException {
        return content.length;
    }
}
