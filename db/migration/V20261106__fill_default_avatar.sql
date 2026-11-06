-- 为历史空头像账号补充系统默认头像。
-- 新注册账号由 sys_user.avatar 的默认值和 UserService 双重兜底。
UPDATE sys_user
SET avatar = '/images/default-avatar.svg'
WHERE avatar IS NULL OR TRIM(avatar) = '';
