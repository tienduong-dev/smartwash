-- =====================================================================
-- NHÓM 6 + 7 — Khuyến mãi, phí sàn, đối soát
-- Đặc tả: mục 6.5, 7.8, 7.9, Phụ lục C.2, C.7
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- coupons — 4 phạm vi: toàn sàn / theo thương hiệu / theo gian hàng / theo sản phẩm
-- cost_bearer quyết định ai chịu chi phí khuyến mãi khi đối soát (mục 7.9)
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[coupons] (
    [id]                   bigint        IDENTITY(1,1) NOT NULL,
    [code]                 nvarchar(50)  NOT NULL,
    [name]                 nvarchar(255) NULL,
    [scope]                nvarchar(20)  NOT NULL,
    [store_id]             bigint        NULL,
    [brand_id]             bigint        NULL,
    [discount_type]        nvarchar(20)  NOT NULL,
    [value]                decimal(12,2) NOT NULL,
    [max_discount]         decimal(12,0) NULL,
    [min_order_amount]     decimal(12,0) NOT NULL CONSTRAINT [DF_coupon_min] DEFAULT 0,
    [budget_amount]        decimal(14,0) NULL,
    [used_amount]          decimal(14,0) NOT NULL CONSTRAINT [DF_coupon_usedamt] DEFAULT 0,
    [usage_limit]          int           NULL,
    [usage_limit_per_user] int           NULL,
    [used_count]           int           NOT NULL CONSTRAINT [DF_coupon_usedcnt] DEFAULT 0,
    [new_customer_only]    bit           NOT NULL CONSTRAINT [DF_coupon_newonly] DEFAULT 0,
    [cost_bearer]          nvarchar(20)  NOT NULL CONSTRAINT [DF_coupon_bearer] DEFAULT N'PLATFORM',
    [start_at]             datetime2(0)  NULL,
    [end_at]               datetime2(0)  NULL,
    [status]               nvarchar(20)  NOT NULL CONSTRAINT [DF_coupon_status] DEFAULT N'ACTIVE',
    [created_by]           bigint        NULL,
    [created_at]           datetime2(0)  NOT NULL CONSTRAINT [DF_coupon_created] DEFAULT SYSDATETIME(),
    [updated_at]           datetime2(0)  NULL,
    CONSTRAINT [PK_coupons] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_coupons_code] UNIQUE ([code]),
    CONSTRAINT [FK_coupons_store]   FOREIGN KEY ([store_id])   REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_coupons_brand]   FOREIGN KEY ([brand_id])   REFERENCES [dbo].[brands] ([id]),
    CONSTRAINT [FK_coupons_creator] FOREIGN KEY ([created_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_coupons_scope] CHECK ([scope] IN (N'PLATFORM', N'BRAND', N'STORE', N'PRODUCT')),
    CONSTRAINT [CK_coupons_type]  CHECK ([discount_type] IN (N'PERCENT', N'FIXED')),
    CONSTRAINT [CK_coupons_bearer] CHECK ([cost_bearer] IN (N'PLATFORM', N'STORE')),
    CONSTRAINT [CK_coupons_status] CHECK ([status] IN (N'ACTIVE', N'PAUSED', N'EXPIRED')),
    -- Phạm vi nào thì bắt buộc có khoá tương ứng
    CONSTRAINT [CK_coupons_pham_vi] CHECK (
        ([scope] = N'STORE' AND [store_id] IS NOT NULL) OR
        ([scope] = N'BRAND' AND [brand_id] IS NOT NULL) OR
        ([scope] IN (N'PLATFORM', N'PRODUCT'))
    ),
    CONSTRAINT [CK_coupons_value] CHECK ([value] > 0),
    -- Giảm theo phần trăm thì không được quá 100
    CONSTRAINT [CK_coupons_percent] CHECK ([discount_type] <> N'PERCENT' OR [value] <= 100),
    CONSTRAINT [CK_coupons_thoi_gian] CHECK ([start_at] IS NULL OR [end_at] IS NULL OR [start_at] < [end_at])
);
GO
CREATE INDEX [IX_coupons_scope]  ON [dbo].[coupons] ([scope], [status]);
CREATE INDEX [IX_coupons_store]  ON [dbo].[coupons] ([store_id]);
GO

-- Chỉ dùng khi scope = PRODUCT
CREATE TABLE [dbo].[coupon_products] (
    [coupon_id]  bigint NOT NULL,
    [product_id] bigint NOT NULL,
    CONSTRAINT [PK_coupon_products] PRIMARY KEY ([coupon_id], [product_id]),
    CONSTRAINT [FK_cp_coupon]  FOREIGN KEY ([coupon_id])  REFERENCES [dbo].[coupons] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_cp_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id]) ON DELETE CASCADE
);
GO

-- ---------------------------------------------------------------------
-- coupon_redemptions — Phụ lục C.2
-- Vòng đời: RESERVED (giữ lượt lúc bắt đầu checkout)
--        -> USED     (đặt hàng thành công)
--        -> RELEASED (checkout thất bại hoặc huỷ đơn -> trả lượt về)
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[coupon_redemptions] (
    [id]              bigint        IDENTITY(1,1) NOT NULL,
    [coupon_id]       bigint        NOT NULL,
    [user_id]         bigint        NOT NULL,
    [order_id]        bigint        NULL,
    [store_order_id]  bigint        NULL,
    [discount_amount] decimal(14,0) NOT NULL CONSTRAINT [DF_cr_amount] DEFAULT 0,
    [status]          nvarchar(20)  NOT NULL CONSTRAINT [DF_cr_status] DEFAULT N'RESERVED',
    [reserved_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_cr_reserved] DEFAULT SYSDATETIME(),
    [used_at]         datetime2(0)  NULL,
    [released_at]     datetime2(0)  NULL,
    CONSTRAINT [PK_coupon_redemptions] PRIMARY KEY ([id]),
    CONSTRAINT [FK_cr_coupon]      FOREIGN KEY ([coupon_id])      REFERENCES [dbo].[coupons] ([id]),
    CONSTRAINT [FK_cr_user]        FOREIGN KEY ([user_id])        REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_cr_order]       FOREIGN KEY ([order_id])       REFERENCES [dbo].[orders] ([id]),
    CONSTRAINT [FK_cr_store_order] FOREIGN KEY ([store_order_id]) REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [CK_cr_status] CHECK ([status] IN (N'RESERVED', N'USED', N'RELEASED'))
);
GO
-- Kiểm tra nhanh giới hạn lượt dùng trên mỗi khách
CREATE INDEX [IX_cr_coupon_user] ON [dbo].[coupon_redemptions] ([coupon_id], [user_id], [status]);
CREATE INDEX [IX_cr_order]       ON [dbo].[coupon_redemptions] ([order_id]);
GO

-- ---------------------------------------------------------------------
-- commission_transactions — Phụ lục D:
--   FEE      ghi khi đơn con GIAO THÀNH CÔNG
--   REVERSAL đảo lại khi hoàn tiền
--   ADJUSTMENT điều chỉnh thủ công của Admin, bắt buộc có ghi chú
-- Đơn huỷ KHÔNG sinh bút toán nào.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[commission_transactions] (
    [id]             bigint        IDENTITY(1,1) NOT NULL,
    [store_order_id] bigint        NULL,
    [store_id]       bigint        NOT NULL,
    [type]           nvarchar(20)  NOT NULL,
    [base_amount]    decimal(14,0) NOT NULL CONSTRAINT [DF_ct_base] DEFAULT 0,
    [rate]           decimal(5,2)  NOT NULL CONSTRAINT [DF_ct_rate] DEFAULT 0,
    [amount]         decimal(14,0) NOT NULL,
    [settlement_id]  bigint        NULL,
    [note]           nvarchar(500) NULL,
    [created_by]     bigint        NULL,
    [recorded_at]    datetime2(0)  NOT NULL CONSTRAINT [DF_ct_recorded] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_commission_transactions] PRIMARY KEY ([id]),
    CONSTRAINT [FK_ct_store_order] FOREIGN KEY ([store_order_id]) REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [FK_ct_store]       FOREIGN KEY ([store_id])       REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_ct_creator]     FOREIGN KEY ([created_by])     REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_ct_type] CHECK ([type] IN (N'FEE', N'REVERSAL', N'ADJUSTMENT')),
    CONSTRAINT [CK_ct_adjust_note] CHECK ([type] <> N'ADJUSTMENT' OR [note] IS NOT NULL)
);
GO
CREATE INDEX [IX_ct_store]    ON [dbo].[commission_transactions] ([store_id], [recorded_at]);
CREATE INDEX [IX_ct_so]       ON [dbo].[commission_transactions] ([store_order_id]);
GO

-- ---------------------------------------------------------------------
-- settlements — kỳ đối soát (mục 7.8)
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[settlements] (
    [id]               bigint        IDENTITY(1,1) NOT NULL,
    [store_id]         bigint        NOT NULL,
    [period_start]     date          NOT NULL,
    [period_end]       date          NOT NULL,
    [gross_sales]      decimal(14,0) NOT NULL CONSTRAINT [DF_st_gross] DEFAULT 0,
    [commission_total] decimal(14,0) NOT NULL CONSTRAINT [DF_st_comm]  DEFAULT 0,
    [adjustment_total] decimal(14,0) NOT NULL CONSTRAINT [DF_st_adj]   DEFAULT 0,
    [payable_amount]   decimal(14,0) NOT NULL CONSTRAINT [DF_st_pay]   DEFAULT 0,
    [status]           nvarchar(20)  NOT NULL CONSTRAINT [DF_st_status] DEFAULT N'OPEN',
    [closed_at]        datetime2(0)  NULL,
    [closed_by]        bigint        NULL,
    [paid_at]          datetime2(0)  NULL,
    [note]             nvarchar(500) NULL,
    [created_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_st_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_settlements] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_settlements_ky] UNIQUE ([store_id], [period_start], [period_end]),
    CONSTRAINT [FK_st_store]  FOREIGN KEY ([store_id])  REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_st_closer] FOREIGN KEY ([closed_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_st_status] CHECK ([status] IN (N'OPEN', N'CLOSED', N'PAID')),
    CONSTRAINT [CK_st_ky] CHECK ([period_start] <= [period_end])
);
GO

ALTER TABLE [dbo].[commission_transactions]
    ADD CONSTRAINT [FK_ct_settlement] FOREIGN KEY ([settlement_id]) REFERENCES [dbo].[settlements] ([id]);
GO

PRINT N'05 — Xong nhóm khuyến mãi và phí sàn.';
GO
