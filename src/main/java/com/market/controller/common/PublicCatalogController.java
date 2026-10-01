package com.market.controller.common;

import com.market.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** 仅公开正在营业的集市里已上架商品，不提供任何写入操作。 */
@RestController
@RequestMapping("/api/public/products")
public class PublicCatalogController {
    public record ProductCard(Long id, String name, BigDecimal price, Integer stock,
                              String imageUrl, Long boothId, String boothName,
                              Long marketId, String marketName) {}
    public record ProductPage(List<ProductCard> items, long total, int page, int size) {}

    private final JdbcTemplate jdbc;

    public PublicCatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Result<ProductPage> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "24") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(48, Math.max(1, size));
        long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM product p
                JOIN booth b ON b.id = p.booth_id
                JOIN market m ON m.id = b.market_id
                WHERE p.sale_status = '上架' AND b.status = '已占用' AND m.status = 1
                """, Long.class);
        List<ProductCard> items = jdbc.query("""
                SELECT p.id, p.name, p.price, p.stock, p.image_url,
                       b.id AS booth_id, b.title AS booth_name,
                       m.id AS market_id, m.name AS market_name
                FROM product p
                JOIN booth b ON b.id = p.booth_id
                JOIN market m ON m.id = b.market_id
                WHERE p.sale_status = '上架' AND b.status = '已占用' AND m.status = 1
                ORDER BY p.update_time DESC, p.id DESC
                LIMIT ? OFFSET ?
                """, (rs, row) -> new ProductCard(
                        rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"),
                        rs.getInt("stock"), rs.getString("image_url"),
                        rs.getLong("booth_id"), rs.getString("booth_name"),
                        rs.getLong("market_id"), rs.getString("market_name")),
                safeSize, (long) (safePage - 1) * safeSize);
        return Result.success(new ProductPage(items, total, safePage, safeSize));
    }
}
