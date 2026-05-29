-- 清理因历史 PID 变化累积的「死实例」心跳记录。
--
-- 背景：旧版 ai-spring-boot-starter 的 instanceId 形如 `hostname-PID@hostname`，
-- 业务系统每次重启 PID 会变，导致 ai_project_instance 表里同一台机器累积出大量
-- OFFLINE 状态的实例记录。本次升级把 SDK 改成基于 host + appName + port 的稳定
-- instanceId，这里同步把历史脏数据清理掉。
--
-- 策略：删除所有 OFFLINE / STALE 状态、且最近 24 小时没有再上报心跳的实例。
-- ONLINE / DISABLED 状态不动；24 小时内有心跳的也不动（避免误杀短暂离线后恢复的实例）。

DELETE FROM `ai_project_instance`
WHERE `status` IN ('OFFLINE', 'STALE')
  AND (
    `last_heartbeat_at` IS NULL
    OR `last_heartbeat_at` < DATE_SUB(NOW(), INTERVAL 24 HOUR)
  );
