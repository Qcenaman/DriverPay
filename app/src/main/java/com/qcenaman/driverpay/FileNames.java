package com.qcenaman.driverpay;

final class FileNames {
    private FileNames() {}

    static String sanitize(String name) {
        if (name == null) return "driver-pay.txt";
        String n = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return n.isEmpty() ? "driver-pay.txt" : n;
    }

    static String mimeFor(String name, String fallback) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".csv")) return "text/csv";
        if (n.endsWith(".json")) return "application/json";
        if (fallback != null && !fallback.isEmpty()) return fallback.split(";")[0].trim();
        return "text/plain";
    }
}
