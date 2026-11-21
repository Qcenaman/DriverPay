package com.qcenaman.driverpay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class FileNamesTest {
    @Test
    public void sanitizeKeepsNormalNames() {
        assertEquals("driver-pay-registos.csv", FileNames.sanitize("driver-pay-registos.csv"));
    }

    @Test
    public void sanitizeReplacesBadCharacters() {
        assertEquals("a_b_c.json", FileNames.sanitize("a/b:c.json"));
        assertEquals("driver-pay.txt", FileNames.sanitize("  "));
        assertEquals("driver-pay.txt", FileNames.sanitize(null));
    }

    @Test
    public void mimeTypes() {
        assertEquals("text/csv", FileNames.mimeFor("x.csv", "text/csv;charset=utf-8"));
        assertEquals("application/json", FileNames.mimeFor("x.json", ""));
        assertEquals("text/plain", FileNames.mimeFor("x", null));
        assertEquals("application/json", FileNames.mimeFor("x", "application/json;charset=utf-8"));
    }

    @Test
    public void bundledPageHasDownloadBridge() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get("src/main/assets/index.html")), StandardCharsets.UTF_8);
        assertTrue(html.contains("AndroidBridge.saveFile"));
        assertTrue(html.contains("driverpay_entries"));
    }
}
