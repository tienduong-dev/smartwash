-- =====================================================================
-- DỮ LIỆU MẪU — GIAI ĐOẠN 3
-- Tài khoản, vai trò, thương hiệu, doanh nghiệp, gian hàng, hồ sơ
--
-- Chạy SAU khi đã chạy xong 00 → 07.
-- Chạy lại được nhiều lần: mỗi khối đều kiểm tra tồn tại trước khi chèn.
--
-- Tài khoản đăng nhập thử:
--   admin@smartwash.vn      / Admin@2026    (quản trị viên)
--   seller.lg@smartwash.vn  / Seller@2026   (chủ gian hàng LG)
--   seller.sam@smartwash.vn / Seller@2026   (chủ gian hàng Samsung)
--   nhanvien@smartwash.vn   / Seller@2026   (nhân viên gian hàng LG)
--   khach@smartwash.vn      / Khach@2026    (khách hàng)
--   ungvien@smartwash.vn    / Seller@2026   (đang có hồ sơ chờ duyệt)
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- 1. VAI TRÒ
-- ---------------------------------------------------------------------
MERGE [dbo].[roles] AS t
USING (VALUES
    (N'ADMIN',        N'Quản trị viên',   N'Toàn quyền trên sàn'),
    (N'SELLER_OWNER', N'Chủ gian hàng',   N'Sở hữu và vận hành gian hàng'),
    (N'SELLER_STAFF', N'Nhân viên bán hàng', N'Được chủ gian hàng phân quyền'),
    (N'CUSTOMER',     N'Khách hàng',      N'Mua sắm trên sàn')
) AS s ([code], [name], [description])
ON t.[code] = s.[code]
WHEN NOT MATCHED THEN
    INSERT ([code], [name], [description]) VALUES (s.[code], s.[name], s.[description]);
GO

-- ---------------------------------------------------------------------
-- 2. CẤU HÌNH SÀN
-- ---------------------------------------------------------------------
MERGE [dbo].[settings] AS t
USING (VALUES
    (N'commission_rate',          N'5.00', N'Tỷ lệ phí sàn mặc định (%)'),
    (N'return_window_days',       N'7',    N'Số ngày được gửi yêu cầu đổi trả sau khi giao thành công'),
    (N'product_review_required',  N'1',    N'Bắt buộc quản trị viên duyệt sản phẩm mới'),
    (N'max_stores_per_business',  N'',     N'Số gian hàng tối đa mỗi doanh nghiệp, để trống là không giới hạn')
) AS s ([setting_key], [setting_value], [description])
ON t.[setting_key] = s.[setting_key]
WHEN NOT MATCHED THEN
    INSERT ([setting_key], [setting_value], [description])
    VALUES (s.[setting_key], s.[setting_value], s.[description]);
GO

-- ---------------------------------------------------------------------
-- 3. TÀI KHOẢN
--    Mật khẩu băm bằng BCrypt cost 12, khớp với BCryptPasswordEncoder(12).
-- ---------------------------------------------------------------------
MERGE [dbo].[users] AS t
USING (VALUES
    (N'Quản trị viên SmartWash', N'admin@smartwash.vn',      N'0901000001',
     N'$2a$12$dTGMDwB4oRh6PHOY5KxDCOENf.vP.tABNpc1te.0FbaIXe.4/LEVW'),
    (N'Trần Minh Khoa',          N'seller.lg@smartwash.vn',  N'0901000002',
     N'$2a$12$0cZMwlHhdOPpHnxNUkmtsOAoUMihAuIvVazNrhYJlUHoeYEmmmJ9S'),
    (N'Lê Thu Hà',               N'seller.sam@smartwash.vn', N'0901000003',
     N'$2a$12$0cZMwlHhdOPpHnxNUkmtsOAoUMihAuIvVazNrhYJlUHoeYEmmmJ9S'),
    (N'Phạm Quốc Dũng',          N'nhanvien@smartwash.vn',   N'0901000004',
     N'$2a$12$0cZMwlHhdOPpHnxNUkmtsOAoUMihAuIvVazNrhYJlUHoeYEmmmJ9S'),
    (N'Nguyễn Thị Mai',          N'khach@smartwash.vn',      N'0901000005',
     N'$2a$12$YIc2zDbQOWmaaeEVrV8ICuQWqUaYLw4RNoovcULIsaqAODxNTFezS'),
    (N'Vũ Hoàng Nam',            N'ungvien@smartwash.vn',    N'0901000006',
     N'$2a$12$0cZMwlHhdOPpHnxNUkmtsOAoUMihAuIvVazNrhYJlUHoeYEmmmJ9S')
) AS s ([full_name], [email], [phone], [password_hash])
ON t.[email] = s.[email]
WHEN NOT MATCHED THEN
    INSERT ([full_name], [email], [phone], [password_hash], [status], [email_verified_at])
    VALUES (s.[full_name], s.[email], s.[phone], s.[password_hash], N'ACTIVE', SYSDATETIME());
GO

-- Cấp vai trò
INSERT INTO [dbo].[user_roles] ([user_id], [role_id])
SELECT u.[id], r.[id]
FROM [dbo].[users] u
CROSS JOIN [dbo].[roles] r
WHERE (
         (u.[email] = N'admin@smartwash.vn'      AND r.[code] = N'ADMIN')
      OR (u.[email] = N'seller.lg@smartwash.vn'  AND r.[code] IN (N'SELLER_OWNER', N'CUSTOMER'))
      OR (u.[email] = N'seller.sam@smartwash.vn' AND r.[code] IN (N'SELLER_OWNER', N'CUSTOMER'))
      OR (u.[email] = N'nhanvien@smartwash.vn'   AND r.[code] IN (N'SELLER_STAFF', N'CUSTOMER'))
      OR (u.[email] = N'khach@smartwash.vn'      AND r.[code] = N'CUSTOMER')
      OR (u.[email] = N'ungvien@smartwash.vn'    AND r.[code] = N'CUSTOMER')
      )
  -- Ngoặc ở trên là bắt buộc: AND ưu tiên hơn OR, thiếu nó thì NOT EXISTS
  -- chỉ áp cho nhánh OR cuối cùng và chạy lại lần hai sẽ vi phạm khoá chính.
  AND NOT EXISTS (
      SELECT 1 FROM [dbo].[user_roles] x WHERE x.[user_id] = u.[id] AND x.[role_id] = r.[id]);
GO

-- ---------------------------------------------------------------------
-- 4. THƯƠNG HIỆU
--    normalized_name sinh theo đúng quy tắc của BrandNameNormalizer:
--    bỏ dấu, viết hoa, bỏ ký tự lạ, bỏ đuôi pháp nhân.
-- ---------------------------------------------------------------------
MERGE [dbo].[brands] AS t
USING (VALUES
    (N'LG',         N'LG',         N'lg',         N'Hàn Quốc',   N'/img/brands/lg.svg'),
    (N'Samsung',    N'SAMSUNG',    N'samsung',    N'Hàn Quốc',   N'/img/brands/samsung.svg'),
    (N'Electrolux', N'ELECTROLUX', N'electrolux', N'Thuỵ Điển',  N'/img/brands/electrolux.svg'),
    (N'Panasonic',  N'PANASONIC',  N'panasonic',  N'Nhật Bản',   N'/img/brands/panasonic.svg'),
    (N'Toshiba',    N'TOSHIBA',    N'toshiba',    N'Nhật Bản',   N'/img/brands/toshiba.svg'),
    (N'Hitachi',    N'HITACHI',    N'hitachi',    N'Nhật Bản',   N'/img/brands/hitachi.svg'),
    (N'Sharp',      N'SHARP',      N'sharp',      N'Nhật Bản',   N'/img/brands/sharp.svg'),
    (N'Aqua',       N'AQUA',       N'aqua',       N'Nhật Bản',   N'/img/brands/aqua.svg'),
    (N'Beko',       N'BEKO',       N'beko',       N'Thổ Nhĩ Kỳ', N'/img/brands/beko.svg'),
    (N'Casper',     N'CASPER',     N'casper',     N'Việt Nam',   N'/img/brands/casper.svg'),
    (N'Midea',      N'MIDEA',      N'midea',      N'Trung Quốc', N'/img/brands/midea.svg'),
    (N'Xiaomi',     N'XIAOMI',     N'xiaomi',     N'Trung Quốc', N'/img/brands/xiaomi.svg')
) AS s ([name], [normalized_name], [slug], [country], [logo])
ON t.[slug] = s.[slug]
WHEN NOT MATCHED THEN
    INSERT ([name], [normalized_name], [slug], [country], [logo], [status], [verified_at])
    VALUES (s.[name], s.[normalized_name], s.[slug], s.[country], s.[logo], N'ACTIVE', SYSDATETIME());
GO

-- Một thương hiệu đang chờ xác minh, để thử luồng duyệt
IF NOT EXISTS (SELECT 1 FROM [dbo].[brands] WHERE [slug] = N'whirlpool')
INSERT INTO [dbo].[brands] ([name], [normalized_name], [slug], [country], [status])
VALUES (N'Whirlpool', N'WHIRLPOOL', N'whirlpool', N'Hoa Kỳ', N'PENDING_VERIFICATION');
GO

-- ---------------------------------------------------------------------
-- 5. DOANH NGHIỆP
-- ---------------------------------------------------------------------
MERGE [dbo].[businesses] AS t
USING (VALUES
    (N'Công ty TNHH Thương mại Minh Khoa', N'0312345678',
     N'145 Nguyễn Trãi, Thanh Xuân, Hà Nội',  N'Trần Minh Khoa', N'0901000002'),
    (N'Công ty CP Điện máy Thu Hà',        N'0398765432',
     N'22 Lê Văn Lương, Cầu Giấy, Hà Nội',    N'Lê Thu Hà',      N'0901000003')
) AS s ([legal_name], [tax_code], [address], [representative_name], [representative_phone])
ON t.[tax_code] = s.[tax_code]
WHEN NOT MATCHED THEN
    INSERT ([legal_name], [tax_code], [address], [representative_name], [representative_phone],
            [verification_status], [verified_at])
    VALUES (s.[legal_name], s.[tax_code], s.[address], s.[representative_name],
            s.[representative_phone], N'VERIFIED', SYSDATETIME());
GO

-- ---------------------------------------------------------------------
-- 6. GIAN HÀNG — mỗi gian hàng đúng MỘT thương hiệu (BR-03)
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[stores]
    ([business_id], [brand_id], [name], [slug], [store_type], [logo], [description],
     [phone], [email], [pickup_address], [return_policy], [warranty_policy], [shipping_policy],
     [status], [is_verified], [approved_at], [approved_by])
SELECT b.[id], br.[id], v.[name], v.[slug], v.[store_type], v.[logo], v.[mo_ta],
       v.[phone], v.[email], v.[pickup],
       N'Đổi trả trong 7 ngày nếu sản phẩm lỗi do nhà sản xuất, còn nguyên tem niêm phong.',
       N'Bảo hành chính hãng theo phiếu của nhà sản xuất, 24 tháng với động cơ inverter.',
       N'Miễn phí giao lắp nội thành Hà Nội. Các tỉnh khác tính theo bảng giá vận chuyển.',
       N'ACTIVE', v.[verified], SYSDATETIME(), (SELECT [id] FROM [dbo].[users] WHERE [email] = N'admin@smartwash.vn')
FROM (VALUES
    (N'0312345678', N'lg',      N'LG Official Store',      N'lg-official-store',      N'OFFICIAL',
     N'/img/brands/lg.svg',      N'Gian hàng chính hãng LG tại Việt Nam, đầy đủ dòng máy giặt lồng ngang và lồng đứng.',
     N'0901000002', N'lg@smartwash.vn',  N'145 Nguyễn Trãi, Thanh Xuân, Hà Nội', CAST(1 AS bit)),
    (N'0312345678', N'toshiba', N'Minh Khoa Electronics',  N'minh-khoa-electronics',  N'RETAILER',
     N'/img/stores/mac-dinh.svg', N'Cửa hàng bán lẻ máy giặt Toshiba chính hãng, nhập từ nhà phân phối uỷ quyền.',
     N'0901000002', N'toshiba@smartwash.vn', N'145 Nguyễn Trãi, Thanh Xuân, Hà Nội', CAST(0 AS bit)),
    (N'0398765432', N'samsung', N'Samsung Home Appliance', N'samsung-home-appliance', N'DISTRIBUTOR',
     N'/img/brands/samsung.svg', N'Nhà phân phối uỷ quyền Samsung khu vực miền Bắc.',
     N'0901000003', N'samsung@smartwash.vn', N'22 Lê Văn Lương, Cầu Giấy, Hà Nội', CAST(1 AS bit))
) AS v ([tax_code], [brand_slug], [name], [slug], [store_type], [logo], [mo_ta], [phone], [email], [pickup], [verified])
JOIN [dbo].[businesses] b ON b.[tax_code] = v.[tax_code]
JOIN [dbo].[brands]     br ON br.[slug]   = v.[brand_slug]
WHERE NOT EXISTS (SELECT 1 FROM [dbo].[stores] s WHERE s.[slug] = v.[slug]);
GO

-- ---------------------------------------------------------------------
-- 7. NHÂN SỰ GIAN HÀNG — chủ có toàn quyền, nhân viên có quyền hẹp hơn
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[store_members]
    ([store_id], [user_id], [member_role],
     [can_manage_products], [can_handle_orders], [can_support_customers], [can_view_reports], [status])
SELECT s.[id], u.[id], v.[vai_tro], v.[sp], v.[dh], v.[ht], v.[bc], N'ACTIVE'
FROM (VALUES
    (N'lg-official-store',      N'seller.lg@smartwash.vn',  N'OWNER',
     CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit)),
    (N'minh-khoa-electronics',  N'seller.lg@smartwash.vn',  N'OWNER',
     CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit)),
    (N'samsung-home-appliance', N'seller.sam@smartwash.vn', N'OWNER',
     CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit), CAST(1 AS bit)),
    -- Nhân viên: chỉ xử lý đơn và hỗ trợ khách, không sửa sản phẩm, không xem báo cáo
    (N'lg-official-store',      N'nhanvien@smartwash.vn',   N'STAFF',
     CAST(0 AS bit), CAST(1 AS bit), CAST(1 AS bit), CAST(0 AS bit))
) AS v ([store_slug], [email], [vai_tro], [sp], [dh], [ht], [bc])
JOIN [dbo].[stores] s ON s.[slug]  = v.[store_slug]
JOIN [dbo].[users]  u ON u.[email] = v.[email]
WHERE NOT EXISTS (
    SELECT 1 FROM [dbo].[store_members] m WHERE m.[store_id] = s.[id] AND m.[user_id] = u.[id]);
GO

-- ---------------------------------------------------------------------
-- 8. ĐỊA CHỈ NHẬN HÀNG CỦA KHÁCH
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[addresses]
    ([user_id], [label], [receiver_name], [phone], [line],
     [province_id], [province_name], [district_id], [district_name],
     [ward_code], [ward_name], [is_default])
SELECT u.[id], N'Nhà riêng', N'Nguyễn Thị Mai', N'0901000005',
       N'Số 18, ngõ 72, đường Trần Duy Hưng',
       1, N'Hà Nội', 3440, N'Quận Cầu Giấy', N'13006', N'Phường Trung Hoà', 1
FROM [dbo].[users] u
WHERE u.[email] = N'khach@smartwash.vn'
  AND NOT EXISTS (SELECT 1 FROM [dbo].[addresses] a WHERE a.[user_id] = u.[id]);
GO

-- ---------------------------------------------------------------------
-- 9. HỒ SƠ MỞ GIAN HÀNG ĐANG CHỜ — để thử luồng thẩm định ngay
--    Cố ý đề xuất "ELECTROLUX VIỆT NAM": chuẩn hoá ra đúng khoá ELECTROLUX
--    của thương hiệu đã có, nên màn hình thẩm định sẽ cảnh báo nghi trùng.
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[seller_registrations]
    ([code], [applicant_user_id], [requested_brand_name], [store_name], [store_slug_proposed],
     [store_type], [contact_phone], [contact_email], [pickup_address], [applicant_note], [status])
SELECT N'HS2026-000001', u.[id], N'Electrolux Việt Nam', N'Electrolux Miền Bắc',
       N'electrolux-mien-bac', N'DISTRIBUTOR', N'0901000006', N'ungvien@smartwash.vn',
       N'88 Giải Phóng, Hoàng Mai, Hà Nội',
       N'Chúng tôi là nhà phân phối uỷ quyền khu vực miền Bắc, đã có hợp đồng phân phối ký năm 2025.',
       N'PENDING'
FROM [dbo].[users] u
WHERE u.[email] = N'ungvien@smartwash.vn'
  AND NOT EXISTS (SELECT 1 FROM [dbo].[seller_registrations] r WHERE r.[code] = N'HS2026-000001');
GO

-- Một hồ sơ nữa đang cần bổ sung, để thấy đủ các trạng thái
INSERT INTO [dbo].[seller_registrations]
    ([code], [applicant_user_id], [requested_brand_id], [store_name], [store_slug_proposed],
     [store_type], [contact_phone], [pickup_address], [status], [review_note], [reviewed_by], [reviewed_at])
SELECT N'HS2026-000002', u.[id], b.[id], N'Beko Hà Nội', N'beko-ha-noi',
       N'RETAILER', N'0901000006', N'12 Thái Hà, Đống Đa, Hà Nội',
       N'NEEDS_MORE_INFO',
       N'Giấy phép kinh doanh đã mờ, không đọc được số đăng ký. Vui lòng chụp lại rõ nét và tải lên.',
       (SELECT [id] FROM [dbo].[users] WHERE [email] = N'admin@smartwash.vn'), SYSDATETIME()
FROM [dbo].[users] u
CROSS JOIN [dbo].[brands] b
WHERE u.[email] = N'ungvien@smartwash.vn' AND b.[slug] = N'beko'
  AND NOT EXISTS (SELECT 1 FROM [dbo].[seller_registrations] r WHERE r.[code] = N'HS2026-000002');
GO

-- ---------------------------------------------------------------------
-- 10. THÔNG BÁO MẪU
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[notifications] ([user_id], [type], [title], [content], [link])
SELECT u.[id], N'STORE_NEEDS_INFO', N'Hồ sơ HS2026-000002 cần bổ sung',
       N'Giấy phép kinh doanh đã mờ, không đọc được số đăng ký. Vui lòng chụp lại rõ nét và tải lên.',
       N'/seller'
FROM [dbo].[users] u
WHERE u.[email] = N'ungvien@smartwash.vn'
  AND NOT EXISTS (SELECT 1 FROM [dbo].[notifications] n
                  WHERE n.[user_id] = u.[id] AND n.[title] = N'Hồ sơ HS2026-000002 cần bổ sung');
GO

PRINT N'10 — Xong dữ liệu mẫu giai đoạn 3.';
GO

-- ---------------------------------------------------------------------
-- KIỂM TRA NHANH
-- ---------------------------------------------------------------------
SELECT N'Vai trò'       AS [Bảng], COUNT(*) AS [Số dòng] FROM [dbo].[roles]
UNION ALL SELECT N'Tài khoản',            COUNT(*) FROM [dbo].[users]
UNION ALL SELECT N'Gán vai trò',          COUNT(*) FROM [dbo].[user_roles]
UNION ALL SELECT N'Thương hiệu',          COUNT(*) FROM [dbo].[brands]
UNION ALL SELECT N'Doanh nghiệp',         COUNT(*) FROM [dbo].[businesses]
UNION ALL SELECT N'Gian hàng',            COUNT(*) FROM [dbo].[stores]
UNION ALL SELECT N'Nhân sự gian hàng',    COUNT(*) FROM [dbo].[store_members]
UNION ALL SELECT N'Hồ sơ mở gian hàng',   COUNT(*) FROM [dbo].[seller_registrations]
UNION ALL SELECT N'Địa chỉ',              COUNT(*) FROM [dbo].[addresses]
UNION ALL SELECT N'Cấu hình',             COUNT(*) FROM [dbo].[settings];
GO

-- Mỗi gian hàng phải có đúng một chủ
SELECT s.[name] AS [Gian hàng],
       SUM(CASE WHEN m.[member_role] = N'OWNER' THEN 1 ELSE 0 END) AS [Số chủ],
       COUNT(m.[id]) AS [Tổng nhân sự]
FROM [dbo].[stores] s
LEFT JOIN [dbo].[store_members] m ON m.[store_id] = s.[id] AND m.[status] = N'ACTIVE'
GROUP BY s.[name]
ORDER BY s.[name];
GO
