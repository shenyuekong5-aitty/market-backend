-- Split existing admin accounts into platform and market administrators.
-- Idempotent: only legacy role='admin' rows are changed.
START TRANSACTION;
UPDATE sys_user
SET role = 'super_admin', is_super_admin = 1
WHERE role = 'admin' AND is_super_admin = 1;
UPDATE sys_user
SET role = 'market_admin', is_super_admin = 0
WHERE role = 'admin' AND (is_super_admin IS NULL OR is_super_admin <> 1);
COMMIT;

ALTER TABLE sys_user MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user'
    COMMENT 'role: super_admin/market_admin/vendor/user';
