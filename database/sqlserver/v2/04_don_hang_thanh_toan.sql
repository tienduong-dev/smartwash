-- =====================================================================
-- NHÓM 4 + 5 — Giỏ hàng, đơn tổng, đơn con, thanh toán, vận chuyển
-- Đặc tả: mục 4.5, 5.7, 5.8, 6.7, 7.7, BR-10..BR-13, Phụ lục C.1, C.3, C.5
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- carts / cart_items — thay cột JSON của bản cũ
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[carts] (
    [id]         bigint       IDENTITY(1,1) NOT NULL,
    [user_id]    bigint       NOT NULL,
    [created_at] datetime2(0) NOT NULL CONSTRAINT [DF_carts_created] DEFAULT SYSDATETIME(),
    [updated_at] datetime2(0) NULL,
    CONSTRAINT [PK_carts] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_carts_user] UNIQUE ([user_id]),
    CONSTRAINT [FK_carts_user] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE
);
GO

CREATE TABLE [dbo].[cart_items] (
    [id]         bigint       IDENTITY(1,1) NOT NULL,
    [cart_id]    bigint       NOT NULL,
    [product_id] bigint       NOT NULL,
    [quantity]   int          NOT NULL CONSTRAINT [DF_cart_items_qty] DEFAULT 1,
    [added_at]   datetime2(0) NOT NULL CONSTRAINT [DF_cart_items_added] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_cart_items] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_cart_items] UNIQUE ([cart_id], [product_id]),
    CONSTRAINT [FK_cart_items_cart]    FOREIGN KEY ([cart_id])    REFERENCES [dbo].[carts] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_cart_items_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id]),
    CONSTRAINT [CK_cart_items_qty] CHECK ([quantity] > 0)
);
GO

-- ---------------------------------------------------------------------
-- orders — ĐƠN TỔNG (Phụ lục C.1)
-- Thông tin chung: khách, địa chỉ, tổng tiền, phương thức thanh toán.
-- Cột status và payment_status là GIÁ TRỊ SUY RA từ các đơn con / bảng payments,
-- không bao giờ sửa tay (xem mục 5 của TAI-LIEU/01-thiet-ke-du-lieu.md).
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[orders] (
    [id]                   bigint        IDENTITY(1,1) NOT NULL,
    [order_code]           nvarchar(20)  NOT NULL,
    [customer_id]          bigint        NOT NULL,

    [receiver_name]        nvarchar(255) NOT NULL,
    [receiver_phone]       nvarchar(20)  NOT NULL,
    [address_line]         nvarchar(500) NOT NULL,
    [province_id]          int           NULL,
    [province_name]        nvarchar(255) NULL,
    [district_id]          int           NULL,
    [district_name]        nvarchar(255) NULL,
    [ward_code]            nvarchar(30)  NULL,
    [ward_name]            nvarchar(255) NULL,
    [note]                 nvarchar(500) NULL,

    [items_subtotal]       decimal(14,0) NOT NULL CONSTRAINT [DF_orders_subtotal] DEFAULT 0,
    [platform_discount]    decimal(14,0) NOT NULL CONSTRAINT [DF_orders_pdisc]    DEFAULT 0,
    [store_discount_total] decimal(14,0) NOT NULL CONSTRAINT [DF_orders_sdisc]    DEFAULT 0,
    [shipping_fee_total]   decimal(14,0) NOT NULL CONSTRAINT [DF_orders_ship]     DEFAULT 0,
    [grand_total]          decimal(14,0) NOT NULL CONSTRAINT [DF_orders_total]    DEFAULT 0,
    [platform_coupon_code] nvarchar(50)  NULL,

    [payment_method]       nvarchar(20)  NOT NULL,
    [payment_status]       nvarchar(30)  NOT NULL CONSTRAINT [DF_orders_pstatus] DEFAULT N'UNPAID',
    [status]               nvarchar(30)  NOT NULL CONSTRAINT [DF_orders_status]  DEFAULT N'PENDING_CONFIRMATION',

    [placed_at]            datetime2(0)  NOT NULL CONSTRAINT [DF_orders_placed] DEFAULT SYSDATETIME(),
    [completed_at]         datetime2(0)  NULL,
    [cancelled_at]         datetime2(0)  NULL,
    [updated_at]           datetime2(0)  NULL,

    CONSTRAINT [PK_orders] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_orders_code] UNIQUE ([order_code]),
    CONSTRAINT [FK_orders_customer] FOREIGN KEY ([customer_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_orders_payment_method] CHECK ([payment_method] IN
        (N'COD', N'VNPAY', N'MOMO', N'BANK_DEMO')),
    CONSTRAINT [CK_orders_payment_status] CHECK ([payment_status] IN
        (N'UNPAID', N'PENDING', N'PAID', N'FAILED', N'REFUND_PENDING', N'PARTIALLY_REFUNDED', N'REFUNDED')),
    CONSTRAINT [CK_orders_status] CHECK ([status] IN
        (N'PENDING_CONFIRMATION', N'PROCESSING', N'SHIPPING', N'DELIVERED',
         N'COMPLETED', N'CANCELLED', N'RETURNED')),
    CONSTRAINT [CK_orders_tien] CHECK ([grand_total] >= 0 AND [items_subtotal] >= 0)
);
GO
CREATE INDEX [IX_orders_customer] ON [dbo].[orders] ([customer_id], [placed_at] DESC);
CREATE INDEX [IX_orders_status]   ON [dbo].[orders] ([status]);
GO

-- ---------------------------------------------------------------------
-- store_orders — ĐƠN CON theo từng gian hàng (BR-11)
-- Seller chỉ thấy và xử lý đơn con thuộc Store mình quản lý.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[store_orders] (
    [id]                          bigint        IDENTITY(1,1) NOT NULL,
    [store_order_code]            nvarchar(24)  NOT NULL,
    [order_id]                    bigint        NOT NULL,
    [store_id]                    bigint        NOT NULL,

    [items_subtotal]              decimal(14,0) NOT NULL CONSTRAINT [DF_so_subtotal] DEFAULT 0,
    [store_discount]              decimal(14,0) NOT NULL CONSTRAINT [DF_so_sdisc]    DEFAULT 0,
    [platform_discount_allocated] decimal(14,0) NOT NULL CONSTRAINT [DF_so_pdisc]    DEFAULT 0,
    [shipping_fee]                decimal(14,0) NOT NULL CONSTRAINT [DF_so_ship]     DEFAULT 0,
    [total_amount]                decimal(14,0) NOT NULL CONSTRAINT [DF_so_total]    DEFAULT 0,

    [commission_rate]             decimal(5,2)  NOT NULL CONSTRAINT [DF_so_crate]  DEFAULT 0,
    [commission_amount]           decimal(14,0) NOT NULL CONSTRAINT [DF_so_camt]   DEFAULT 0,
    [seller_amount]               decimal(14,0) NOT NULL CONSTRAINT [DF_so_samt]   DEFAULT 0,
    [store_coupon_code]           nvarchar(50)  NULL,

    [status]                      nvarchar(30)  NOT NULL CONSTRAINT [DF_so_status] DEFAULT N'PENDING_CONFIRMATION',
    [cancel_reason]               nvarchar(500) NULL,

    [confirmed_at]                datetime2(0)  NULL,
    [shipped_at]                  datetime2(0)  NULL,
    [delivered_at]                datetime2(0)  NULL,
    [completed_at]                datetime2(0)  NULL,
    [cancelled_at]                datetime2(0)  NULL,
    [created_at]                  datetime2(0)  NOT NULL CONSTRAINT [DF_so_created] DEFAULT SYSDATETIME(),
    [updated_at]                  datetime2(0)  NULL,

    CONSTRAINT [PK_store_orders] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_store_orders_code] UNIQUE ([store_order_code]),
    CONSTRAINT [FK_so_order] FOREIGN KEY ([order_id]) REFERENCES [dbo].[orders] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_so_store] FOREIGN KEY ([store_id]) REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [CK_so_status] CHECK ([status] IN
        (N'PENDING_CONFIRMATION', N'CONFIRMED', N'PROCESSING', N'SHIPPED', N'DELIVERED',
         N'COMPLETED', N'CANCEL_REQUESTED', N'CANCELLED', N'RETURN_REQUESTED', N'RETURNED')),
    CONSTRAINT [CK_so_tien] CHECK ([total_amount] >= 0 AND [commission_amount] >= 0)
);
GO
CREATE INDEX [IX_so_store_status] ON [dbo].[store_orders] ([store_id], [status]);
CREATE INDEX [IX_so_order]        ON [dbo].[store_orders] ([order_id]);
GO

-- ---------------------------------------------------------------------
-- order_items — BR-10: snapshot tại thời điểm mua.
-- Hiển thị lại đơn cũ LUÔN dùng các cột *_snapshot, không join sang products,
-- nên sửa giá hay ẩn sản phẩm không làm sai đơn đã đặt (kịch bản AT-10).
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[order_items] (
    [id]                       bigint        IDENTITY(1,1) NOT NULL,
    [store_order_id]           bigint        NOT NULL,
    [product_id]               bigint        NULL,
    [product_name_snapshot]    nvarchar(255) NOT NULL,
    [model_snapshot]           nvarchar(100) NULL,
    [brand_name_snapshot]      nvarchar(150) NULL,
    [image_snapshot]           nvarchar(500) NULL,
    [unit_price]               decimal(12,0) NOT NULL,
    [quantity]                 int           NOT NULL,
    [discount_allocated]       decimal(12,0) NOT NULL CONSTRAINT [DF_oi_disc] DEFAULT 0,
    [subtotal]                 decimal(14,0) NOT NULL,
    [warranty_months_snapshot] int           NOT NULL CONSTRAINT [DF_oi_warranty] DEFAULT 0,
    CONSTRAINT [PK_order_items] PRIMARY KEY ([id]),
    CONSTRAINT [FK_oi_store_order] FOREIGN KEY ([store_order_id])
        REFERENCES [dbo].[store_orders] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_oi_product] FOREIGN KEY ([product_id])
        REFERENCES [dbo].[products] ([id]) ON DELETE SET NULL,
    CONSTRAINT [CK_oi_qty] CHECK ([quantity] > 0),
    CONSTRAINT [CK_oi_price] CHECK ([unit_price] >= 0)
);
GO
CREATE INDEX [IX_oi_store_order] ON [dbo].[order_items] ([store_order_id]);
CREATE INDEX [IX_oi_product]     ON [dbo].[order_items] ([product_id]);
GO

-- ---------------------------------------------------------------------
-- order_status_history — mục 4.6 "mỗi lần đổi trạng thái phải lưu lịch sử"
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[order_status_history] (
    [id]             bigint        IDENTITY(1,1) NOT NULL,
    [store_order_id] bigint        NOT NULL,
    [from_status]    nvarchar(30)  NULL,
    [to_status]      nvarchar(30)  NOT NULL,
    [actor_user_id]  bigint        NULL,
    [note]           nvarchar(500) NULL,
    [created_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_osh_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_order_status_history] PRIMARY KEY ([id]),
    CONSTRAINT [FK_osh_store_order] FOREIGN KEY ([store_order_id])
        REFERENCES [dbo].[store_orders] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_osh_actor] FOREIGN KEY ([actor_user_id]) REFERENCES [dbo].[users] ([id])
);
GO
CREATE INDEX [IX_osh_store_order] ON [dbo].[order_status_history] ([store_order_id], [created_at]);
GO

-- ---------------------------------------------------------------------
-- payments — BR-12. Gắn vào ĐƠN TỔNG vì khách trả một lần cho cả giỏ.
-- Trạng thái thanh toán ĐỘC LẬP với trạng thái đơn.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[payments] (
    [id]              bigint        IDENTITY(1,1) NOT NULL,
    [order_id]        bigint        NOT NULL,
    [provider]        nvarchar(20)  NOT NULL,
    [transaction_ref] nvarchar(100) NULL,
    [idempotency_key] nvarchar(80)  NULL,
    [amount]          decimal(14,0) NOT NULL,
    [status]          nvarchar(30)  NOT NULL CONSTRAINT [DF_pay_status] DEFAULT N'PENDING',
    [paid_at]         datetime2(0)  NULL,
    [failed_at]       datetime2(0)  NULL,
    [fail_reason]     nvarchar(500) NULL,
    [raw_response]    nvarchar(max) NULL,
    [created_at]      datetime2(0)  NOT NULL CONSTRAINT [DF_pay_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_payments] PRIMARY KEY ([id]),
    CONSTRAINT [FK_payments_order] FOREIGN KEY ([order_id]) REFERENCES [dbo].[orders] ([id]) ON DELETE CASCADE,
    CONSTRAINT [CK_pay_provider] CHECK ([provider] IN (N'COD', N'VNPAY', N'MOMO', N'BANK_DEMO')),
    CONSTRAINT [CK_pay_status] CHECK ([status] IN
        (N'UNPAID', N'PENDING', N'PAID', N'FAILED', N'REFUND_PENDING', N'PARTIALLY_REFUNDED', N'REFUNDED'))
);
GO
CREATE INDEX [IX_payments_order] ON [dbo].[payments] ([order_id]);
CREATE INDEX [IX_payments_ref]   ON [dbo].[payments] ([transaction_ref]);
-- Chống webhook gửi lặp (mục 10.1): cùng một idempotency_key chỉ ghi nhận một lần
CREATE UNIQUE INDEX [UX_payments_idem] ON [dbo].[payments] ([idempotency_key])
    WHERE [idempotency_key] IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- refunds — BR-13
-- store_order_id để NO ACTION: nếu cascade thì có hai đường cascade
-- cùng xuất phát từ orders về refunds, SQL Server sẽ từ chối (lỗi 1785).
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[refunds] (
    [id]             bigint        IDENTITY(1,1) NOT NULL,
    [payment_id]     bigint        NOT NULL,
    [store_order_id] bigint        NULL,
    [amount]         decimal(14,0) NOT NULL,
    [reason]         nvarchar(500) NULL,
    [status]         nvarchar(20)  NOT NULL CONSTRAINT [DF_refund_status] DEFAULT N'PENDING',
    [requested_by]   bigint        NULL,
    [processed_by]   bigint        NULL,
    [processed_at]   datetime2(0)  NULL,
    [created_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_refund_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_refunds] PRIMARY KEY ([id]),
    CONSTRAINT [FK_refunds_payment]     FOREIGN KEY ([payment_id])     REFERENCES [dbo].[payments] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_refunds_store_order] FOREIGN KEY ([store_order_id]) REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [FK_refunds_requester]   FOREIGN KEY ([requested_by])   REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_refunds_processor]   FOREIGN KEY ([processed_by])   REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_refund_status] CHECK ([status] IN (N'PENDING', N'DONE', N'FAILED')),
    CONSTRAINT [CK_refund_amount] CHECK ([amount] > 0)
);
GO

-- ---------------------------------------------------------------------
-- shipments — Phụ lục C.5. GHN MÔ PHỎNG: mã vận đơn dạng DEMO-xxxxxxxx,
-- không gọi API thật, không tạo vận đơn thật.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[shipments] (
    [id]                   bigint        IDENTITY(1,1) NOT NULL,
    [store_order_id]       bigint        NOT NULL,
    [carrier]              nvarchar(30)  NOT NULL CONSTRAINT [DF_ship_carrier] DEFAULT N'GHN_MOCK',
    [tracking_code]        nvarchar(40)  NOT NULL,
    [service_name]         nvarchar(100) NULL,
    [fee]                  decimal(12,0) NOT NULL CONSTRAINT [DF_ship_fee] DEFAULT 0,
    [expected_delivery_at] datetime2(0)  NULL,
    [status]               nvarchar(30)  NOT NULL CONSTRAINT [DF_ship_status] DEFAULT N'CREATED',
    [shipped_at]           datetime2(0)  NULL,
    [delivered_at]         datetime2(0)  NULL,
    [created_at]           datetime2(0)  NOT NULL CONSTRAINT [DF_ship_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_shipments] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_shipments_tracking] UNIQUE ([tracking_code]),
    CONSTRAINT [FK_shipments_store_order] FOREIGN KEY ([store_order_id])
        REFERENCES [dbo].[store_orders] ([id]) ON DELETE CASCADE,
    CONSTRAINT [CK_ship_status] CHECK ([status] IN
        (N'CREATED', N'PICKED_UP', N'IN_TRANSIT', N'DELIVERED', N'FAILED', N'RETURNED'))
);
GO
CREATE INDEX [IX_shipments_store_order] ON [dbo].[shipments] ([store_order_id]);
GO

CREATE TABLE [dbo].[shipment_events] (
    [id]          bigint        IDENTITY(1,1) NOT NULL,
    [shipment_id] bigint        NOT NULL,
    [status]      nvarchar(30)  NOT NULL,
    [description] nvarchar(500) NULL,
    [location]    nvarchar(255) NULL,
    [happened_at] datetime2(0)  NOT NULL CONSTRAINT [DF_sev_happened] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_shipment_events] PRIMARY KEY ([id]),
    CONSTRAINT [FK_sev_shipment] FOREIGN KEY ([shipment_id])
        REFERENCES [dbo].[shipments] ([id]) ON DELETE CASCADE
);
GO
CREATE INDEX [IX_sev_shipment] ON [dbo].[shipment_events] ([shipment_id], [happened_at]);
GO

PRINT N'04 — Xong nhóm đơn hàng, thanh toán, vận chuyển.';
GO
