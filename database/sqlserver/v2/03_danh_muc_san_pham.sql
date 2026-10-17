-- =====================================================================
-- NHÓM 3 — Danh mục, sản phẩm, thông số, ảnh, tồn kho
-- Đặc tả: mục 4.4, 5.2, 5.5, 6.4, 6.6, 7.6, BR-04, BR-07, BR-08, BR-09
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- categories — cây phân cấp qua parent_id
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[categories] (
    [id]          bigint        IDENTITY(1,1) NOT NULL,
    [parent_id]   bigint        NULL,
    [name]        nvarchar(255) NOT NULL,
    [slug]        nvarchar(255) NOT NULL,
    [description] nvarchar(500) NULL,
    [image]       nvarchar(255) NULL,
    [sort_order]  int           NOT NULL CONSTRAINT [DF_categories_sort] DEFAULT 0,
    [status]      nvarchar(20)  NOT NULL CONSTRAINT [DF_categories_status] DEFAULT N'ACTIVE',
    [created_at]  datetime2(0)  NOT NULL CONSTRAINT [DF_categories_created] DEFAULT SYSDATETIME(),
    [updated_at]  datetime2(0)  NULL,
    CONSTRAINT [PK_categories] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_categories_slug] UNIQUE ([slug]),
    -- Tự tham chiếu nên KHÔNG dùng cascade (SQL Server cấm cascade vòng)
    CONSTRAINT [FK_categories_parent] FOREIGN KEY ([parent_id]) REFERENCES [dbo].[categories] ([id]),
    CONSTRAINT [CK_categories_status] CHECK ([status] IN (N'ACTIVE', N'HIDDEN'))
);
GO
CREATE INDEX [IX_categories_parent] ON [dbo].[categories] ([parent_id]);
GO

-- ---------------------------------------------------------------------
-- products — BR-04: brand_id LUÔN bằng store.brand_id, backend tự gán
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[products] (
    [id]               bigint        IDENTITY(1,1) NOT NULL,
    [store_id]         bigint        NOT NULL,
    [brand_id]         bigint        NOT NULL,
    [category_id]      bigint        NOT NULL,
    [model_code]       nvarchar(100) NOT NULL,
    [name]             nvarchar(255) NOT NULL,
    [slug]             nvarchar(255) NULL,
    [description]      nvarchar(max) NULL,
    [highlights]       nvarchar(max) NULL,

    -- Giá (mục 6.5)
    [price]            decimal(12,0) NOT NULL,
    [original_price]   decimal(12,0) NULL,
    [promo_price]      decimal(12,0) NULL,
    [promo_start_at]   datetime2(0)  NULL,
    [promo_end_at]     datetime2(0)  NULL,

    -- Kiểm duyệt (mục 4.4, 7.6)
    [status]           nvarchar(30)  NOT NULL CONSTRAINT [DF_products_status] DEFAULT N'DRAFT',
    [reject_reason]    nvarchar(500) NULL,
    [submitted_at]     datetime2(0)  NULL,
    [approved_at]      datetime2(0)  NULL,
    [approved_by]      bigint        NULL,

    [warranty_months]  int           NOT NULL CONSTRAINT [DF_products_warranty] DEFAULT 12,
    [image]            nvarchar(255) NULL,
    [sold_count]       int           NOT NULL CONSTRAINT [DF_products_sold] DEFAULT 0,
    [view_count]       int           NOT NULL CONSTRAINT [DF_products_view] DEFAULT 0,
    [rating_avg]       decimal(3,2)  NOT NULL CONSTRAINT [DF_products_rating] DEFAULT 0,
    [review_count]     int           NOT NULL CONSTRAINT [DF_products_rcount] DEFAULT 0,

    -- Thông số dùng để LỌC (mục 5.2) và SO SÁNH (mục 5.5) -> phải là cột, không phải key/value
    [door_type]        nvarchar(20)  NULL,
    [capacity_kg]      decimal(4,1)  NULL,
    [spin_speed_rpm]   int           NULL,
    [is_inverter]      bit           NOT NULL CONSTRAINT [DF_products_inverter] DEFAULT 0,
    [energy_rating]    nvarchar(10)  NULL,
    [width_cm]         decimal(6,1)  NULL,
    [height_cm]        decimal(6,1)  NULL,
    [depth_cm]         decimal(6,1)  NULL,
    [weight_kg]        decimal(6,1)  NULL,
    [power_kwh_year]   decimal(8,2)  NULL,
    [water_liter_cycle] decimal(8,2) NULL,

    [created_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_products_created] DEFAULT SYSDATETIME(),
    [updated_at]       datetime2(0)  NULL,

    CONSTRAINT [PK_products] PRIMARY KEY ([id]),
    -- Mục 9.3: cảnh báo/giới hạn trùng model TRONG CÙNG một Store,
    -- nhưng cùng model ở Store khác thì cho phép.
    CONSTRAINT [UQ_products_store_model] UNIQUE ([store_id], [model_code]),
    CONSTRAINT [FK_products_store]    FOREIGN KEY ([store_id])    REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_products_brand]    FOREIGN KEY ([brand_id])    REFERENCES [dbo].[brands] ([id]),
    CONSTRAINT [FK_products_category] FOREIGN KEY ([category_id]) REFERENCES [dbo].[categories] ([id]),
    CONSTRAINT [FK_products_approver] FOREIGN KEY ([approved_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_products_status] CHECK ([status] IN
        (N'DRAFT', N'PENDING_REVIEW', N'ACTIVE', N'REJECTED', N'INACTIVE', N'SUSPENDED')),
    CONSTRAINT [CK_products_door] CHECK ([door_type] IS NULL OR [door_type] IN (N'cua_tren', N'cua_ngang')),
    -- BR-07: giá không âm, giá khuyến mãi phải thấp hơn giá bán
    CONSTRAINT [CK_products_price] CHECK ([price] >= 0),
    CONSTRAINT [CK_products_promo] CHECK ([promo_price] IS NULL OR [promo_price] <= [price]),
    CONSTRAINT [CK_products_promo_time] CHECK
        ([promo_start_at] IS NULL OR [promo_end_at] IS NULL OR [promo_start_at] < [promo_end_at])
);
GO
CREATE INDEX [IX_products_status_cat]   ON [dbo].[products] ([status], [category_id]);
CREATE INDEX [IX_products_status_brand] ON [dbo].[products] ([status], [brand_id]);
CREATE INDEX [IX_products_store_status] ON [dbo].[products] ([store_id], [status]);
CREATE INDEX [IX_products_capacity]     ON [dbo].[products] ([capacity_kg]);
CREATE INDEX [IX_products_price]        ON [dbo].[products] ([price]);
GO

-- BR-04 ở tầng dữ liệu: chặn cứng việc sản phẩm mang brand khác brand của gian hàng.
-- CHECK không tham chiếu được bảng khác nên dùng trigger phòng vệ;
-- tầng service vẫn phải kiểm tra trước để báo lỗi thân thiện.
CREATE TRIGGER [dbo].[TR_products_brand_khop_store]
ON [dbo].[products]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN [dbo].[stores] s ON s.[id] = i.[store_id]
        WHERE i.[brand_id] <> s.[brand_id]
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 51004, N'BR-04: san pham phai thuoc dung thuong hieu cua gian hang.', 1;
    END
END;
GO

-- ---------------------------------------------------------------------
-- product_specs — thông số mở rộng, phần không cần lọc
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[product_specs] (
    [id]         bigint        IDENTITY(1,1) NOT NULL,
    [product_id] bigint        NOT NULL,
    [spec_key]   nvarchar(100) NOT NULL,
    [spec_value] nvarchar(500) NOT NULL,
    [sort_order] int           NOT NULL CONSTRAINT [DF_pspecs_sort] DEFAULT 0,
    CONSTRAINT [PK_product_specs] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_product_specs] UNIQUE ([product_id], [spec_key]),
    CONSTRAINT [FK_pspecs_product] FOREIGN KEY ([product_id])
        REFERENCES [dbo].[products] ([id]) ON DELETE CASCADE
);
GO

-- ---------------------------------------------------------------------
-- product_images — mục 6.4: nhiều ảnh, sắp xếp được, chọn ảnh đại diện
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[product_images] (
    [id]         bigint        IDENTITY(1,1) NOT NULL,
    [product_id] bigint        NOT NULL,
    [path]       nvarchar(500) NOT NULL,
    [sort_order] int           NOT NULL CONSTRAINT [DF_pimg_sort] DEFAULT 0,
    [is_primary] bit           NOT NULL CONSTRAINT [DF_pimg_primary] DEFAULT 0,
    [created_at] datetime2(0)  NOT NULL CONSTRAINT [DF_pimg_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_product_images] PRIMARY KEY ([id]),
    CONSTRAINT [FK_pimg_product] FOREIGN KEY ([product_id])
        REFERENCES [dbo].[products] ([id]) ON DELETE CASCADE
);
GO
CREATE INDEX [IX_pimg_product] ON [dbo].[product_images] ([product_id], [sort_order]);
GO

-- ---------------------------------------------------------------------
-- inventory — BR-09. Quan hệ 1-1 với products.
-- Bán được khi  quantity_available - quantity_reserved >= số lượng đặt.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[inventory] (
    [product_id]         bigint       NOT NULL,
    [quantity_available] int          NOT NULL CONSTRAINT [DF_inv_avail] DEFAULT 0,
    [quantity_reserved]  int          NOT NULL CONSTRAINT [DF_inv_reserved] DEFAULT 0,
    [min_stock]          int          NOT NULL CONSTRAINT [DF_inv_min] DEFAULT 0,
    [cost_price]         bigint       NOT NULL CONSTRAINT [DF_inv_cost] DEFAULT 0,
    [updated_at]         datetime2(0) NULL,
    CONSTRAINT [PK_inventory] PRIMARY KEY ([product_id]),
    CONSTRAINT [FK_inventory_product] FOREIGN KEY ([product_id])
        REFERENCES [dbo].[products] ([id]) ON DELETE CASCADE,
    -- Không bao giờ để tồn âm hoặc giữ chỗ nhiều hơn tồn thực
    CONSTRAINT [CK_inv_khong_am]  CHECK ([quantity_available] >= 0 AND [quantity_reserved] >= 0),
    CONSTRAINT [CK_inv_giu_cho]   CHECK ([quantity_reserved] <= [quantity_available])
);
GO
CREATE INDEX [IX_inventory_thap] ON [dbo].[inventory] ([quantity_available]);
GO

-- ---------------------------------------------------------------------
-- wishlists — mục 5.9
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[wishlists] (
    [id]         bigint       IDENTITY(1,1) NOT NULL,
    [user_id]    bigint       NOT NULL,
    [product_id] bigint       NOT NULL,
    [created_at] datetime2(0) NOT NULL CONSTRAINT [DF_wishlist_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_wishlists] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_wishlists] UNIQUE ([user_id], [product_id]),
    CONSTRAINT [FK_wishlists_user]    FOREIGN KEY ([user_id])    REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_wishlists_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id])
);
GO

PRINT N'03 — Xong nhóm danh mục và sản phẩm.';
GO
