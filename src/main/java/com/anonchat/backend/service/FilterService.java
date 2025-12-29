package com.anonchat.backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class FilterService {

    /** Profanity with severity level */
    private static final Map<String, Integer> BAD_WORDS = Map.of(
            "stupid", 1,
            "idiot", 1,
            "dumb", 1,
            "ass", 2
    );

    /** Reserved usernames */
    private static final Set<String> RESERVED_NAMES = Set.of(
            "admin", "administrator", "system",
            "mod", "moderator", "owner",
            "server", "anonchat"
    );

    /** Precompiled profanity patterns */
    private static final List<Pattern> BAD_WORD_PATTERNS = BAD_WORDS.keySet()
            .stream()
            .map(word ->
                    Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)
            )
            .toList();

    /**
     * Sanitize user-generated content
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = normalize(input);
        String sanitized = input;

        for (Pattern pattern : BAD_WORD_PATTERNS) {
            sanitized = pattern.matcher(sanitized)
                    .replaceAll(match -> "*".repeat(match.group().length()));
        }

        return sanitized;
    }

    /**
     * Validate usernames against reserved names
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;

        String normalized = normalize(username);

        // 1. Check Exact Match
        if (RESERVED_NAMES.contains(normalized)) return false;

        // 2. Check Partial Match (Starts with or Contains)
        // This blocks "admin_123", "super_admin", "moderator_dave"
        for (String reserved : RESERVED_NAMES) {
            if (normalized.contains(reserved)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Normalize text:
     * - lowercase
     * - remove accents
     * - collapse spaces
     * - remove punctuation
     */
    private String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "")      // remove accents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // remove symbols
                .replaceAll("\\s+", " ")        // normalize spaces
                .trim();
    }
}
