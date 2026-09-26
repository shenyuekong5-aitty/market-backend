package com.market.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiCatalogService {
    private record Item(String name, BigDecimal price, int stock, String boothName, Long boothId, String marketName) {}

    private final JdbcTemplate jdbc;

    public AiCatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> answerIfCatalogQuestion(String question) {
        String normalized = question.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (!isCatalogQuestion(normalized)) return Optional.empty();

        List<Item> items = jdbc.query("""
                SELECT p.name, p.price, p.stock, b.title AS booth_name, b.id AS booth_id, m.name AS market_name
                FROM product p
                JOIN booth b ON b.id = p.booth_id
                JOIN market m ON m.id = b.market_id
                WHERE p.sale_status = '上架' AND b.status = '已占用' AND m.status = 1
                ORDER BY p.update_time DESC, p.id DESC
                LIMIT 12
                """, (rs, row) -> new Item(rs.getString("name"), rs.getBigDecimal("price"),
                rs.getInt("stock"), rs.getString("booth_name"), rs.getLong("booth_id"),
                rs.getString("market_name")));

        if (items.isEmpty()) {
            return Optional.of("目前没有查询到已开放集市中的上架商品。你可以稍后在“探索集市”查看各摊位的最新商品。");
        }
        StringBuilder answer = new StringBuilder("当前部分在售商品（最多展示 12 件）：\n");
        for (Item item : items) {
            answer.append("· ").append(item.name()).append(" ¥")
                    .append(item.price()).append("｜").append(item.marketName())
                    .append(" · ").append(item.boothName()).append("（摊位 #").append(item.boothId()).append("）");
            if (item.stock() <= 0) answer.append("（暂时缺货）");
            answer.append("\n");
        }
        answer.append("可进入对应集市和摊位查看详情；价格与库存以商品页面实时信息为准。");
        return Optional.of(answer.toString());
    }

    static boolean isCatalogQuestion(String question) {
        if (question.equals("商品") || question.equals("商品列表") || question.equals("商品推荐")) return true;
        if (question.contains("有什么卖") || question.contains("卖什么") || question.contains("卖啥")) return true;
        if (!question.contains("商品")) return false;
        return List.of("哪些", "那些", "什么", "有啥", "有哪", "列表", "推荐", "都有", "有卖", "在售")
                .stream().anyMatch(question::contains);
    }
}
