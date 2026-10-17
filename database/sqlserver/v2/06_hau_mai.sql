-- =====================================================================
-- NHÓM 8 — Đánh giá, phiếu hỗ trợ, đổi trả, bảo hành, tranh chấp
-- Đặc tả: mục 4.6, 5.9, 6.8, 7.10, BR-14, Phụ lục C.8
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- reviews — BR-14 / kịch bản AT-13
-- order_item_id là NOT NULL + UNIQUE: không mua thì không có dòng hàng
-- để gắn vào, nên đánh giá khống bị chặn ngay ở tầng dữ liệu.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[reviews] (
    [id]                bigint        IDENTITY(1,1) NOT NULL,
    [order_item_id]     bigint        NOT NULL,
    [product_id]        bigint        NOT NULL,
    [store_id]          bigint        NOT NULL,
    [customer_id]       bigint        NOT NULL,
    [rating]            tinyint       NOT NULL,
    [content]           nvarchar(max) NULL,
    [images]            nvarchar(max) NULL,
    [status]            nvarchar(20)  NOT NULL CONSTRAINT [DF_reviews_status] DEFAULT N'VISIBLE',
    [hidden_reason]     nvarchar(500) NULL,
    [seller_reply]      nvarchar(max) NULL,
    [seller_replied_at] datetime2(0)  NULL,
    [created_at]        datetime2(0)  NOT NULL CONSTRAINT [DF_reviews_created] DEFAULT SYSDATETIME(),
    [updated_at]        datetime2(0)  NULL,
    CONSTRAINT [PK_reviews] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_reviews_order_item] UNIQUE ([order_item_id]),
    CONSTRAINT [FK_reviews_order_item] FOREIGN KEY ([order_item_id]) REFERENCES [dbo].[order_items] ([id]),
    CONSTRAINT [FK_reviews_product]    FOREIGN KEY ([product_id])    REFERENCES [dbo].[products] ([id]),
    CONSTRAINT [FK_reviews_store]      FOREIGN KEY ([store_id])      REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_reviews_customer]   FOREIGN KEY ([customer_id])   REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_reviews_rating] CHECK ([rating] BETWEEN 1 AND 5),
    CONSTRAINT [CK_reviews_status] CHECK ([status] IN (N'VISIBLE', N'HIDDEN', N'REPORTED'))
);
GO
CREATE INDEX [IX_reviews_product] ON [dbo].[reviews] ([product_id], [status]);
CREATE INDEX [IX_reviews_store]   ON [dbo].[reviews] ([store_id], [status]);
GO

-- ---------------------------------------------------------------------
-- review_reports — mục 7.10. Admin KHÔNG sửa nội dung đánh giá,
-- chỉ ẩn và ghi lý do.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[review_reports] (
    [id]               bigint        IDENTITY(1,1) NOT NULL,
    [review_id]        bigint        NOT NULL,
    [reporter_user_id] bigint        NOT NULL,
    [reason]           nvarchar(500) NOT NULL,
    [status]           nvarchar(20)  NOT NULL CONSTRAINT [DF_rr_status] DEFAULT N'OPEN',
    [handled_by]       bigint        NULL,
    [handled_at]       datetime2(0)  NULL,
    [created_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_rr_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_review_reports] PRIMARY KEY ([id]),
    CONSTRAINT [FK_rr_review]   FOREIGN KEY ([review_id])        REFERENCES [dbo].[reviews] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_rr_reporter] FOREIGN KEY ([reporter_user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_rr_handler]  FOREIGN KEY ([handled_by])       REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_rr_status] CHECK ([status] IN (N'OPEN', N'RESOLVED', N'DISMISSED'))
);
GO

-- ---------------------------------------------------------------------
-- tickets — mục 5.9, 6.8, 7.10
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[tickets] (
    [id]               bigint        IDENTITY(1,1) NOT NULL,
    [code]             nvarchar(20)  NOT NULL,
    [customer_id]      bigint        NULL,
    [store_id]         bigint        NULL,
    [order_id]         bigint        NULL,
    [store_order_id]   bigint        NULL,
    [name]             nvarchar(255) NOT NULL,
    [email]            nvarchar(255) NULL,
    [phone]            nvarchar(20)  NULL,
    [subject]          nvarchar(255) NOT NULL,
    [content]          nvarchar(max) NOT NULL,
    [category]         nvarchar(40)  NOT NULL,
    [priority]         nvarchar(20)  NOT NULL CONSTRAINT [DF_tickets_priority] DEFAULT N'NORMAL',
    [status]           nvarchar(30)  NOT NULL CONSTRAINT [DF_tickets_status]   DEFAULT N'OPEN',
    [assigned_to]      bigint        NULL,
    [first_replied_at] datetime2(0)  NULL,
    [resolved_at]      datetime2(0)  NULL,
    [closed_at]        datetime2(0)  NULL,
    [created_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_tickets_created] DEFAULT SYSDATETIME(),
    [updated_at]       datetime2(0)  NULL,
    CONSTRAINT [PK_tickets] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_tickets_code] UNIQUE ([code]),
    CONSTRAINT [FK_tickets_customer]    FOREIGN KEY ([customer_id])    REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_tickets_store]       FOREIGN KEY ([store_id])       REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_tickets_order]       FOREIGN KEY ([order_id])       REFERENCES [dbo].[orders] ([id]),
    CONSTRAINT [FK_tickets_store_order] FOREIGN KEY ([store_order_id]) REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [FK_tickets_assignee]    FOREIGN KEY ([assigned_to])    REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_tickets_priority] CHECK ([priority] IN (N'LOW', N'NORMAL', N'HIGH', N'URGENT')),
    CONSTRAINT [CK_tickets_status] CHECK ([status] IN
        (N'OPEN', N'ASSIGNED', N'IN_PROGRESS', N'WAITING_CUSTOMER', N'RESOLVED', N'CLOSED'))
);
GO
CREATE INDEX [IX_tickets_status]   ON [dbo].[tickets] ([status], [created_at]);
CREATE INDEX [IX_tickets_store]    ON [dbo].[tickets] ([store_id], [status]);
CREATE INDEX [IX_tickets_customer] ON [dbo].[tickets] ([customer_id]);
GO

CREATE TABLE [dbo].[ticket_messages] (
    [id]          bigint        IDENTITY(1,1) NOT NULL,
    [ticket_id]   bigint        NOT NULL,
    [user_id]     bigint        NULL,
    [sender_role] nvarchar(20)  NOT NULL,
    [sender_name] nvarchar(255) NULL,
    [message]     nvarchar(max) NOT NULL,
    [attachment]  nvarchar(500) NULL,
    [created_at]  datetime2(0)  NOT NULL CONSTRAINT [DF_tm_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_ticket_messages] PRIMARY KEY ([id]),
    CONSTRAINT [FK_tm_ticket] FOREIGN KEY ([ticket_id]) REFERENCES [dbo].[tickets] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_tm_user]   FOREIGN KEY ([user_id])   REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_tm_role] CHECK ([sender_role] IN (N'CUSTOMER', N'SELLER', N'ADMIN'))
);
GO
CREATE INDEX [IX_tm_ticket] ON [dbo].[ticket_messages] ([ticket_id], [created_at]);
GO

-- ---------------------------------------------------------------------
-- return_requests — Phụ lục C.8.
-- Chỉ tạo được khi đơn con đã DELIVERED và trong hạn đổi trả
-- (settings.return_window_days, mặc định 7 ngày) — kiểm tra ở tầng service.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[return_requests] (
    [id]              bigint         IDENTITY(1,1) NOT NULL,
    [code]            nvarchar(20)   NOT NULL,
    [store_order_id]  bigint         NOT NULL,
    [order_item_id]   bigint         NOT NULL,
    [customer_id]     bigint         NOT NULL,
    [type]            nvarchar(20)   NOT NULL,
    [reason_code]     nvarchar(40)   NOT NULL,
    [description]     nvarchar(1000) NULL,
    [status]          nvarchar(30)   NOT NULL CONSTRAINT [DF_ret_status] DEFAULT N'REQUESTED',
    [refund_amount]   decimal(14,0)  NULL,
    [resolution_note] nvarchar(1000) NULL,
    [handled_by]      bigint         NULL,
    [requested_at]    datetime2(0)   NOT NULL CONSTRAINT [DF_ret_requested] DEFAULT SYSDATETIME(),
    [resolved_at]     datetime2(0)   NULL,
    CONSTRAINT [PK_return_requests] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_return_requests_code] UNIQUE ([code]),
    CONSTRAINT [FK_ret_store_order] FOREIGN KEY ([store_order_id]) REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [FK_ret_order_item]  FOREIGN KEY ([order_item_id])  REFERENCES [dbo].[order_items] ([id]),
    CONSTRAINT [FK_ret_customer]    FOREIGN KEY ([customer_id])    REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_ret_handler]     FOREIGN KEY ([handled_by])     REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_ret_type] CHECK ([type] IN (N'RETURN', N'EXCHANGE', N'REFUND_ONLY')),
    CONSTRAINT [CK_ret_status] CHECK ([status] IN
        (N'REQUESTED', N'SELLER_REVIEWING', N'APPROVED', N'REJECTED',
         N'ESCALATED', N'COMPLETED', N'CANCELLED'))
);
GO
CREATE INDEX [IX_ret_store_order] ON [dbo].[return_requests] ([store_order_id]);
CREATE INDEX [IX_ret_status]      ON [dbo].[return_requests] ([status]);
GO

CREATE TABLE [dbo].[return_attachments] (
    [id]                bigint        IDENTITY(1,1) NOT NULL,
    [return_request_id] bigint        NOT NULL,
    [file_path]         nvarchar(500) NOT NULL,
    [mime_type]         nvarchar(100) NULL,
    [uploaded_at]       datetime2(0)  NOT NULL CONSTRAINT [DF_ra_uploaded] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_return_attachments] PRIMARY KEY ([id]),
    CONSTRAINT [FK_ra_request] FOREIGN KEY ([return_request_id])
        REFERENCES [dbo].[return_requests] ([id]) ON DELETE CASCADE
);
GO

-- ---------------------------------------------------------------------
-- warranties — sinh tự động khi đơn con chuyển DELIVERED,
-- lấy warranty_months_snapshot của từng dòng hàng.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[warranties] (
    [id]            bigint       IDENTITY(1,1) NOT NULL,
    [code]          nvarchar(20) NOT NULL,
    [order_item_id] bigint       NOT NULL,
    [product_id]    bigint       NULL,
    [customer_id]   bigint       NOT NULL,
    [store_id]      bigint       NOT NULL,
    [serial_no]     nvarchar(60) NULL,
    [months]        int          NOT NULL,
    [start_at]      date         NOT NULL,
    [end_at]        date         NOT NULL,
    [status]        nvarchar(20) NOT NULL CONSTRAINT [DF_wr_status] DEFAULT N'ACTIVE',
    [created_at]    datetime2(0) NOT NULL CONSTRAINT [DF_wr_created] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_warranties] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_warranties_code] UNIQUE ([code]),
    CONSTRAINT [UQ_warranties_item] UNIQUE ([order_item_id]),
    CONSTRAINT [FK_wr_order_item] FOREIGN KEY ([order_item_id]) REFERENCES [dbo].[order_items] ([id]),
    CONSTRAINT [FK_wr_product]    FOREIGN KEY ([product_id])    REFERENCES [dbo].[products] ([id]),
    CONSTRAINT [FK_wr_customer]   FOREIGN KEY ([customer_id])   REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_wr_store]      FOREIGN KEY ([store_id])      REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [CK_wr_status] CHECK ([status] IN (N'ACTIVE', N'EXPIRED', N'VOID')),
    CONSTRAINT [CK_wr_han] CHECK ([start_at] <= [end_at])
);
GO
CREATE INDEX [IX_wr_customer] ON [dbo].[warranties] ([customer_id]);
CREATE INDEX [IX_wr_end]      ON [dbo].[warranties] ([end_at]);
GO

CREATE TABLE [dbo].[warranty_claims] (
    [id]           bigint         IDENTITY(1,1) NOT NULL,
    [warranty_id]  bigint         NOT NULL,
    [description]  nvarchar(1000) NOT NULL,
    [status]       nvarchar(20)   NOT NULL CONSTRAINT [DF_wc_status] DEFAULT N'OPEN',
    [handled_by]   bigint         NULL,
    [resolution]   nvarchar(1000) NULL,
    [created_at]   datetime2(0)   NOT NULL CONSTRAINT [DF_wc_created] DEFAULT SYSDATETIME(),
    [resolved_at]  datetime2(0)   NULL,
    CONSTRAINT [PK_warranty_claims] PRIMARY KEY ([id]),
    CONSTRAINT [FK_wc_warranty] FOREIGN KEY ([warranty_id]) REFERENCES [dbo].[warranties] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK_wc_handler]  FOREIGN KEY ([handled_by])  REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_wc_status] CHECK ([status] IN (N'OPEN', N'IN_PROGRESS', N'DONE', N'REJECTED'))
);
GO

-- ---------------------------------------------------------------------
-- disputes — mục 7.10. Leo thang khi Seller và Customer không thống nhất.
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[disputes] (
    [id]                bigint         IDENTITY(1,1) NOT NULL,
    [code]              nvarchar(20)   NOT NULL,
    [store_order_id]    bigint         NOT NULL,
    [return_request_id] bigint         NULL,
    [ticket_id]         bigint         NULL,
    [customer_id]       bigint         NOT NULL,
    [store_id]          bigint         NOT NULL,
    [reason]            nvarchar(1000) NOT NULL,
    [status]            nvarchar(30)   NOT NULL CONSTRAINT [DF_dp_status] DEFAULT N'OPEN',
    [decision]          nvarchar(1000) NULL,
    [decided_by]        bigint         NULL,
    [decided_at]        datetime2(0)   NULL,
    [opened_at]         datetime2(0)   NOT NULL CONSTRAINT [DF_dp_opened] DEFAULT SYSDATETIME(),
    CONSTRAINT [PK_disputes] PRIMARY KEY ([id]),
    CONSTRAINT [UQ_disputes_code] UNIQUE ([code]),
    CONSTRAINT [FK_dp_store_order] FOREIGN KEY ([store_order_id])    REFERENCES [dbo].[store_orders] ([id]),
    CONSTRAINT [FK_dp_return]      FOREIGN KEY ([return_request_id]) REFERENCES [dbo].[return_requests] ([id]),
    CONSTRAINT [FK_dp_ticket]      FOREIGN KEY ([ticket_id])         REFERENCES [dbo].[tickets] ([id]),
    CONSTRAINT [FK_dp_customer]    FOREIGN KEY ([customer_id])       REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_dp_store]       FOREIGN KEY ([store_id])          REFERENCES [dbo].[stores] ([id]),
    CONSTRAINT [FK_dp_decider]     FOREIGN KEY ([decided_by])        REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [CK_dp_status] CHECK ([status] IN
        (N'OPEN', N'INVESTIGATING', N'RESOLVED_CUSTOMER', N'RESOLVED_SELLER', N'CLOSED')),
    -- Mục 7.10: quyết định phải kèm lý do
    CONSTRAINT [CK_dp_quyet_dinh] CHECK
        ([status] NOT IN (N'RESOLVED_CUSTOMER', N'RESOLVED_SELLER') OR [decision] IS NOT NULL)
);
GO
CREATE INDEX [IX_dp_status] ON [dbo].[disputes] ([status]);
GO

PRINT N'06 — Xong nhóm hậu mãi.';
GO
