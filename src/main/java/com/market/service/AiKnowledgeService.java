package com.market.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AiKnowledgeService {
    public record Entry(String id, String question, List<String> keywords, String answer) {}

    private final List<Entry> entries;

    public AiKnowledgeService(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("ai/faq.json").getInputStream()) {
            entries = List.copyOf(mapper.readValue(stream, new TypeReference<List<Entry>>() {}));
        }
    }

    public List<Entry> search(String question, int limit) {
        String query = normalize(question);
        if (query.isEmpty()) return List.of();
        return entries.stream()
                .map(entry -> new ScoredEntry(entry, score(entry, query)))
                .filter(hit -> hit.score > 0)
                .sorted(Comparator.comparingInt(ScoredEntry::score).reversed())
                .limit(limit)
                .map(ScoredEntry::entry)
                .toList();
    }

    public List<String> suggestions() {
        return entries.stream().limit(6).map(Entry::question).toList();
    }

    private int score(Entry entry, String query) {
        int best = 0;
        for (String keyword : entry.keywords()) {
            String term = normalize(keyword);
            if (!term.isEmpty() && query.contains(term)) {
                best = Math.max(best, term.length() * 2 + (query.equals(term) ? 20 : 0));
            }
        }
        return best;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?：:（）()]", "");
    }

    private record ScoredEntry(Entry entry, int score) {}
}
