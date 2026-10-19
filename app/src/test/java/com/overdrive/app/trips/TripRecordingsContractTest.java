package com.overdrive.app.trips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.server.RecordingsIndex;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TripRecordingsContractTest {

    @Test
    public void testFormatSize() {
        assertEquals("0 B", RecordingsIndex.formatSize(0));
        assertEquals("500 B", RecordingsIndex.formatSize(500));
        assertEquals("1.0 KB", RecordingsIndex.formatSize(1000));
        assertEquals("50.0 MB", RecordingsIndex.formatSize(50_000_000L));
        assertEquals("1.0 GB", RecordingsIndex.formatSize(1_000_000_000L));
    }

    @Test
    public void testTripRecordingsRoutePatterns() throws Exception {
        Pattern listPattern = Pattern.compile("^/api/trips/(\\d+)/recordings(?:\\?.*)?$");
        Pattern downloadPattern = Pattern.compile("^/api/trips/(\\d+)/recordings/download(?:\\?.*)?$");

        Matcher m1 = listPattern.matcher("/api/trips/42/recordings");
        assertTrue(m1.matches());
        assertEquals("42", m1.group(1));

        Matcher m2 = listPattern.matcher("/api/trips/42/recordings?foo=bar");
        assertTrue(m2.matches());
        assertEquals("42", m2.group(1));

        assertFalse(listPattern.matcher("/api/trips/abc/recordings").matches());
        assertFalse(listPattern.matcher("/api/trips/42/recordings/download").matches());

        Matcher m3 = downloadPattern.matcher("/api/trips/99/recordings/download");
        assertTrue(m3.matches());
        assertEquals("99", m3.group(1));

        Matcher m4 = downloadPattern.matcher("/api/trips/99/recordings/download?ids=1,2,3");
        assertTrue(m4.matches());
        assertEquals("99", m4.group(1));

        assertFalse(downloadPattern.matcher("/api/trips/99/recordings").matches());
    }

    @Test
    public void testZeroCompressionStreamingContract() throws IOException {
        String handlerSource = readRepositoryFile(
                "app/src/main/java/com/overdrive/app/trips/TripApiHandler.java");
        String serverSource = readRepositoryFile(
                "app/src/main/java/com/overdrive/app/server/HttpServer.java");

        // Direct streaming without compression saves CPU/RAM on head units
        assertTrue("Must use NO_COMPRESSION for streaming pre-compressed video clips",
                handlerSource.contains("Deflater.NO_COMPRESSION"));
        assertTrue("Must stream with attachment no-store headers",
                handlerSource.contains("sendAttachmentNoStoreHeaders"));
        assertTrue("Must stream directly via ZipOutputStream",
                handlerSource.contains("ZipOutputStream(out)"));

        // HttpServer routes stream directly to TripApiHandler
        assertTrue("HttpServer must delegate streaming to TripApiHandler",
                serverSource.contains("handler.handleStream(path, method, out)"));
    }

    private static String readRepositoryFile(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("..", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
