-- 为存量数据库补齐交易完成时间和核心数据约束（MySQL 8）。
-- 执行前建议先备份；若历史数据存在孤儿记录或非法状态，外键/检查约束会拒绝创建，
-- 应先修复数据，而不是关闭约束。

ALTER TABLE order_master
  ADD COLUMN complete_time DATETIME NULL AFTER pay_time,
  ADD KEY idx_order_completed (complete_time);

-- 旧的已完成订单没有完成时间，使用最后更新时间作为最接近的历史时间。
UPDATE order_master
SET complete_time = COALESCE(update_time, pay_time, create_time)
WHERE status = '已完成' AND complete_time IS NULL;

ALTER TABLE sys_user
  ADD CONSTRAINT chk_user_role CHECK (role IN ('super_admin', 'market_admin', 'vendor', 'user')),
  ADD CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
  ADD CONSTRAINT chk_user_super_admin CHECK (is_super_admin IN (0, 1));

-- 购物车不是历史凭证；商品或用户已经不存在时，该条目已无法继续结算，可安全清理。
DELETE c FROM cart c
LEFT JOIN sys_user u ON u.id = c.user_id
LEFT JOIN product p ON p.id = c.product_id
WHERE u.id IS NULL OR p.id IS NULL;

ALTER TABLE market
  ADD CONSTRAINT fk_market_admin FOREIGN KEY (admin_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  ADD CONSTRAINT chk_market_status CHECK (status IN (0, 1)),
  ADD CONSTRAINT chk_market_max_booths CHECK (max_booths >= 0);

ALTER TABLE booth
  ADD CONSTRAINT fk_booth_market FOREIGN KEY (market_id) REFERENCES market(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_booth_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  ADD CONSTRAINT chk_booth_status CHECK (status IN ('空闲', '已占用', '停用'));

ALTER TABLE product
  ADD CONSTRAINT fk_product_booth FOREIGN KEY (booth_id) REFERENCES booth(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_product_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT chk_product_price CHECK (price >= 0),
  ADD CONSTRAINT chk_product_stock CHECK (stock >= 0),
  ADD CONSTRAINT chk_product_reserve CHECK (can_reserve IN (0, 1)),
  ADD CONSTRAINT chk_product_sale_status CHECK (sale_status IN ('上架', '下架'));

ALTER TABLE cart
  ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
  ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);

ALTER TABLE order_master
  ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_order_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_order_market FOREIGN KEY (market_id) REFERENCES market(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_order_booth FOREIGN KEY (booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  ADD CONSTRAINT chk_order_amount_nonnegative CHECK (total_amount >= 0),
  ADD CONSTRAINT chk_order_status CHECK (status IN ('待付款', '已付款', '待取货', '已完成', '已取消'));

ALTER TABLE order_item
  ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES order_master(id) ON DELETE CASCADE,
  ADD CONSTRAINT chk_order_item_price CHECK (product_price >= 0),
  ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0);

ALTER TABLE reservation
  ADD CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_reservation_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_reservation_order FOREIGN KEY (order_id) REFERENCES order_master(id) ON DELETE SET NULL,
  ADD CONSTRAINT chk_reservation_time CHECK (end_time > start_time),
  ADD CONSTRAINT chk_reservation_status CHECK (status IN ('待确认', '处理中', '已确认', '已拒绝', '已取消', '已完成'));

ALTER TABLE booth_apply
  ADD CONSTRAINT fk_apply_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_apply_origin_booth FOREIGN KEY (origin_booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_apply_target_booth FOREIGN KEY (target_booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_apply_admin FOREIGN KEY (approve_admin_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  ADD CONSTRAINT chk_apply_status CHECK (status IN ('待审批', '通过', '拒绝'));

ALTER TABLE notification
  ADD CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  ADD CONSTRAINT chk_notification_read CHECK (is_read IN (0, 1));

ALTER TABLE operation_log
  ADD CONSTRAINT fk_operation_admin FOREIGN KEY (admin_id) REFERENCES sys_user(id) ON DELETE RESTRICT;

ALTER TABLE follow
  ADD CONSTRAINT fk_follow_customer FOREIGN KEY (customer_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_follow_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE CASCADE;
