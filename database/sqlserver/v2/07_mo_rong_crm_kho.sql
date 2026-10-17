-- =====================================================================
-- NHÓM 9 — Phần mở rộng ngoài đặc tả: CRM và kho nhập/xuất
-- Giữ lại từ bản trước vì đã làm xong và là điểm cộng.
-- Khác bản cũ: thẻ kho ghi vào `inventory` thay cho `products.stock`.
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- CRM — khách hàng tiềm năng và lịch sử chăm sóc
-- (crm_tickets / crm_ticket_messages của bản cũ đã gộp vào tickets)
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[crm_leads] (
    [id]                bigint         IDENTITY(1,1) NOT NULL,
    [name]              nvarchar(255)  NOT NULL,
    [phone]             nvarchar(20)   NOT NULL,
    [email]             nvarchar(255)  NULL,
    [source]            nvarchar(30)   NOT NULL CONSTRAINT [DF_lead_source] DEFAULT N'website',
    [status]            nvarchar(30)   NOT NULL CONSTRAINT [DF_lead_status] DEFAULT N'moi',
    [product_id]        bigint         NULL,
    [store_id]          bigint         NULL,
    [assigned_to]       bigint         NULL,
    [budget]            decimal(14,0)  NULL,
    [note]              nvarchar(1000) NULL,
    [lost_reason]       nvarchar(500)  NULL,
    [next_follow_up_at] datetime2(0)   NULL,
    [closed_at]         datetime2(0)   NULL,
    [created_at]        datetime2(0)   NOT NULL CONSTRAINT [DF_lead_created] DEFAULT SYSDATETIME(),
    [updated_at]        datetime2(0)   NULL,
    CONSTRAINT [PK_crm_leads] PRIMARY KEY ([id]),
    CONSTRAINT [FK_lead_product]  FOREIGN KEY ([product_id])  REFERENCES [dbo].[products] ([id]) ON DELETE SET NULL,
    CONSTRAINT [FK_lead_store]    FOREIGN KEY ([store_id])    REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_lead_assignee] FOREIGN KEY ([assigned_to]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_lead_status] CHECK ([status] IN
        (N'moi', N'dang_cham_soc', N'da_chot', N'that_bai'))
);
GO
CREATE INDEX [IX_lead_status]   ON [dbo].[crm_leads] ([status]);
CREATE INDEX [IX_lead_followup] ON [dbo].[crm_leads] ([next_follow_up_at]);
GO

CREATE TABLE [dbo].[crm_activities] (
    [id]          bigint         IDENTITY(1,1) NOT NULL,
    [lead_id]     bigint         NULL,
    [customer_id] bigint         NULL,
    [type]        nvarchar(30)   NOT NULL CONSTRAINT [DF_act_type] DEFAULT N'goi_dien',
    [content]     nvarchar(1000) NOT NULL,
    [result]      nvarchar(500)  NULL,
    [remind_at]   datetime2(0)   NULL,
    [done]        bit            NOT NULL CONSTRAINT [DF_act_done] DEFAULT 0,
    [created_by]  bigint         NULL,
    [created_at]  datetime2(0)   NOT NULL CONSTRAINT [DF_act_created] DEFAULT SYSDATETIME(),
    [updated_at]  datetime2(0)   NULL,
    CONSTRAINT [PK_crm_activities] PRIMARY KEY ([id]),
    CONSTRAINT [FK_act_lead]     FOREIGN KEY ([lead_id])     REFERENCES [dbo].[crm_leads] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_act_customer] FOREIGN KEY ([customer_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_act_creator]  FOREIGN KEY ([created_by])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_act_type] CHECK ([type] IN (N'goi_dien', N'nhan_tin', N'email', N'gap_mat', N'ghi_chu'))
);
GO
CREATE INDEX [IX_act_lead]   ON [dbo].[crm_activities] ([lead_id]);
CREATE INDEX [IX_act_remind] ON [dbo].[crm_activities] ([remind_at], [done]);
GO

-- ---------------------------------------------------------------------
-- KHO — nhà cung cấp, phiếu nhập, phiếu xuất, thẻ kho
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[suppliers] (
    [id]         bigint        IDENTITY(1,1) NOT NULL,
    [store_id]   bigint        NOT NULL,
    [name]       nvarchar(255) NOT NULL,
    [phone]      nvarchar(20)  NULL,
    [email]      nvarchar(255) NULL,
    [address]    nvarchar(500) NULL,
    [tax_code]   nvarchar(20)  NULL,
    [note]       nvarchar(500) NULL,
    [active]     bit           NOT NULL CONSTRAINT [DF_sup_active] DEFAULT 1,
    [created_at] datetime2(0)  NOT NULL CONSTRAINT [DF_sup_created] DEFAULT SYSDATETIME(),
    [updated_at] datetime2(0)  NULL,
    CONSTRAINT [PK_suppliers] PRIMARY KEY ([id]),
    CONSTRAINT [FK_sup_store] FOREIGN KEY ([store_id]) REFERENCES [dbo].[stores] ([id]) ON DELETE CASCADE
);
GO
CREATE INDEX [IX_sup_store] ON [dbo].[suppliers] ([store_id]);
GO

CREATE TABLE [dbo].[stock_receipts] (
    [id]             bigint        IDENTITY(1,1) NOT NULL,
    [code]           nvarchar(20)  NOT NULL,
    [store_id]       bigint        NOT NULL,
    [supplier_id]    bigint        NULL,
    [status]         nvarchar(20)  NOT NULL CONSTRAINT [DF_sr_status] DEFAULT N'hoan_thanh',
    [total_quantity] int           NOT NULL CONSTRAINT [DF_sr_qty] DEFAULT 0,
    [total_amount]   decimal(14,0) NOT NULL CONSTRAINT [DF_sr_amount] DEFAULT 0,
    [note]           nvarchar(500) NULL,
    [created_by]     bigint        NULL,
    [received_at]    datetime2(0)  NULL,
    [created_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_sr_created] DEFAULT SYSDATETIME(),
    [updated_at]     datetime2(0)  NULL,
    CONSTRAINT [PK_stock_receipts] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_stock_receipts_code] UNIQUE ([code]),
    CONSTRAINT [FK_sr_store]    FOREIGN KEY ([store_id])    REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_sr_supplier] FOREIGN KEY ([supplier_id]) REFERENCES [dbo].[suppliers] ([id]),
    CONSTRAINT [FK_sr_creator]  FOREIGN KEY ([created_by])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_sr_status] CHECK ([status] IN (N'nhap', N'hoan_thanh', N'da_huy'))
);
GO
CREATE INDEX [IX_sr_store] ON [dbo].[stock_receipts] ([store_id]);
GO

CREATE TABLE [dbo].[stock_receipt_items] (
    [id]           bigint        IDENTITY(1,1) NOT NULL,
    [receipt_id]   bigint        NOT NULL,
    [product_id]   bigint        NULL,
    [product_name] nvarchar(255) NOT NULL,
    [quantity]     int           NOT NULL,
    [cost_price]   bigint        NOT NULL,
    [subtotal]     decimal(14,0) NOT NULL,
    CONSTRAINT [PK_stock_receipt_items] PRIMARY KEY ([id]),
    CONSTRAINT [FK_sri_receipt] FOREIGN KEY ([receipt_id]) REFERENCES [dbo].[stock_receipts] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_sri_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id]) ON DELETE SET NULL,
    CONSTRAINT [CK_sri_qty] CHECK ([quantity] > 0)
);
GO
CREATE INDEX [IX_sri_receipt] ON [dbo].[stock_receipt_items] ([receipt_id]);
GO

CREATE TABLE [dbo].[stock_issues] (
    [id]             bigint        IDENTITY(1,1) NOT NULL,
    [code]           nvarchar(20)  NOT NULL,
    [store_id]       bigint        NOT NULL,
    [supplier_id]    bigint        NULL,
    [type]           nvarchar(30)  NOT NULL CONSTRAINT [DF_si_type] DEFAULT N'khac',
    [status]         nvarchar(20)  NOT NULL CONSTRAINT [DF_si_status] DEFAULT N'hoan_thanh',
    [reason]         nvarchar(500) NULL,
    [total_quantity] int           NOT NULL CONSTRAINT [DF_si_qty] DEFAULT 0,
    [total_amount]   decimal(14,0) NOT NULL CONSTRAINT [DF_si_amount] DEFAULT 0,
    [created_by]     bigint        NULL,
    [issued_at]      datetime2(0)  NULL,
    [created_at]     datetime2(0)  NOT NULL CONSTRAINT [DF_si_created] DEFAULT SYSDATETIME(),
    [updated_at]     datetime2(0)  NULL,
    CONSTRAINT [PK_stock_issues] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_stock_issues_code] UNIQUE ([code]),
    CONSTRAINT [FK_si_store]    FOREIGN KEY ([store_id])    REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_si_supplier] FOREIGN KEY ([supplier_id]) REFERENCES [dbo].[suppliers] ([id]),
    CONSTRAINT [FK_si_creator]  FOREIGN KEY ([created_by])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_si_type] CHECK ([type] IN (N'hu_hong', N'tra_ncc', N'khac'))
);
GO
CREATE INDEX [IX_si_store] ON [dbo].[stock_issues] ([store_id]);
GO

CREATE TABLE [dbo].[stock_issue_items] (
    [id]           bigint        IDENTITY(1,1) NOT NULL,
    [issue_id]     bigint        NOT NULL,
    [product_id]   bigint        NULL,
    [product_name] nvarchar(255) NOT NULL,
    [quantity]     int           NOT NULL,
    [cost_price]   bigint        NOT NULL,
    [subtotal]     decimal(14,0) NOT NULL,
    CONSTRAINT [PK_stock_issue_items] PRIMARY KEY ([id]),
    CONSTRAINT [FK_sii_issue]   FOREIGN KEY ([issue_id])   REFERENCES [dbo].[stock_issues] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_sii_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id]) ON DELETE SET NULL,
    CONSTRAINT [CK_sii_qty] CHECK ([quantity] > 0)
);
GO
CREATE INDEX [IX_sii_issue] ON [dbo].[stock_issue_items] ([issue_id]);
GO

-- Thẻ kho: mọi biến động tồn đều ghi một dòng ở đây.
-- Tiền tố tên ràng buộc là smov_ (không phải sm_) vì sm_ đã dùng cho store_members;
-- trong SQL Server tên ràng buộc là duy nhất trên TOÀN database, không theo từng bảng.
-- Cộng dồn `quantity` của một sản phẩm phải đúng bằng inventory.quantity_available.
CREATE TABLE [dbo].[stock_movements] (
    [id]            bigint        IDENTITY(1,1) NOT NULL,
    [store_id]      bigint        NULL,
    [product_id]    bigint        NULL,
    [product_name]  nvarchar(255) NOT NULL,
    [type]          nvarchar(30)  NOT NULL,
    [quantity]      int           NOT NULL,
    [stock_before]  int           NOT NULL,
    [stock_after]   int           NOT NULL,
    [unit_cost]     bigint        NOT NULL CONSTRAINT [DF_smov_cost] DEFAULT 0,
    [ref_type]      nvarchar(30)  NULL,
    [ref_id]        bigint        NULL,
    [ref_code]      nvarchar(40)  NULL,
    [note]          nvarchar(500) NULL,
    [created_by]    bigint        NULL,
    [created_at]    datetime2(0)  NOT NULL CONSTRAINT [DF_smov_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_stock_movements] PRIMARY KEY ([id]),
    CONSTRAINT [FK_smov_store]   FOREIGN KEY ([store_id])   REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_smov_product] FOREIGN KEY ([product_id]) REFERENCES [dbo].[products] ([id]) ON DELETE SET NULL,
    CONSTRAINT [FK_smov_creator] FOREIGN KEY ([created_by]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_smov_type] CHECK ([type] IN
        (N'nhap', N'xuat_ban', N'hoan_ban', N'xuat_huy', N'tra_ncc', N'dieu_chinh', N'huy_phieu_nhap'))
);
GO
CREATE INDEX [IX_smov_product] ON [dbo].[stock_movements] ([product_id], [created_at]);
CREATE INDEX [IX_smov_store]   ON [dbo].[stock_movements] ([store_id], [created_at]);
CREATE INDEX [IX_smov_type]    ON [dbo].[stock_movements] ([type]);
GO

PRINT N'07 — Xong nhóm mở rộng CRM và kho.';
GO
PRINT N'=== Đã tạo xong toàn bộ bảng. Chạy tiếp các file dữ liệu mẫu. ===';
GO
