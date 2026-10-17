-- =====================================================================
-- NHÓM 2 — Doanh nghiệp, thương hiệu, hồ sơ mở gian hàng, gian hàng
-- Đặc tả: mục 4.2, 4.3, 6.1, 6.3, 6.10, 7.3, 7.4, 7.5, BR-02..BR-06
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- businesses — pháp nhân vận hành gian hàng
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[businesses] (
    [id]                     bigint        IDENTITY(1,1) NOT NULL,
    [legal_name]             nvarchar(255) NOT NULL,
    [tax_code]               nvarchar(20)  NULL,
    [address]                nvarchar(500) NULL,
    [representative_name]    nvarchar(255) NULL,
    [representative_phone]   nvarchar(20)  NULL,
    [representative_email]   nvarchar(255) NULL,
    [verification_status]    nvarchar(30)  NOT NULL CONSTRAINT [DF_business_vstatus] DEFAULT N'PENDING',
    [verified_at]            datetime2(0)  NULL,
    [verified_by]            bigint        NULL,
    [note]                   nvarchar(500) NULL,
    [created_at]             datetime2(0)  NOT NULL CONSTRAINT [DF_business_created] DEFAULT SYSDATETIME(),
    [updated_at]             datetime2(0)  NULL,
    CONSTRAINT [PK_businesses] PRIMARY KEY ([id]),
    CONSTRAINT [FK_businesses_verifier] FOREIGN KEY ([verified_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_businesses_vstatus] CHECK ([verification_status] IN (N'PENDING', N'VERIFIED', N'REJECTED'))
);
GO
CREATE UNIQUE INDEX [UX_businesses_tax] ON [dbo].[businesses] ([tax_code]) WHERE [tax_code] IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- brands — BR-02. Dò trùng bằng normalized_name, KHÔNG unique tên thô (mục 9.3)
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[brands] (
    [id]              bigint        IDENTITY(1,1) NOT NULL,
    [name]            nvarchar(150) NOT NULL,
    [normalized_name] nvarchar(150) NOT NULL,
    [slug]            nvarchar(150) NOT NULL,
    [logo]            nvarchar(255) NULL,
    [description]     nvarchar(max) NULL,
    [website]         nvarchar(255) NULL,
    [country]         nvarchar(100) NULL,
    [status]          nvarchar(30)  NOT NULL CONSTRAINT [DF_brands_status] DEFAULT N'PENDING_VERIFICATION',
    [created_by]      bigint        NULL,
    [verified_by]     bigint        NULL,
    [verified_at]     datetime2(0)  NULL,
    [created_at]      datetime2(0)  NOT NULL CONSTRAINT [DF_brands_created] DEFAULT SYSDATETIME(),
    [updated_at]      datetime2(0)  NULL,
    CONSTRAINT [PK_brands] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_brands_slug] UNIQUE ([slug]),
    CONSTRAINT [FK_brands_creator]  FOREIGN KEY ([created_by])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_brands_verifier] FOREIGN KEY ([verified_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_brands_status] CHECK ([status] IN (N'PENDING_VERIFICATION', N'ACTIVE', N'LOCKED'))
);
GO
CREATE INDEX [IX_brands_normalized] ON [dbo].[brands] ([normalized_name]);
CREATE INDEX [IX_brands_status]     ON [dbo].[brands] ([status]);
GO

-- ---------------------------------------------------------------------
-- seller_registrations — mục 4.2, 6.1, 7.4
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[seller_registrations] (
    [id]                   bigint         IDENTITY(1,1) NOT NULL,
    [code]                 nvarchar(20)   NOT NULL,
    [applicant_user_id]    bigint         NOT NULL,
    [business_id]          bigint         NULL,
    [requested_brand_id]   bigint         NULL,
    [requested_brand_name] nvarchar(150)  NULL,
    [store_name]           nvarchar(255)  NOT NULL,
    [store_slug_proposed]  nvarchar(255)  NULL,
    [store_type]           nvarchar(30)   NOT NULL,
    [contact_phone]        nvarchar(20)   NULL,
    [contact_email]        nvarchar(255)  NULL,
    [pickup_address]       nvarchar(500)  NULL,
    [applicant_note]       nvarchar(1000) NULL,
    [status]               nvarchar(30)   NOT NULL CONSTRAINT [DF_sreg_status] DEFAULT N'PENDING',
    [review_note]          nvarchar(1000) NULL,
    [reviewed_by]          bigint         NULL,
    [reviewed_at]          datetime2(0)   NULL,
    [created_store_id]     bigint         NULL,
    [created_at]           datetime2(0)   NOT NULL CONSTRAINT [DF_sreg_created] DEFAULT SYSDATETIME(),
    [updated_at]           datetime2(0)   NULL,
    CONSTRAINT [PK_seller_registrations] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_sreg_code] UNIQUE ([code]),
    CONSTRAINT [FK_sreg_applicant] FOREIGN KEY ([applicant_user_id])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_sreg_business]  FOREIGN KEY ([business_id])        REFERENCES [dbo].[businesses] ([id]),
    CONSTRAINT [FK_sreg_brand]     FOREIGN KEY ([requested_brand_id]) REFERENCES [dbo].[brands] ([id]),
    CONSTRAINT [FK_sreg_reviewer]  FOREIGN KEY ([reviewed_by])        REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_sreg_status] CHECK ([status] IN
        (N'PENDING', N'UNDER_REVIEW', N'NEEDS_MORE_INFO', N'APPROVED', N'REJECTED', N'CANCELLED')),
    CONSTRAINT [CK_sreg_store_type] CHECK ([store_type] IN (N'OFFICIAL', N'DISTRIBUTOR', N'RETAILER')),
    -- Phải nêu được brand: hoặc chọn brand có sẵn, hoặc đề xuất tên brand mới
    CONSTRAINT [CK_sreg_brand_nguon] CHECK
        ([requested_brand_id] IS NOT NULL OR [requested_brand_name] IS NOT NULL)
);
GO
CREATE INDEX [IX_sreg_status]    ON [dbo].[seller_registrations] ([status]);
CREATE INDEX [IX_sreg_applicant] ON [dbo].[seller_registrations] ([applicant_user_id]);
GO

-- ---------------------------------------------------------------------
-- seller_documents — mục 12.1. File lưu ở uploads-private/, KHÔNG công khai.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[seller_documents] (
    [id]              bigint        IDENTITY(1,1) NOT NULL,
    [registration_id] bigint        NOT NULL,
    [document_type]   nvarchar(40)  NOT NULL,
    [file_path]       nvarchar(500) NOT NULL,
    [original_name]   nvarchar(255) NULL,
    [mime_type]       nvarchar(100) NULL,
    [size_bytes]      bigint        NULL,
    [status]          nvarchar(30)  NOT NULL CONSTRAINT [DF_sdoc_status] DEFAULT N'PENDING',
    [note]            nvarchar(500) NULL,
    [uploaded_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_sdoc_uploaded] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_seller_documents] PRIMARY KEY ([id]),
    CONSTRAINT [FK_sdoc_registration] FOREIGN KEY ([registration_id])
        REFERENCES [dbo].[seller_registrations] ([id]) ON DELETE CASCADE,
    CONSTRAINT [CK_sdoc_type] CHECK ([document_type] IN
        (N'BUSINESS_LICENSE', N'AUTHORIZATION_LETTER', N'DISTRIBUTION_CONTRACT', N'ID_CARD', N'OTHER')),
    CONSTRAINT [CK_sdoc_status] CHECK ([status] IN (N'PENDING', N'ACCEPTED', N'NEEDS_REPLACEMENT'))
);
GO
CREATE INDEX [IX_sdoc_registration] ON [dbo].[seller_documents] ([registration_id]);
GO

-- ---------------------------------------------------------------------
-- stores — BR-03: đúng MỘT brand, không đổi sau khi duyệt
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[stores] (
    [id]               bigint        IDENTITY(1,1) NOT NULL,
    [business_id]      bigint        NOT NULL,
    [brand_id]         bigint        NOT NULL,
    [registration_id]  bigint        NULL,
    [name]             nvarchar(255) NOT NULL,
    [slug]             nvarchar(255) NOT NULL,
    [store_type]       nvarchar(30)  NOT NULL,
    [logo]             nvarchar(255) NULL,
    [banner]           nvarchar(255) NULL,
    [description]      nvarchar(max) NULL,
    [phone]            nvarchar(20)  NULL,
    [email]            nvarchar(255) NULL,
    [pickup_address]   nvarchar(500) NULL,
    [return_policy]    nvarchar(max) NULL,
    [warranty_policy]  nvarchar(max) NULL,
    [shipping_policy]  nvarchar(max) NULL,
    [status]           nvarchar(30)  NOT NULL CONSTRAINT [DF_stores_status] DEFAULT N'PENDING_SETUP',
    [commission_rate]  decimal(5,2)  NULL,
    [is_verified]      bit           NOT NULL CONSTRAINT [DF_stores_verified] DEFAULT 0,
    [rating_avg]       decimal(3,2)  NOT NULL CONSTRAINT [DF_stores_rating] DEFAULT 0,
    [review_count]     int           NOT NULL CONSTRAINT [DF_stores_rcount] DEFAULT 0,
    [approved_at]      datetime2(0)  NULL,
    [approved_by]      bigint        NULL,
    [suspended_at]     datetime2(0)  NULL,
    [suspended_reason] nvarchar(500) NULL,
    [closed_at]        datetime2(0)  NULL,
    [created_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_stores_created] DEFAULT SYSDATETIME(),
    [updated_at]       datetime2(0)  NULL,
    CONSTRAINT [PK_stores] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_stores_slug] UNIQUE ([slug]),
    CONSTRAINT [FK_stores_business]     FOREIGN KEY ([business_id])     REFERENCES [dbo].[businesses] ([id]),
    CONSTRAINT [FK_stores_brand]        FOREIGN KEY ([brand_id])        REFERENCES [dbo].[brands] ([id]),
    CONSTRAINT [FK_stores_registration] FOREIGN KEY ([registration_id]) REFERENCES [dbo].[seller_registrations] ([id]),
    CONSTRAINT [FK_stores_approver]     FOREIGN KEY ([approved_by])     REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_stores_status] CHECK ([status] IN
        (N'PENDING_SETUP', N'ACTIVE', N'SUSPENDED', N'CLOSED')),
    CONSTRAINT [CK_stores_type] CHECK ([store_type] IN (N'OFFICIAL', N'DISTRIBUTOR', N'RETAILER')),
    CONSTRAINT [CK_stores_commission] CHECK ([commission_rate] IS NULL
        OR ([commission_rate] >= 0 AND [commission_rate] <= 100))
);
GO
CREATE INDEX [IX_stores_brand]    ON [dbo].[stores] ([brand_id]);
CREATE INDEX [IX_stores_business] ON [dbo].[stores] ([business_id]);
CREATE INDEX [IX_stores_status]   ON [dbo].[stores] ([status]);
GO

-- Khoá ngoại vòng: seller_registrations.created_store_id trỏ ngược về stores,
-- thêm sau khi cả hai bảng đã tồn tại.
ALTER TABLE [dbo].[seller_registrations]
    ADD CONSTRAINT [FK_sreg_created_store] FOREIGN KEY ([created_store_id]) REFERENCES [dbo].[stores] ([id]);
GO

-- ---------------------------------------------------------------------
-- store_members — mục 6.10. Thay cho shops.user_id unique của bản cũ.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[store_members] (
    [id]                    bigint       IDENTITY(1,1) NOT NULL,
    [store_id]              bigint       NOT NULL,
    [user_id]               bigint       NOT NULL,
    [member_role]           nvarchar(20) NOT NULL,
    [can_manage_products]   bit          NOT NULL CONSTRAINT [DF_sm_products]  DEFAULT 0,
    [can_handle_orders]     bit          NOT NULL CONSTRAINT [DF_sm_orders]    DEFAULT 0,
    [can_support_customers] bit          NOT NULL CONSTRAINT [DF_sm_support]   DEFAULT 0,
    [can_view_reports]      bit          NOT NULL CONSTRAINT [DF_sm_reports]   DEFAULT 0,
    [status]                nvarchar(20) NOT NULL CONSTRAINT [DF_sm_status]    DEFAULT N'ACTIVE',
    [invited_by]            bigint       NULL,
    [joined_at]             datetime2(0) NOT NULL CONSTRAINT [DF_sm_joined]    DEFAULT SYSDATETIME(),
    [revoked_at]            datetime2(0) NULL,
    CONSTRAINT [PK_store_members] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_store_members] UNIQUE ([store_id], [user_id]),
    CONSTRAINT [FK_sm_store]   FOREIGN KEY ([store_id])   REFERENCES [dbo].[stores] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_sm_user]    FOREIGN KEY ([user_id])    REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_sm_inviter] FOREIGN KEY ([invited_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_sm_role]   CHECK ([member_role] IN (N'OWNER', N'STAFF')),
    CONSTRAINT [CK_sm_status] CHECK ([status] IN (N'ACTIVE', N'REVOKED'))
);
GO
CREATE INDEX [IX_sm_user]  ON [dbo].[store_members] ([user_id]);
CREATE INDEX [IX_sm_store] ON [dbo].[store_members] ([store_id], [status]);
GO

PRINT N'02 — Xong nhóm doanh nghiệp, thương hiệu, gian hàng.';
GO
