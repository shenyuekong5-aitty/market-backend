package com.market.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiKnowledgeService {
    public record Entry(String id, String question, List<String> keywords, String answer) {}

    // 宽泛名词单独出现才命中，避免“商品退货政策”被误判为普通商品问题。
    private static final Set<String> GENERIC_TERMS = Set.of(
            "集市", "摊位", "商品", "订单", "预订", "购物车", "支付", "付款", "消息", "通知", "账户");
    private static final List<String> SUGGESTION_IDS = List.of(
            "market", "apply", "browse-goods", "reservation", "order", "profile");

    private final List<Entry> entries;

    // 启动时一次性加载 FAQ；维护文件后需重启服务。
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
        return SUGGESTION_IDS.stream()
                .map(this::findById)
                .filter(entry -> entry != null)
                .map(Entry::question)
                .toList();
    }

    // 只处理能从上一话题明确推断的短追问；不拿历史话题猜测任意问题。
    public List<Entry> followUp(String question, String previousId) {
        String query = normalize(question);
        if (previousId == null || query.length() > 16) return List.of();
        String targetId = null;
        if (query.contains("取消") || query.contains("撤销")) {
            if (previousId.startsWith("reservation")) targetId = "reservation-cancel";
            else if (previousId.startsWith("order") || previousId.equals("payment")) targetId = "order-cancel";
        } else if ((query.contains("多久") || query.contains("进度"))
                && (previousId.equals("apply") || previousId.equals("approval"))) {
            targetId = "approval";
        }
        Entry entry = targetId == null ? null : findById(targetId);
        return entry == null ? List.of() : List.of(entry);
    }

    private Entry findById(String id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    private int score(Entry entry, String query) {
        int best = 0;
        for (String keyword : entry.keywords()) {
            String term = normalize(keyword);
            // Short conversational intents must match the whole input; e.g. "hi" must not match "shipping".
            if (isConversational(entry) && !query.equals(term)) continue;
            // A broad noun should not make unrelated questions look answered.
            if (GENERIC_TERMS.contains(term) && !query.equals(term)) continue;
            if (!term.isEmpty() && query.contains(term)) {
                best = Math.max(best, term.length() * 2 + (query.equals(term) ? 20 : 0));
            }
        }
        return best;
    }

    private boolean isConversational(Entry entry) {
        return entry.id().equals("greeting") || entry.id().equals("help")
                || entry.id().equals("thanks");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?：:（）()；;\"'“”‘’]", "")
                .replace("预定", "预订")
                .replace("登陆", "登录")
                .replace("定单", "订单");
    }

    private record ScoredEntry(Entry entry, int score) {}
}
