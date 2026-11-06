-- 智慧集市基础表结构（MySQL 8）。
-- 用于全新数据库初始化；已有数据库请使用 db/migration 下的增量脚本。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(64),
  avatar VARCHAR(500) DEFAULT '/images/default-avatar.svg',
  gender TINYINT DEFAULT 2,
  role VARCHAR(20) NOT NULL DEFAULT 'user',
  is_super_admin TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_phone (phone),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_role_status (role, status),
  CONSTRAINT chk_user_role CHECK (role IN ('super_admin', 'market_admin', 'vendor', 'user')),
  CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
  CONSTRAINT chk_user_super_admin CHECK (is_super_admin IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS market (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  location VARCHAR(255) NOT NULL,
  admin_id BIGINT,
  status TINYINT NOT NULL DEFAULT 1,
  max_booths INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_market_admin (admin_id),
  KEY idx_market_status (status),
  CONSTRAINT fk_market_admin FOREIGN KEY (admin_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_market_status CHECK (status IN (0, 1)),
  CONSTRAINT chk_market_max_booths CHECK (max_booths >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS booth (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  market_id BIGINT NOT NULL,
  position VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '空闲',
  vendor_id BIGINT,
  title VARCHAR(100) DEFAULT '新摊位',
  description TEXT,
  open_time VARCHAR(100),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_booth_market_status (market_id, status),
  KEY idx_booth_vendor (vendor_id),
  CONSTRAINT fk_booth_market FOREIGN KEY (market_id) REFERENCES market(id) ON DELETE RESTRICT,
  CONSTRAINT fk_booth_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_booth_status CHECK (status IN ('空闲', '已占用', '停用'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booth_id BIGINT NOT NULL,
  vendor_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  can_reserve TINYINT NOT NULL DEFAULT 0,
  sale_status VARCHAR(20) NOT NULL DEFAULT '下架',
  stock INT NOT NULL DEFAULT 0,
  image_url VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product_booth_status (booth_id, sale_status),
  KEY idx_product_vendor (vendor_id),
  KEY idx_product_updated (update_time),
  CONSTRAINT fk_product_booth FOREIGN KEY (booth_id) REFERENCES booth(id) ON DELETE RESTRICT,
  CONSTRAINT fk_product_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT chk_product_price CHECK (price >= 0),
  CONSTRAINT chk_product_stock CHECK (stock >= 0),
  CONSTRAINT chk_product_reserve CHECK (can_reserve IN (0, 1)),
  CONSTRAINT chk_product_sale_status CHECK (sale_status IN ('上架', '下架'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cart_user_product (user_id, product_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
  CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_master (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  customer_id BIGINT NOT NULL,
  customer_username VARCHAR(64),
  vendor_id BIGINT NOT NULL,
  vendor_username VARCHAR(64),
  market_id BIGINT,
  booth_id BIGINT,
  total_amount DECIMAL(12,2) NOT NULL,
  pay_time DATETIME,
  complete_time DATETIME,
  status VARCHAR(20) NOT NULL DEFAULT '待付款',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_customer_status (customer_id, status),
  KEY idx_order_vendor_status (vendor_id, status),
  KEY idx_order_market_status (market_id, status),
  KEY idx_order_created (create_time),
  KEY idx_order_completed (complete_time),
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_order_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_order_market FOREIGN KEY (market_id) REFERENCES market(id) ON DELETE SET NULL,
  CONSTRAINT fk_order_booth FOREIGN KEY (booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  CONSTRAINT chk_order_amount_nonnegative CHECK (total_amount >= 0),
  CONSTRAINT chk_order_status CHECK (status IN ('待付款', '已付款', '待取货', '已完成', '已取消'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  product_price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  reserve_start_time DATETIME,
  reserve_end_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_product (product_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES order_master(id) ON DELETE CASCADE,
  CONSTRAINT chk_order_item_price CHECK (product_price >= 0),
  CONSTRAINT chk_order_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  vendor_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '待确认',
  order_id BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_reservation_user_status (user_id, status),
  KEY idx_reservation_vendor_status (vendor_id, status),
  KEY idx_reservation_product_time (product_id, start_time, end_time),
  CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_reservation_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_reservation_order FOREIGN KEY (order_id) REFERENCES order_master(id) ON DELETE SET NULL,
  CONSTRAINT chk_reservation_time CHECK (end_time > start_time),
  CONSTRAINT chk_reservation_status CHECK (status IN ('待确认', '处理中', '已确认', '已拒绝', '已取消', '已完成'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS booth_apply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(20) NOT NULL,
  vendor_id BIGINT NOT NULL,
  origin_booth_id BIGINT,
  target_booth_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT '待审批',
  approve_admin_id BIGINT,
  apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  process_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_apply_vendor_status (vendor_id, status),
  KEY idx_apply_target_status (target_booth_id, status),
  CONSTRAINT fk_apply_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_apply_origin_booth FOREIGN KEY (origin_booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  CONSTRAINT fk_apply_target_booth FOREIGN KEY (target_booth_id) REFERENCES booth(id) ON DELETE SET NULL,
  CONSTRAINT fk_apply_admin FOREIGN KEY (approve_admin_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_apply_status CHECK (status IN ('待审批', '通过', '拒绝'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receiver_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  content VARCHAR(500) NOT NULL,
  business_id BIGINT,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notification_receiver (receiver_id, is_read, create_time),
  CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT chk_notification_read CHECK (is_read IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  description VARCHAR(500) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_admin_time (admin_id, create_time),
  CONSTRAINT fk_operation_admin FOREIGN KEY (admin_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS follow (
  customer_id BIGINT NOT NULL,
  vendor_id BIGINT NOT NULL,
  vendor_username VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (customer_id, vendor_id),
  KEY idx_follow_vendor (vendor_id),
  CONSTRAINT fk_follow_customer FOREIGN KEY (customer_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_follow_vendor FOREIGN KEY (vendor_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
