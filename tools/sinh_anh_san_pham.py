#!/usr/bin/env python3
"""Sinh bộ ảnh minh hoạ sản phẩm dạng SVG.

Đây là ảnh TẠM, vẽ chung chung theo kiểu máy, KHÔNG mô phỏng mẫu mã thật của
hãng nào. Người bán tải ảnh thật lên là ảnh này bị thay.

Chạy:  python3 tools/sinh_anh_san_pham.py
"""
import os

RA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                  "src/main/resources/static/img/products")

INK   = "#0b0b0c"
THAN  = "#e8e8e6"   # vỏ máy
SAU   = "#d6d6d3"   # mặt bên, tối hơn để tạo khối
KINH  = "#9fb3c8"   # kính cửa
TRANG = "#ffffff"
MO    = "#c7c7c4"

# Màu nhấn riêng từng hãng — chỉ là điểm nhỏ trên bảng điều khiển,
# không phải màu thương hiệu chính thức.
HANG = {
    "lg":         "#a50034", "samsung":  "#1428a0", "electrolux": "#0d2d6c",
    "panasonic":  "#0f4c9c", "toshiba":  "#d10a11", "hitachi":    "#c8102e",
    "sharp":      "#b00000", "aqua":     "#00a0b0", "beko":       "#004b93",
    "casper":     "#e2231a", "midea":    "#0f62fe", "xiaomi":     "#ff6900",
    "smartwash":  "#fe5313",
}


def khung(noi_dung):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" '
            'width="400" height="400" role="img">\n' + noi_dung + '\n</svg>\n')


def cua_ngang(m):
    """Máy giặt lồng ngang: thân hộp, cửa tròn lớn chính diện."""
    return khung(f'''
  <rect x="86" y="70" width="228" height="268" rx="14" fill="{THAN}"/>
  <rect x="86" y="70" width="18" height="268" rx="14" fill="{SAU}"/>
  <rect x="100" y="86" width="200" height="40" rx="8" fill="{TRANG}"/>
  <rect x="116" y="100" width="86" height="12" rx="6" fill="{MO}"/>
  <circle cx="276" cy="106" r="9" fill="{m}"/>
  <circle cx="250" cy="106" r="5" fill="{MO}"/>
  <circle cx="200" cy="232" r="84" fill="{SAU}"/>
  <circle cx="200" cy="232" r="72" fill="{TRANG}"/>
  <circle cx="200" cy="232" r="60" fill="{KINH}" opacity="0.55"/>
  <circle cx="200" cy="232" r="60" fill="none" stroke="{INK}" stroke-width="2.5" opacity="0.35"/>
  <path d="M164 206a60 60 0 0 1 34-20" stroke="{TRANG}" stroke-width="7"
        stroke-linecap="round" fill="none" opacity="0.85"/>
  <rect x="104" y="338" width="22" height="14" rx="4" fill="{SAU}"/>
  <rect x="274" y="338" width="22" height="14" rx="4" fill="{SAU}"/>''')


def cua_tren(m):
    """Máy giặt lồng đứng nhìn chếch từ trên xuống.

    Dựng khối bằng ba mảng: bảng điều khiển dựng đứng phía sau, mặt nắp là
    hình thang (xa hẹp, gần rộng) để tạo phối cảnh, rồi mới tới thân máy.
    """
    return khung(f'''
  <rect x="132" y="86" width="136" height="24" rx="6" fill="{TRANG}"/>
  <rect x="132" y="86" width="136" height="24" rx="6" fill="none"
        stroke="{MO}" stroke-width="1.5"/>
  <circle cx="250" cy="98" r="7" fill="{m}"/>
  <rect x="146" y="94" width="52" height="9" rx="4.5" fill="{MO}"/>

  <path d="M130 110 L270 110 L302 146 L98 146 Z" fill="{THAN}"/>
  <path d="M130 110 L270 110 L302 146 L98 146 Z" fill="none"
        stroke="{MO}" stroke-width="1.5"/>
  <ellipse cx="200" cy="130" rx="62" ry="16" fill="{SAU}"/>
  <ellipse cx="200" cy="130" rx="54" ry="12" fill="{KINH}" opacity="0.5"/>
  <ellipse cx="200" cy="130" rx="54" ry="12" fill="none" stroke="{INK}"
           stroke-width="2" opacity="0.3"/>
  <path d="M168 126a54 12 0 0 1 20-7" stroke="{TRANG}" stroke-width="4"
        stroke-linecap="round" fill="none" opacity="0.9"/>

  <rect x="98" y="146" width="204" height="192" rx="10" fill="{THAN}"/>
  <rect x="98" y="146" width="16" height="192" rx="10" fill="{SAU}"/>
  <rect x="98" y="146" width="204" height="192" rx="10" fill="none"
        stroke="{MO}" stroke-width="1.5"/>
  <rect x="112" y="340" width="22" height="13" rx="4" fill="{SAU}"/>
  <rect x="266" y="340" width="22" height="13" rx="4" fill="{SAU}"/>''')


def may_say(m):
    """Máy sấy: giống cửa ngang nhưng có lưới thoát hơi, cửa nhỏ hơn."""
    hoi = "".join(
        f'<rect x="{150 + i * 20}" y="312" width="12" height="4" rx="2" fill="{MO}"/>'
        for i in range(6))
    return khung(f'''
  <rect x="86" y="70" width="228" height="268" rx="14" fill="{THAN}"/>
  <rect x="86" y="70" width="18" height="268" rx="14" fill="{SAU}"/>
  <rect x="100" y="86" width="200" height="38" rx="8" fill="{TRANG}"/>
  <circle cx="276" cy="105" r="9" fill="{m}"/>
  <rect x="116" y="99" width="70" height="12" rx="6" fill="{MO}"/>
  <circle cx="200" cy="218" r="76" fill="{SAU}"/>
  <circle cx="200" cy="218" r="64" fill="{TRANG}"/>
  <circle cx="200" cy="218" r="52" fill="{KINH}" opacity="0.45"/>
  <circle cx="200" cy="218" r="52" fill="none" stroke="{INK}" stroke-width="2.5" opacity="0.3"/>
  <path d="M184 196c10 10 22 10 32 0" stroke="{TRANG}" stroke-width="6"
        stroke-linecap="round" fill="none" opacity="0.8"/>
  {hoi}
  <rect x="104" y="338" width="22" height="14" rx="4" fill="{SAU}"/>
  <rect x="274" y="338" width="22" height="14" rx="4" fill="{SAU}"/>''')


def thap_giat_say(m):
    """Tháp giặt sấy: hai khoang chồng nhau."""
    return khung(f'''
  <rect x="108" y="40" width="184" height="150" rx="12" fill="{THAN}"/>
  <rect x="108" y="40" width="14" height="150" rx="12" fill="{SAU}"/>
  <rect x="120" y="52" width="160" height="28" rx="7" fill="{TRANG}"/>
  <circle cx="262" cy="66" r="7" fill="{m}"/>
  <circle cx="200" cy="134" r="46" fill="{SAU}"/>
  <circle cx="200" cy="134" r="37" fill="{TRANG}"/>
  <circle cx="200" cy="134" r="29" fill="{KINH}" opacity="0.5"/>

  <rect x="108" y="196" width="184" height="162" rx="12" fill="{THAN}"/>
  <rect x="108" y="196" width="14" height="162" rx="12" fill="{SAU}"/>
  <rect x="120" y="208" width="160" height="28" rx="7" fill="{TRANG}"/>
  <circle cx="262" cy="222" r="7" fill="{MO}"/>
  <circle cx="200" cy="292" r="48" fill="{SAU}"/>
  <circle cx="200" cy="292" r="39" fill="{TRANG}"/>
  <circle cx="200" cy="292" r="31" fill="{KINH}" opacity="0.5"/>
  <circle cx="200" cy="292" r="31" fill="none" stroke="{INK}" stroke-width="2" opacity="0.3"/>''')


def mini(m):
    """Máy giặt mini: thân thấp, vuông vắn."""
    return khung(f'''
  <rect x="104" y="120" width="192" height="196" rx="14" fill="{THAN}"/>
  <rect x="104" y="120" width="16" height="196" rx="14" fill="{SAU}"/>
  <rect x="118" y="134" width="164" height="34" rx="8" fill="{TRANG}"/>
  <circle cx="258" cy="151" r="8" fill="{m}"/>
  <rect x="134" y="146" width="62" height="10" rx="5" fill="{MO}"/>
  <circle cx="200" cy="244" r="58" fill="{SAU}"/>
  <circle cx="200" cy="244" r="48" fill="{TRANG}"/>
  <circle cx="200" cy="244" r="38" fill="{KINH}" opacity="0.5"/>
  <circle cx="200" cy="244" r="38" fill="none" stroke="{INK}" stroke-width="2" opacity="0.3"/>
  <rect x="120" y="316" width="20" height="12" rx="4" fill="{SAU}"/>
  <rect x="260" y="316" width="20" height="12" rx="4" fill="{SAU}"/>''')


def cong_nghiep(m):
    """Máy giặt công nghiệp: thân to, chân đế cao, cửa thép."""
    return khung(f'''
  <rect x="70" y="56" width="260" height="268" rx="10" fill="{THAN}"/>
  <rect x="70" y="56" width="20" height="268" rx="10" fill="{SAU}"/>
  <rect x="86" y="70" width="228" height="46" rx="6" fill="{TRANG}"/>
  <rect x="104" y="86" width="96" height="14" rx="7" fill="{MO}"/>
  <circle cx="286" cy="93" r="10" fill="{m}"/>
  <circle cx="256" cy="93" r="6" fill="{MO}"/>
  <circle cx="200" cy="226" r="92" fill="{SAU}"/>
  <circle cx="200" cy="226" r="80" fill="{MO}"/>
  <circle cx="200" cy="226" r="66" fill="{KINH}" opacity="0.55"/>
  <circle cx="200" cy="226" r="66" fill="none" stroke="{INK}" stroke-width="3" opacity="0.4"/>
  <circle cx="200" cy="226" r="80" fill="none" stroke="{INK}" stroke-width="2" opacity="0.2"/>
  <rect x="78" y="324" width="34" height="30" rx="4" fill="{SAU}"/>
  <rect x="288" y="324" width="34" height="30" rx="4" fill="{SAU}"/>''')


KIEU = {
    "cua-ngang":     cua_ngang,
    "cua-tren":      cua_tren,
    "may-say":       may_say,
    "thap-giat-say": thap_giat_say,
    "mini":          mini,
    "cong-nghiep":   cong_nghiep,
}


def main():
    os.makedirs(RA, exist_ok=True)
    n = 0
    for ten_kieu, ve in KIEU.items():
        for hang, mau in HANG.items():
            open(os.path.join(RA, f"{ten_kieu}-{hang}.svg"), "w", encoding="utf-8").write(ve(mau))
            n += 1
    # Ảnh dùng khi sản phẩm chưa có ảnh nào
    open(os.path.join(RA, "mac-dinh.svg"), "w", encoding="utf-8").write(cua_ngang(MO))
    n += 1
    print("Đã sinh %d ảnh vào %s" % (n, RA))


if __name__ == "__main__":
    main()
