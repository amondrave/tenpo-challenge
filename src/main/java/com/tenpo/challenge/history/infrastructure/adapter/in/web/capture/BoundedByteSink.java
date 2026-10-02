package com.tenpo.challenge.history.infrastructure.adapter.in.web.capture;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class BoundedByteSink {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final int maxBytes;
    private volatile boolean truncated = false;

    public BoundedByteSink(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    public synchronized void write(byte[] bytes) {
        int writable = Math.max(Math.min(maxBytes - buffer.size(), bytes.length), 0);
        buffer.write(bytes, 0, writable);
        truncated = truncated || writable < bytes.length;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public synchronized String text() {
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
