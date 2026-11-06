-- 智慧集市主题商品种子数据（MySQL 8）。
-- 只选第一个“开放集市 + 已占用且绑定摊主”的摊位；若没有合适摊位，本脚本不会插入。
-- 重复运行不会给同一摊主重复添加同名商品。执行前请先核对目标摊位和经营者。
SELECT b.id AS booth_id, b.title AS booth_name, b.vendor_id, m.name AS market_name
FROM booth b JOIN market m ON m.id = b.market_id
WHERE b.status = '已占用' AND b.vendor_id IS NOT NULL AND m.status = 1
ORDER BY b.id LIMIT 1;

INSERT INTO product (booth_id, vendor_id, name, price, can_reserve, sale_status, stock, image_url)
SELECT target.id, target.vendor_id, ideas.name, ideas.price, ideas.can_reserve, '上架', ideas.stock, ideas.image_url
FROM (
  SELECT '晨露蜜柑小篮' AS name, 19.80 AS price, 1 AS can_reserve, 18 AS stock, '/products/mandarin.png' AS image_url
  UNION ALL SELECT '秋日果园苹果礼袋', 26.80, 1, 16, '/products/apple.png'
  UNION ALL SELECT '晚风香梨双人份', 16.80, 1, 20, '/products/pear.png'
  UNION ALL SELECT '日光青提分享盒', 29.90, 1, 12, '/products/grapes.png'
  UNION ALL SELECT '街角红心火龙果', 15.80, 0, 24, '/products/dragonfruit.png'
  UNION ALL SELECT '周末草莓野餐盒', 32.00, 1, 10, '/products/strawberry.png'
  UNION ALL SELECT '岭南蜜橘家庭装', 24.80, 1, 24, '/products/mandarin.png'
  UNION ALL SELECT '红富士苹果六枚装', 29.80, 1, 18, '/products/apple.png'
  UNION ALL SELECT '砀山酥梨家庭篮', 22.80, 1, 16, '/products/pear.png'
  UNION ALL SELECT '阳光青提双人盒', 35.80, 1, 12, '/products/grapes.png'
  UNION ALL SELECT '红心火龙果双果装', 18.80, 0, 20, '/products/dragonfruit.png'
  UNION ALL SELECT '奶油草莓分享篮', 36.80, 1, 10, '/products/strawberry.png'
) AS ideas
CROSS JOIN (
  SELECT b.id, b.vendor_id FROM booth b JOIN market m ON m.id = b.market_id
  WHERE b.status = '已占用' AND b.vendor_id IS NOT NULL AND m.status = 1
  ORDER BY b.id LIMIT 1
) AS target
WHERE NOT EXISTS (
  SELECT 1 FROM product p
  WHERE p.booth_id = target.id AND p.vendor_id = target.vendor_id AND p.name = ideas.name
);

-- 同步旧数据中的插画地址；图片均为项目自带、风格统一的真实商品摄影资源。
UPDATE product SET image_url = REPLACE(image_url, '.svg', '.png')
WHERE image_url IN ('/products/mandarin.svg', '/products/apple.svg', '/products/pear.svg',
                    '/products/grapes.svg', '/products/dragonfruit.svg', '/products/strawberry.svg');

-- 真实经营时，摊主仍应在“商品管理”替换为实际商品照片，并核对库存和价格。
