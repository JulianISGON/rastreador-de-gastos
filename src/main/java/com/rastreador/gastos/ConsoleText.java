package com.rastreador.gastos;

import java.text.Normalizer;

public final class ConsoleText {

    private ConsoleText() {
    }

    public static String ascii(String value) {
        if (value == null) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String cleaned = normalized.replace("…", "...");

        if (cleaned.length() >= 2 && cleaned.startsWith("\\") && cleaned.endsWith("\\")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned;
    }
}