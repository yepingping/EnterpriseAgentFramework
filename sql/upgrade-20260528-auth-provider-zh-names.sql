-- 认证源种子数据展示名称中文化（已有环境升级）
UPDATE `platform_auth_provider` SET `provider_name` = '本地开发登录' WHERE `provider_code` = 'LOCAL';
UPDATE `platform_auth_provider` SET `provider_name` = '受信任网关请求头' WHERE `provider_code` = 'HEADER';
UPDATE `platform_auth_provider` SET `provider_name` = '企业 OIDC 登录' WHERE `provider_code` = 'OIDC';
UPDATE `platform_auth_provider` SET `provider_name` = '企业 SAML 登录' WHERE `provider_code` = 'SAML';
