# SmartWash — Sàn thương mại điện tử máy giặt

Đồ án môn học. Sàn nhiều người bán chuyên máy giặt: khách mua hàng từ các gian
hàng uỷ quyền, mỗi gian hàng bán đúng một thương hiệu.

Xây bằng **Spring Boot MVC + Thymeleaf + SQL Server**.

---

## 1. Công nghệ

| Thành phần | Dùng gì |
|---|---|
| Nền tảng | Spring Boot 3.5.6, Java 17 |
| Giao diện | Thymeleaf + Layout Dialect, Tailwind CSS v4 |
| Dữ liệu | SQL Server 2022, Spring Data JPA (Hibernate 6) |
| Bảo mật | Spring Security — chống CSRF, băm mật khẩu BCrypt |
| Quản lý thư viện | Maven |

Phân quyền và phiên đăng nhập tự viết bằng `HandlerInterceptor` thay vì dùng
form login của Spring Security, để kiểm soát được từng nhóm đường dẫn và báo
lỗi bằng tiếng Việt.

---

## 2. Cài đặt

### 2.1. Cần có trước

- JDK 17 trở lên
- IntelliJ IDEA (Community là đủ)
- SQL Server 2022 (bản Express là đủ) + SSMS

### 2.2. Tạo tài khoản SQL Server cho ứng dụng

Mở SSMS, kết nối `localhost,1433`, chạy:

```sql
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'smartwash')
    CREATE LOGIN [smartwash] WITH PASSWORD = N'SmartWash@2026',
        CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
GO
ALTER SERVER ROLE [sysadmin] ADD MEMBER [smartwash];
```

Đổi tên hoặc mật khẩu thì sửa lại cho khớp trong
`src/main/resources/application-sqlserver.properties`.

### 2.3. Tạo cơ sở dữ liệu

Chạy lần lượt các file trong `database/sqlserver/v2/`, **đúng thứ tự** vì file
sau phụ thuộc bảng của file trước. Mỗi file: SSMS → `File → Open → File…` → F5.

| Thứ tự | File | Nội dung |
|---|---|---|
| 1 | `00_tao_database.sql` | Tạo database, đặt collation `Vietnamese_CI_AS` |
| 2 | `01_tai_khoan_phan_quyen.sql` | Tài khoản, vai trò, địa chỉ, thông báo, nhật ký |
| 3 | `02_doanh_nghiep_gian_hang.sql` | Doanh nghiệp, thương hiệu, hồ sơ mở gian hàng, gian hàng |
| 4 | `03_danh_muc_san_pham.sql` | Danh mục, sản phẩm, thông số, ảnh, tồn kho |
| 5 | `04_don_hang_thanh_toan.sql` | Giỏ hàng, đơn hàng hai cấp, thanh toán, vận chuyển |
| 6 | `05_khuyen_mai_phi_san.sql` | Mã giảm giá, hoa hồng, đối soát |
| 7 | `06_hau_mai.sql` | Đánh giá, phiếu hỗ trợ, đổi trả, bảo hành, khiếu nại |
| 8 | `07_mo_rong_crm_kho.sql` | CRM và kho nhập/xuất |
| 9 | `10_du_lieu_mau_gd3.sql` | Dữ liệu mẫu: tài khoản, thương hiệu, gian hàng |
| 10 | `11_du_lieu_mau_gd4.sql` | Dữ liệu mẫu: danh mục, sản phẩm, tồn kho |

Hai file dữ liệu mẫu chạy lại nhiều lần không sao — mỗi khối đều kiểm tra tồn
tại trước khi chèn.

Kiểm tra nhanh sau khi chạy xong:

```sql
USE banmayruabat;
SELECT COUNT(*) AS so_bang FROM sys.tables;   -- 52
SELECT COUNT(*) AS san_pham FROM products;    -- 12
```

### 2.4. Chạy ứng dụng

Trong IntelliJ: mở project, đợi Maven tải thư viện xong, chạy
`SmartWashApplication`.

Hoặc dòng lệnh:

```bash
mvn spring-boot:run
```

Mở `http://localhost:8000`.

> File CSS đã biên dịch sẵn ở `src/main/resources/static/css/app.css` nên
> **không cần cài Node** để chạy. Chỉ khi sửa template mới cần
> `npm install && npm run css` để Tailwind quét lại các lớp đang dùng.

---

## 3. Tài khoản thử

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `admin@smartwash.vn` | `Admin@2026` | Quản trị viên |
| `seller.lg@smartwash.vn` | `Seller@2026` | Chủ 2 gian hàng (LG, Toshiba) |
| `seller.sam@smartwash.vn` | `Seller@2026` | Chủ gian hàng Samsung |
| `nhanvien@smartwash.vn` | `Seller@2026` | Nhân viên gian hàng LG |
| `khach@smartwash.vn` | `Khach@2026` | Khách hàng |
| `ungvien@smartwash.vn` | `Seller@2026` | Đang có hồ sơ mở gian hàng chờ duyệt |

---

## 4. Cấu trúc mã nguồn

Chia theo **miền nghiệp vụ**, không chia theo tầng kỹ thuật. Mỗi gói chứa đủ
entity, repository, service và controller của miền đó, nên sửa một chức năng
chỉ phải mở một thư mục.

```
src/main/java/com/smartwash/
├── common/              tiện ích dùng chung, lưu file, lỗi nghiệp vụ
├── config/              phân quyền, xử lý lỗi, biến dùng chung cho view
├── auth/                đăng nhập, đăng xuất, quên mật khẩu
├── user/                tài khoản, vai trò, sổ địa chỉ
├── business/            doanh nghiệp
├── brand/               thương hiệu + chuẩn hoá tên để dò trùng
├── sellerregistration/  hồ sơ mở gian hàng và thẩm định
├── store/               gian hàng và nhân sự gian hàng
├── category/            danh mục dạng cây
├── product/             sản phẩm, ảnh, thông số, kiểm duyệt, tra cứu
├── inventory/           tồn kho
├── notification/        thông báo
├── audit/               nhật ký thao tác
├── setting/             cấu hình sàn
└── admin/               các màn hình quản trị
```

---

## 5. Vài quy tắc nghiệp vụ đáng chú ý

**Mỗi gian hàng bán đúng một thương hiệu.** Sản phẩm luôn mang thương hiệu của
gian hàng, chặn ở ba lớp: form không có ô chọn thương hiệu, service gán thẳng
từ `store.getBrand()`, và trigger `TR_products_brand_khop_store` chặn ở tầng
cơ sở dữ liệu.

**Dò trùng thương hiệu không tự chặn.** `BrandNameNormalizer` bỏ dấu, viết hoa,
bỏ đuôi pháp nhân (`CÔNG TY TNHH`, `JSC`, `VIỆT NAM`) để sinh `normalized_name`,
nhờ đó "Electrolux" và "Công ty TNHH Electrolux Việt Nam" ra cùng một khoá.
Nhưng hệ thống chỉ **cảnh báo** cho quản trị viên quyết định, vì hai thương
hiệu trùng tên vẫn có thể là hai hãng khác nhau thật.

**Thông số sản phẩm chia hai nơi.** Những gì khách dùng để *lọc* và *so sánh*
(kiểu cửa, khối lượng giặt, tốc độ vắt, Inverter, kích thước) là cột thật có
đánh chỉ mục; phần còn lại nằm ở `product_specs` dạng khoá/giá trị. Để tất cả
dạng khoá/giá trị thì không lọc theo khoảng số được.

**Tồn kho tách tồn thực và phần giữ chỗ.** `quantity_available` là hàng trong
kho, `quantity_reserved` là phần đã giữ cho đơn chưa hoàn tất. Bán được là
hiệu hai số — nhờ vậy huỷ đơn chỉ cần nhả chỗ, không phải cộng lại kho.

Chi tiết đầy đủ về cấu trúc bảng, các mâu thuẫn trong đặc tả và cách chốt:
xem `TAI-LIEU/01-thiet-ke-du-lieu.md`.

---

## 6. Trạng thái

**Đã xong**

- Tài khoản, nhiều vai trò trên một tài khoản, khoá tạm khi đăng nhập sai nhiều lần
- Doanh nghiệp, thương hiệu, hồ sơ mở gian hàng và luồng thẩm định
- Gian hàng, phân quyền nhân sự theo từng chức năng
- Danh mục cây, sản phẩm, nhiều ảnh, thông số, tồn kho, kiểm duyệt sản phẩm
- Tìm kiếm và lọc sản phẩm cho khách
- Nhật ký thao tác, cấu hình sàn

**Đang làm**

- Giỏ hàng, đặt hàng nhiều gian hàng, thanh toán, vận chuyển
- Mã giảm giá, hoa hồng, đối soát
- Đánh giá, phiếu hỗ trợ, đổi trả, bảo hành
- Báo cáo

Các đường dẫn `/cart`, `/orders`, `/support` đã có sẵn trong menu nhưng chưa
nối controller nên hiện trả 404.

---

## 7. Ghi chú

Ảnh sản phẩm trong `static/img/products/` là ảnh minh hoạ vẽ bằng SVG, chỉ thể
hiện kiểu máy chứ không mô phỏng mẫu mã thật của hãng nào. Sinh lại bằng
`python3 tools/sinh_anh_san_pham.py`. Người bán tải ảnh thật lên là ảnh này
được thay.

Mật khẩu cơ sở dữ liệu trong `application-sqlserver.properties` là tài khoản
cục bộ dùng cho môi trường học tập. Khi triển khai thật phải chuyển sang biến
môi trường.
