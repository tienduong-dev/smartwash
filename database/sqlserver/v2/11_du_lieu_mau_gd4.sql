-- =====================================================================
-- DỮ LIỆU MẪU — GIAI ĐOẠN 4
-- Danh mục, sản phẩm, tồn kho, ảnh, thông số
--
-- Chạy SAU 10_du_lieu_mau_gd3.sql.
-- Chạy lại được nhiều lần: mỗi khối đều kiểm tra tồn tại trước khi chèn.
--
-- BR-04 được bảo đảm bằng cách lấy brand_id THẲNG TỪ GIAN HÀNG, không gõ tay.
-- Trigger TR_products_brand_khop_store sẽ chặn nếu có sai sót.
-- =====================================================================
USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- 1. DANH MỤC — cây hai cấp
-- ---------------------------------------------------------------------
MERGE [dbo].[categories] AS t
USING (VALUES
    (N'Máy giặt',             N'may-giat',             1, N'Máy giặt gia đình mọi khối lượng và kiểu cửa'),
    (N'Máy sấy quần áo',      N'may-say-quan-ao',      2, N'Máy sấy thông hơi và bơm nhiệt'),
    (N'Máy giặt sấy',         N'may-giat-say',         3, N'Giặt và sấy trong cùng một máy'),
    (N'Máy giặt công nghiệp', N'may-giat-cong-nghiep', 4, N'Công suất lớn cho tiệm giặt là và khách sạn')
) AS s ([name], [slug], [sort_order], [description])
ON t.[slug] = s.[slug]
WHEN NOT MATCHED THEN
    INSERT ([name], [slug], [sort_order], [description], [status])
    VALUES (s.[name], s.[slug], s.[sort_order], s.[description], N'ACTIVE');
GO

-- Danh mục con của "Máy giặt"
INSERT INTO [dbo].[categories] ([parent_id], [name], [slug], [sort_order], [description], [status])
SELECT cha.[id], v.[name], v.[slug], v.[sort_order], v.[mo_ta], N'ACTIVE'
FROM (VALUES
    (N'Máy giặt cửa ngang', N'may-giat-cua-ngang', 1, N'Lồng ngang, giặt sạch sâu, tiết kiệm nước'),
    (N'Máy giặt cửa trên',  N'may-giat-cua-tren',  2, N'Lồng đứng, thao tác nhanh, giá dễ chịu'),
    (N'Máy giặt mini',      N'may-giat-mini',      3, N'Khối lượng nhỏ, hợp nhà trọ và em bé')
) AS v ([name], [slug], [sort_order], [mo_ta])
CROSS JOIN (SELECT [id] FROM [dbo].[categories] WHERE [slug] = N'may-giat') AS cha
WHERE NOT EXISTS (SELECT 1 FROM [dbo].[categories] c WHERE c.[slug] = v.[slug]);
GO

-- ---------------------------------------------------------------------
-- 2. SẢN PHẨM
--    brand_id lấy từ stores.brand_id -> BR-04 luôn đúng.
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[products]
    ([store_id], [brand_id], [category_id], [model_code], [name], [slug],
     [description], [highlights],
     [price], [original_price], [promo_price], [promo_start_at], [promo_end_at],
     [status], [reject_reason], [submitted_at], [approved_at],
     [warranty_months], [image], [sold_count], [view_count],
     [door_type], [capacity_kg], [spin_speed_rpm], [is_inverter], [energy_rating],
     [width_cm], [height_cm], [depth_cm], [weight_kg], [power_kwh_year], [water_liter_cycle])
SELECT
    st.[id], st.[brand_id], dm.[id], v.[model_code], v.[ten], v.[slug],
    v.[mo_ta], v.[noi_bat],
    v.[gia], v.[gia_goc], v.[gia_km],
    CASE WHEN v.[gia_km] IS NULL THEN NULL ELSE DATEADD(DAY, -3, SYSDATETIME()) END,
    CASE WHEN v.[gia_km] IS NULL THEN NULL ELSE DATEADD(DAY, 25, SYSDATETIME()) END,
    v.[trang_thai], v.[ly_do],
    CASE WHEN v.[trang_thai] IN (N'DRAFT') THEN NULL ELSE DATEADD(DAY, -5, SYSDATETIME()) END,
    CASE WHEN v.[trang_thai] = N'ACTIVE' THEN DATEADD(DAY, -4, SYSDATETIME()) ELSE NULL END,
    v.[bao_hanh], v.[anh], v.[da_ban], v.[luot_xem],
    v.[kieu_cua], v.[dung_tich], v.[toc_do_vat], v.[inverter], v.[nhan_nl],
    v.[rong], v.[cao], v.[sau], v.[nang], v.[dien], v.[nuoc]
FROM (VALUES
    -- ===================== LG Official Store (thương hiệu LG) =====================
    (N'lg-official-store', N'may-giat-cua-ngang', N'FV1450H3W',
     N'Máy giặt LG AI DD Inverter 10.5 kg FV1450H3W', N'may-giat-lg-fv1450h3w',
     N'Công nghệ AI DD nhận diện chất liệu vải để chọn chuyển động giặt phù hợp, giúp vải bền hơn tới 18%. Lồng giặt thép không gỉ, động cơ Inverter trực tiếp bảo hành 10 năm.',
     N'AI DD nhận diện loại vải' + CHAR(10) + N'Giặt hơi nước Steam diệt 99,9% vi khuẩn' + CHAR(10) + N'Động cơ Inverter bảo hành 10 năm' + CHAR(10) + N'Khoá trẻ em và tự chẩn đoán lỗi',
     13410000, 15240000, 12890000,
     N'ACTIVE', NULL, 24, N'/img/products/cua-ngang-lg.svg', 128, 2140,
     N'cua_ngang', 10.5, 1400, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 56.5, 68.0, 198.00, 48.00),

    (N'lg-official-store', N'may-giat-cua-ngang', N'FV1409S4W',
     N'Máy giặt LG Inverter 9 kg FV1409S4W', N'may-giat-lg-fv1409s4w',
     N'Phù hợp gia đình 3–4 người. Lồng giặt 9 kg, 14 chương trình giặt, chế độ giặt nhanh 14 phút cho quần áo ít bẩn.',
     N'14 chương trình giặt' + CHAR(10) + N'Giặt nhanh 14 phút' + CHAR(10) + N'Thêm đồ giữa chừng',
     8990000, 10490000, NULL,
     N'ACTIVE', NULL, 24, N'/img/products/cua-ngang-lg.svg', 96, 1510,
     N'cua_ngang', 9.0, 1200, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 55.0, 63.0, 176.00, 45.00),

    (N'lg-official-store', N'may-giat-cua-tren', N'T2555VSAB',
     N'Máy giặt LG Inverter lồng đứng 12 kg T2555VSAB', N'may-giat-lg-t2555vsab',
     N'Lồng đứng 12 kg cho gia đình đông người. Công nghệ Smart Inverter tiết kiệm điện, mâm giặt Punch+3 tạo dòng nước xoáy ba chiều.',
     N'Lồng đứng 12 kg' + CHAR(10) + N'Mâm giặt Punch+3' + CHAR(10) + N'Smart Inverter tiết kiệm điện',
     7490000, 8290000, NULL,
     N'ACTIVE', NULL, 24, N'/img/products/cua-tren-lg.svg', 74, 1180,
     N'cua_tren', 12.0, 700, CAST(1 AS bit), N'4 sao', 54.0, 103.0, 58.0, 41.0, 152.00, 62.00),

    (N'lg-official-store', N'may-giat-say', N'FV1450H2B',
     N'Máy giặt sấy LG Inverter 10.5 kg giặt / 7 kg sấy FV1450H2B', N'may-giat-say-lg-fv1450h2b',
     N'Giặt và sấy khép kín trong một máy, không cần chuyển đồ. Sấy bằng hơi nóng kết hợp cảm biến độ ẩm để tránh sấy quá tay.',
     N'Giặt 10.5 kg, sấy 7 kg' + CHAR(10) + N'Cảm biến độ ẩm tự ngắt' + CHAR(10) + N'Giặt sấy liên hoàn trong 3 giờ',
     18990000, 21500000, NULL,
     N'PENDING_REVIEW', NULL, 24, N'/img/products/thap-giat-say-lg.svg', 0, 12,
     N'cua_ngang', 10.5, 1400, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 61.0, 76.0, 228.00, 52.00),

    (N'lg-official-store', N'may-say-quan-ao', N'RH9V3DMA',
     N'Máy sấy bơm nhiệt LG Dual Inverter 9 kg RH9V3DMA', N'may-say-lg-rh9v3dma',
     N'Sấy bơm nhiệt ở nhiệt độ thấp nên giữ được sợi vải, phù hợp đồ len và đồ trẻ em.',
     N'Bơm nhiệt tiết kiệm điện' + CHAR(10) + N'Sấy nhiệt độ thấp giữ sợi vải',
     16490000, NULL, NULL,
     N'DRAFT', NULL, 24, N'/img/products/may-say-lg.svg', 0, 0,
     NULL, 9.0, NULL, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 66.0, 54.0, 176.00, NULL),

    -- ============= Minh Khoa Electronics (thương hiệu Toshiba) =============
    (N'minh-khoa-electronics', N'may-giat-cua-ngang', N'TW-BK105G4V',
     N'Máy giặt Toshiba Inverter 9.5 kg TW-BK105G4V', N'may-giat-toshiba-tw-bk105g4v',
     N'Công nghệ giặt Ag+ kháng khuẩn, lồng giặt Dual SDD giúp quần áo ít xoắn rối. Hàng chính hãng nhập từ nhà phân phối uỷ quyền.',
     N'Ion bạc Ag+ kháng khuẩn' + CHAR(10) + N'Lồng Dual SDD chống xoắn' + CHAR(10) + N'15 chương trình giặt',
     9290000, 10900000, 8790000,
     N'ACTIVE', NULL, 24, N'/img/products/cua-ngang-toshiba.svg', 61, 940,
     N'cua_ngang', 9.5, 1200, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 55.0, 62.0, 182.00, 47.00),

    (N'minh-khoa-electronics', N'may-giat-cua-tren', N'AW-DUM1300KV',
     N'Máy giặt Toshiba Inverter lồng đứng 12 kg AW-DUM1300KV', N'may-giat-toshiba-aw-dum1300kv',
     N'Mâm giặt 3 chiều Great Waves tạo dòng nước mạnh, giặt sạch đồ dày như chăn mền.',
     N'Mâm giặt 3 chiều Great Waves' + CHAR(10) + N'Giặt được chăn mền dày',
     6890000, 7990000, NULL,
     N'ACTIVE', NULL, 24, N'/img/products/cua-tren-toshiba.svg', 52, 810,
     N'cua_tren', 12.0, 700, CAST(1 AS bit), N'4 sao', 59.5, 105.0, 63.0, 43.0, 158.00, 65.00),

    (N'minh-khoa-electronics', N'may-say-quan-ao', N'TD-H80SEV',
     N'Máy sấy thông hơi Toshiba 8 kg TD-H80SEV', N'may-say-toshiba-td-h80sev',
     N'Máy sấy thông hơi dung tích 8 kg, 4 chế độ sấy.',
     N'4 chế độ sấy' + CHAR(10) + N'Cửa kính rộng dễ lấy đồ',
     6190000, NULL, NULL,
     N'REJECTED', N'Ảnh sản phẩm bị mờ và thiếu thông số điện năng tiêu thụ. Vui lòng bổ sung rồi nộp lại.',
     12, N'/img/products/may-say-toshiba.svg', 0, 34,
     NULL, 8.0, NULL, CAST(0 AS bit), N'3 sao', 59.5, 85.0, 60.0, 35.0, NULL, NULL),

    -- ============ Samsung Home Appliance (thương hiệu Samsung) ============
    (N'samsung-home-appliance', N'may-giat-cua-ngang', N'WW10TP54DSH',
     N'Máy giặt Samsung AI Ecobubble Inverter 10 kg WW10TP54DSH', N'may-giat-samsung-ww10tp54dsh',
     N'Công nghệ Ecobubble hoà tan bột giặt thành bọt trước khi tiếp xúc quần áo, giặt sạch ngay ở nước lạnh nên tiết kiệm điện đun nước.',
     N'Ecobubble giặt sạch bằng nước lạnh' + CHAR(10) + N'AI Wash tự tính lượng nước và bột giặt' + CHAR(10) + N'Khử khuẩn bằng hơi nước',
     12990000, 14900000, 11990000,
     N'ACTIVE', NULL, 24, N'/img/products/cua-ngang-samsung.svg', 143, 2480,
     N'cua_ngang', 10.0, 1400, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 60.0, 67.0, 194.00, 46.00),

    (N'samsung-home-appliance', N'may-giat-cua-ngang', N'WW85T4040CE',
     N'Máy giặt Samsung Inverter 8.5 kg WW85T4040CE', N'may-giat-samsung-ww85t4040ce',
     N'Lựa chọn gọn nhẹ cho căn hộ nhỏ, lồng giặt Diamond Drum bảo vệ sợi vải.',
     N'Lồng Diamond Drum' + CHAR(10) + N'Vệ sinh lồng giặt tự động' + CHAR(10) + N'Giặt nhanh 15 phút',
     7190000, 8490000, NULL,
     N'ACTIVE', NULL, 24, N'/img/products/cua-ngang-samsung.svg', 88, 1320,
     N'cua_ngang', 8.5, 1200, CAST(1 AS bit), N'5 sao', 60.0, 85.0, 55.0, 58.0, 170.00, 44.00),

    (N'samsung-home-appliance', N'may-giat-cua-tren', N'WA11CG5745BV',
     N'Máy giặt Samsung Inverter lồng đứng 11 kg WA11CG5745BV', N'may-giat-samsung-wa11cg5745bv',
     N'Lồng đứng 11 kg, công nghệ Wobble tạo dòng nước đa chiều giúp quần áo ít rối.',
     N'Công nghệ Wobble chống xoắn' + CHAR(10) + N'Lồng giặt tráng kim cương',
     6590000, 7490000, NULL,
     N'ACTIVE', NULL, 24, N'/img/products/cua-tren-samsung.svg', 67, 1040,
     N'cua_tren', 11.0, 700, CAST(1 AS bit), N'4 sao', 54.0, 100.0, 59.0, 39.0, 148.00, 60.00),

    (N'samsung-home-appliance', N'may-giat-mini', N'WW30T4530AW',
     N'Máy giặt Samsung mini 3.5 kg WW30T4530AW', N'may-giat-samsung-ww30t4530aw',
     N'Máy giặt mini 3.5 kg, hợp nhà trọ hoặc giặt riêng đồ em bé.',
     N'Nhỏ gọn, đặt vừa gầm bàn' + CHAR(10) + N'Giặt riêng đồ em bé',
     4290000, NULL, NULL,
     N'PENDING_REVIEW', NULL, 12, N'/img/products/mini-samsung.svg', 0, 8,
     N'cua_ngang', 3.5, 700, CAST(0 AS bit), N'3 sao', 46.0, 62.0, 43.0, 25.0, 98.00, 28.00)
) AS v ([store_slug], [cat_slug], [model_code], [ten], [slug], [mo_ta], [noi_bat],
        [gia], [gia_goc], [gia_km], [trang_thai], [ly_do], [bao_hanh], [anh],
        [da_ban], [luot_xem], [kieu_cua], [dung_tich], [toc_do_vat], [inverter],
        [nhan_nl], [rong], [cao], [sau], [nang], [dien], [nuoc])
JOIN [dbo].[stores]     st ON st.[slug] = v.[store_slug]
JOIN [dbo].[categories] dm ON dm.[slug] = v.[cat_slug]
WHERE NOT EXISTS (
    SELECT 1 FROM [dbo].[products] p
    WHERE p.[store_id] = st.[id] AND p.[model_code] = v.[model_code]);
GO

-- ---------------------------------------------------------------------
-- 3. NGƯỜI DUYỆT cho các sản phẩm đã ACTIVE
-- ---------------------------------------------------------------------
UPDATE p
SET p.[approved_by] = a.[id]
FROM [dbo].[products] p
CROSS JOIN (SELECT [id] FROM [dbo].[users] WHERE [email] = N'admin@smartwash.vn') a
WHERE p.[status] = N'ACTIVE' AND p.[approved_by] IS NULL;
GO

-- ---------------------------------------------------------------------
-- 4. TỒN KHO
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[inventory] ([product_id], [quantity_available], [quantity_reserved],
                               [min_stock], [cost_price])
SELECT p.[id],
       CASE
           WHEN p.[status] = N'ACTIVE'  THEN 8 + (p.[id] % 17)
           WHEN p.[status] = N'DRAFT'   THEN 0
           ELSE 5
       END,
       0,
       3,
       CAST(p.[price] * 0.78 AS bigint)
FROM [dbo].[products] p
WHERE NOT EXISTS (SELECT 1 FROM [dbo].[inventory] i WHERE i.[product_id] = p.[id]);
GO

-- Cho một sản phẩm sắp hết để thấy cảnh báo trên trang người bán
UPDATE [dbo].[inventory]
SET [quantity_available] = 2
WHERE [product_id] IN (SELECT [id] FROM [dbo].[products] WHERE [model_code] = N'FV1409S4W');
GO

-- ---------------------------------------------------------------------
-- 5. ẢNH SẢN PHẨM
--    Đường dẫn bắt đầu bằng / nên được phục vụ thẳng từ static,
--    không đi qua tiền tố /storage/ dành cho ảnh người dùng tải lên.
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[product_images] ([product_id], [path], [sort_order], [is_primary])
SELECT p.[id], p.[image], 0, 1
FROM [dbo].[products] p
WHERE p.[image] IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM [dbo].[product_images] i WHERE i.[product_id] = p.[id]);
GO

-- Ảnh phụ: thêm một góc chụp khác cho hàng đang bán
INSERT INTO [dbo].[product_images] ([product_id], [path], [sort_order], [is_primary])
SELECT p.[id], N'/img/products/mac-dinh.svg', 1, 0
FROM [dbo].[products] p
WHERE p.[status] = N'ACTIVE'
  AND (SELECT COUNT(*) FROM [dbo].[product_images] i WHERE i.[product_id] = p.[id]) = 1;
GO

-- ---------------------------------------------------------------------
-- 6. THÔNG SỐ PHỤ
-- ---------------------------------------------------------------------
INSERT INTO [dbo].[product_specs] ([product_id], [spec_key], [spec_value], [sort_order])
SELECT p.[id], v.[khoa], v.[gia_tri], v.[thu_tu]
FROM [dbo].[products] p
CROSS JOIN (VALUES
    (N'Số chương trình giặt', N'14',                 0),
    (N'Chất liệu lồng giặt',  N'Thép không gỉ',      1),
    (N'Độ ồn khi giặt',       N'52 dB',              2),
    (N'Độ ồn khi vắt',        N'74 dB',              3),
    (N'Xuất xứ',              N'Chính hãng, nhập khẩu', 4)
) AS v ([khoa], [gia_tri], [thu_tu])
WHERE p.[status] IN (N'ACTIVE', N'PENDING_REVIEW')
  AND NOT EXISTS (
      SELECT 1 FROM [dbo].[product_specs] s
      WHERE s.[product_id] = p.[id] AND s.[spec_key] = v.[khoa]);
GO

PRINT N'11 — Xong dữ liệu mẫu giai đoạn 4.';
GO

-- =====================================================================
-- KIỂM TRA
-- =====================================================================
SELECT N'Danh mục' AS [Bảng], COUNT(*) AS [Số dòng] FROM [dbo].[categories]
UNION ALL SELECT N'Sản phẩm',    COUNT(*) FROM [dbo].[products]
UNION ALL SELECT N'Tồn kho',     COUNT(*) FROM [dbo].[inventory]
UNION ALL SELECT N'Ảnh sản phẩm',COUNT(*) FROM [dbo].[product_images]
UNION ALL SELECT N'Thông số',    COUNT(*) FROM [dbo].[product_specs];
GO

-- BR-04: không sản phẩm nào được mang thương hiệu khác thương hiệu gian hàng
SELECT N'Sản phẩm sai thương hiệu (phải bằng 0)' AS [Kiểm tra], COUNT(*) AS [Số dòng]
FROM [dbo].[products] p
JOIN [dbo].[stores] s ON s.[id] = p.[store_id]
WHERE p.[brand_id] <> s.[brand_id];
GO

-- Sản phẩm theo gian hàng và trạng thái
SELECT s.[name] AS [Gian hàng], b.[name] AS [Thương hiệu],
       p.[status] AS [Trạng thái], COUNT(*) AS [Số sản phẩm]
FROM [dbo].[products] p
JOIN [dbo].[stores] s ON s.[id] = p.[store_id]
JOIN [dbo].[brands] b ON b.[id] = p.[brand_id]
GROUP BY s.[name], b.[name], p.[status]
ORDER BY s.[name], p.[status];
GO
