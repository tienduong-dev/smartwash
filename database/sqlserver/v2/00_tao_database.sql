-- =====================================================================
-- SmartWash v2 — Tạo lại database theo đặc tả v1.1
-- Chạy file này ĐẦU TIÊN. Nó xoá sạch database cũ.
-- =====================================================================
USE [master];
GO

IF DB_ID(N'banmayruabat') IS NOT NULL
BEGIN
    ALTER DATABASE [banmayruabat] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [banmayruabat];
END
GO

CREATE DATABASE [banmayruabat] COLLATE Vietnamese_CI_AS;
GO

-- Giảm hiện tượng chờ khoá khi nhiều người cùng đặt hàng:
-- câu đọc lấy bản chụp thay vì chờ câu ghi nhả khoá.
ALTER DATABASE [banmayruabat] SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;
GO

USE [banmayruabat];
GO
SET NOCOUNT ON;
GO

PRINT N'Đã tạo database banmayruabat. Chạy tiếp 01 -> 07.';
GO
