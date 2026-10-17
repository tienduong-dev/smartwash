-- =====================================================================
-- NHÓM 1 — Tài khoản, phân quyền, địa chỉ, thông báo, nhật ký
-- Đặc tả: mục 3, 4.1, 9.1, 12, 13.4
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- users — BR-01: vai trò KHÔNG nằm ở đây, nằm ở user_roles
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[users] (
    [id]                 bigint          IDENTITY(1,1) NOT NULL,
    [full_name]          nvarchar(255)   NOT NULL,
    [email]              nvarchar(255)   NOT NULL,
    [phone]              nvarchar(20)    NULL,
    [password_hash]      nvarchar(255)   NOT NULL,
    [avatar]             nvarchar(255)   NULL,
    [status]             nvarchar(30)    NOT NULL CONSTRAINT [DF_users_status] DEFAULT N'ACTIVE',
    [email_verified_at]  datetime2(0)    NULL,
    [failed_login_count] int             NOT NULL CONSTRAINT [DF_users_failed] DEFAULT 0,
    [locked_until]       datetime2(0)    NULL,
    [last_login_at]      datetime2(0)    NULL,
    [created_at]         datetime2(0)    NOT NULL CONSTRAINT [DF_users_created] DEFAULT SYSDATETIME(),
    [updated_at]         datetime2(0)    NULL,
    CONSTRAINT [PK_users] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_users_email] UNIQUE ([email]),
    CONSTRAINT [CK_users_status] CHECK ([status] IN (N'ACTIVE', N'LOCKED', N'PENDING_VERIFICATION'))
);
GO
-- Số điện thoại duy nhất nhưng cho phép nhiều NULL
-- (UNIQUE thường của SQL Server chỉ cho đúng 1 NULL, nên dùng chỉ mục lọc).
CREATE UNIQUE INDEX [UX_users_phone] ON [dbo].[users] ([phone]) WHERE [phone] IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- roles / user_roles — mục 9.1
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[roles] (
    [id]          int           IDENTITY(1,1) NOT NULL,
    [code]        nvarchar(30)  NOT NULL,
    [name]        nvarchar(100) NOT NULL,
    [description] nvarchar(255) NULL,
    CONSTRAINT [PK_roles] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_roles_code] UNIQUE ([code]),
    CONSTRAINT [CK_roles_code] CHECK ([code] IN (N'ADMIN', N'SELLER_OWNER', N'SELLER_STAFF', N'CUSTOMER'))
);
GO

CREATE TABLE [dbo].[user_roles] (
    [user_id]    bigint       NOT NULL,
    [role_id]    int          NOT NULL,
    [granted_at] datetime2(0) NOT NULL CONSTRAINT [DF_user_roles_at] DEFAULT SYSDATETIME(),
    [granted_by] bigint       NULL,
    CONSTRAINT [PK_user_roles] PRIMARY KEY ([user_id], [role_id]),
    CONSTRAINT [FK_user_roles_user] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_user_roles_role] FOREIGN KEY ([role_id]) REFERENCES [dbo].[roles] ([id]),
    CONSTRAINT [FK_user_roles_granter] FOREIGN KEY ([granted_by]) REFERENCES [dbo].[users] ([id])
);
GO
CREATE INDEX [IX_user_roles_role] ON [dbo].[user_roles] ([role_id]);
GO

-- ---------------------------------------------------------------------
-- addresses — mục 5.6
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[addresses] (
    [id]            bigint        IDENTITY(1,1) NOT NULL,
    [user_id]       bigint        NOT NULL,
    [label]         nvarchar(50)  NULL,
    [receiver_name] nvarchar(255) NOT NULL,
    [phone]         nvarchar(20)  NOT NULL,
    [line]          nvarchar(500) NOT NULL,
    [province_id]   int           NOT NULL,
    [province_name] nvarchar(255) NOT NULL,
    [district_id]   int           NOT NULL,
    [district_name] nvarchar(255) NOT NULL,
    [ward_code]     nvarchar(30)  NOT NULL,
    [ward_name]     nvarchar(255) NOT NULL,
    [is_default]    bit           NOT NULL CONSTRAINT [DF_addresses_default] DEFAULT 0,
    [created_at]    datetime2(0)  NOT NULL CONSTRAINT [DF_addresses_created] DEFAULT SYSDATETIME(),
    [updated_at]    datetime2(0)  NULL,
    CONSTRAINT [PK_addresses] PRIMARY KEY ([id]),
    CONSTRAINT [FK_addresses_user] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE
);
GO
CREATE INDEX [IX_addresses_user] ON [dbo].[addresses] ([user_id]);
GO

-- ---------------------------------------------------------------------
-- password_reset_tokens — mục 4.1
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[password_reset_tokens] (
    [email]      nvarchar(255) NOT NULL,
    [token]      nvarchar(255) NOT NULL,
    [expires_at] datetime2(0)  NOT NULL,
    [created_at] datetime2(0)  NOT NULL CONSTRAINT [DF_prt_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_password_reset_tokens] PRIMARY KEY ([email])
);
GO

-- ---------------------------------------------------------------------
-- notifications — mục 9.1
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[notifications] (
    [id]         bigint         IDENTITY(1,1) NOT NULL,
    [user_id]    bigint         NOT NULL,
    [type]       nvarchar(50)   NOT NULL,
    [title]      nvarchar(255)  NOT NULL,
    [content]    nvarchar(max)  NULL,
    [link]       nvarchar(255)  NULL,
    [read_at]    datetime2(0)   NULL,
    [created_at] datetime2(0)   NOT NULL CONSTRAINT [DF_notifications_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_notifications] PRIMARY KEY ([id]),
    CONSTRAINT [FK_notifications_user] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE
);
GO
CREATE INDEX [IX_notifications_user_unread] ON [dbo].[notifications] ([user_id], [read_at]);
GO

-- ---------------------------------------------------------------------
-- audit_logs — BR-15, mục 13.4
-- Không bao giờ ghi mật khẩu, token hay dữ liệu thẻ vào đây.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[audit_logs] (
    [id]            bigint        IDENTITY(1,1) NOT NULL,
    [actor_user_id] bigint        NULL,
    [action]        nvarchar(60)  NOT NULL,
    [entity_type]   nvarchar(60)  NOT NULL,
    [entity_id]     bigint        NULL,
    [old_value]     nvarchar(max) NULL,
    [new_value]     nvarchar(max) NULL,
    [reason]        nvarchar(500) NULL,
    [ip_address]    nvarchar(45)  NULL,
    [created_at]    datetime2(0)  NOT NULL CONSTRAINT [DF_audit_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_audit_logs] PRIMARY KEY ([id]),
    CONSTRAINT [FK_audit_actor] FOREIGN KEY ([actor_user_id]) REFERENCES [dbo].[users] ([id])
);
GO
CREATE INDEX [IX_audit_entity]  ON [dbo].[audit_logs] ([entity_type], [entity_id]);
CREATE INDEX [IX_audit_created] ON [dbo].[audit_logs] ([created_at] DESC);
CREATE INDEX [IX_audit_action]  ON [dbo].[audit_logs] ([action]);
GO

-- ---------------------------------------------------------------------
-- settings — cấu hình toàn sàn
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[settings] (
    [id]            bigint        IDENTITY(1,1) NOT NULL,
    [setting_key]   nvarchar(100) NOT NULL,
    [setting_value] nvarchar(500) NULL,
    [description]   nvarchar(255) NULL,
    [updated_at]    datetime2(0)  NULL,
    CONSTRAINT [PK_settings] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_settings_key] UNIQUE ([setting_key])
);
GO

PRINT N'01 — Xong nhóm tài khoản và phân quyền.';
GO
