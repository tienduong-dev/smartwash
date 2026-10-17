#!/usr/bin/env python3
"""Kiểm tra bộ DDL trước khi chạy trên SQL Server.

Bắt 5 loại lỗi hay gặp, trong đó 2 loại SQL Server chỉ báo khi chạy tới nơi:
  1. Tên ràng buộc trùng  -> Msg 2714 (tên ràng buộc duy nhất trên TOÀN database)
  2. Nhiều đường cascade  -> Msg 1785
  3. Khoá ngoại trỏ tới bảng không tồn tại
  4. Tên bảng trùng
  5. Chỉ mục trùng tên trong cùng một bảng

Chạy:  python3 kiem_tra_ddl.py
"""
import collections
import glob
import os
import re
import sys

THU_MUC = os.path.dirname(os.path.abspath(__file__))


def doc_tat_ca():
    noi_dung = {}
    for f in sorted(glob.glob(os.path.join(THU_MUC, "*.sql"))):
        noi_dung[os.path.basename(f)] = open(f, encoding="utf-8").read()
    return noi_dung


def bang_tai_vi_tri(text):
    moc = [(m.start(), m.group(1)) for m in re.finditer(r"CREATE TABLE \[dbo\]\.\[(\w+)\]", text)]

    def tra(pos):
        cur = "?"
        for p, n in moc:
            if p <= pos:
                cur = n
            else:
                break
        return cur

    return tra


def main():
    files = doc_tat_ca()
    tat_ca = "\n".join(files.values())
    loi = []

    # --- 4. tên bảng trùng -------------------------------------------------
    tables = re.findall(r"CREATE TABLE \[dbo\]\.\[(\w+)\]", tat_ca)
    trung_bang = [t for t, c in collections.Counter(tables).items() if c > 1]
    if trung_bang:
        loi.append("Tên bảng trùng: %s" % trung_bang)

    # --- 1. tên ràng buộc trùng -------------------------------------------
    ten = collections.defaultdict(list)
    for fname, text in files.items():
        tra = bang_tai_vi_tri(text)
        for m in re.finditer(r"CONSTRAINT \[(\w+)\]", text):
            ten[m.group(1)].append("%s/%s" % (fname, tra(m.start())))
        for m in re.finditer(r"CREATE TRIGGER \[dbo\]\.\[(\w+)\]", text):
            ten[m.group(1)].append("%s/TRIGGER" % fname)
    for k, v in sorted(ten.items()):
        if len(v) > 1:
            loi.append("Tên ràng buộc trùng '%s' tại %s" % (k, v))

    # --- 5. chỉ mục trùng trong cùng bảng ---------------------------------
    idx = collections.Counter()
    for text in files.values():
        for m in re.finditer(r"CREATE (?:UNIQUE )?INDEX \[(\w+)\] ON \[dbo\]\.\[(\w+)\]", text):
            idx[(m.group(2), m.group(1))] += 1
    for (bang, ten_idx), c in idx.items():
        if c > 1:
            loi.append("Chỉ mục '%s' khai báo %d lần trên bảng %s" % (ten_idx, c, bang))

    # --- đọc khoá ngoại ----------------------------------------------------
    fks = []
    for m in re.finditer(r"CREATE TABLE \[dbo\]\.\[(\w+)\] \((.*?)\n\);", tat_ca, re.S):
        con, than = m.group(1), m.group(2)
        for fm in re.finditer(
            r"FOREIGN KEY \(\[(\w+)\]\)\s*\n?\s*REFERENCES \[dbo\]\.\[(\w+)\] \(\[(\w+)\]\)([^,\n]*)", than
        ):
            fks.append((con, fm.group(1), fm.group(2), fm.group(4)))
    for m in re.finditer(
        r"ALTER TABLE \[dbo\]\.\[(\w+)\]\s*\n?\s*ADD CONSTRAINT \[\w+\] FOREIGN KEY \(\[(\w+)\]\) "
        r"REFERENCES \[dbo\]\.\[(\w+)\] \(\[(\w+)\]\)([^;]*)",
        tat_ca,
    ):
        fks.append((m.group(1), m.group(2), m.group(3), m.group(5)))

    # --- 3. khoá ngoại trỏ tới bảng không có ------------------------------
    for con, cot, cha, _ in fks:
        if cha not in tables:
            loi.append("Khoá ngoại %s.%s trỏ tới bảng không tồn tại: %s" % (con, cot, cha))

    # --- 2. nhiều đường cascade / chu trình -------------------------------
    canh = collections.defaultdict(set)
    for con, _, cha, act in fks:
        a = act.upper()
        if "CASCADE" in a or "SET NULL" in a:
            canh[cha].add(con)

    for goc in list(canh):
        dem = collections.Counter()
        ngan_xep = [(goc, frozenset([goc]))]
        while ngan_xep:
            nut, tham = ngan_xep.pop()
            for ke in canh.get(nut, ()):
                if ke in tham:
                    loi.append("Chu trình cascade: %s -> %s" % (nut, ke))
                    continue
                dem[ke] += 1
                ngan_xep.append((ke, tham | {ke}))
        for t, n in dem.items():
            if n > 1:
                loi.append("Nhiều đường cascade: từ %s tới %s có %d đường (SQL Server báo lỗi 1785)" % (goc, t, n))

    # --- báo cáo -----------------------------------------------------------
    print("Số file          : %d" % len(files))
    print("Số bảng          : %d" % len(tables))
    print("Số khoá ngoại    : %d" % len(fks))
    print("Số tên ràng buộc : %d" % len(ten))
    print()
    if loi:
        print("CÓ %d LỖI:" % len(loi))
        for l in sorted(set(loi)):
            print("  - %s" % l)
        return 1
    print("Không phát hiện lỗi. DDL sẵn sàng chạy trên SQL Server.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
