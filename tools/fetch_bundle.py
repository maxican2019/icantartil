#!/usr/bin/env python3
"""Unduh teks 114 surat dari EQuran.id menjadi app/src/main/assets/quran_bundle.json.
Bundel ini jadi cadangan offline di dalam APK (tafsir tidak ikut). File TIDAK di-commit (lihat .gitignore).
Jalankan lokal:  python3 tools/fetch_bundle.py
Lewati bundel:   hapus langkah 'Unduh bundel' di .github/workflows/android.yml (aplikasi lalu mengunduh saat pertama dibuka)."""
import json, pathlib, sys, time, urllib.request

BASE = "https://equran.id/api/v2"
QARI = "05"  # qari bawaan; qari lain diambil saat pengguna memperbarui data dari server
OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/quran_bundle.json"


def get(path, tries=4):
    for i in range(tries):
        try:
            req = urllib.request.Request(BASE + path, headers={"User-Agent": "IcanTartil-bundle/0.1"})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)["data"]
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(1.5 * (i + 1))


def main():
    lst = get("/surat")
    if len(lst) != 114:
        raise RuntimeError(f"jumlah surat {len(lst)}, harusnya 114")
    keep = ["nomor", "nama", "namaLatin", "arti", "jumlahAyat"]
    out = {"list": [{k: s.get(k) for k in keep} for s in lst], "detail": {}}
    for s in lst:
        n = s["nomor"]
        ayat = get(f"/surat/{n}")["ayat"]
        rows = []
        for a in ayat:
            url = (a.get("audio") or {}).get(QARI)
            rows.append({
                "nomorAyat": a["nomorAyat"], "teksArab": a["teksArab"],
                "teksLatin": a.get("teksLatin", ""), "teksIndonesia": a.get("teksIndonesia", ""),
                "audio": {QARI: url} if url else {},
            })
        if len(rows) != s["jumlahAyat"]:
            raise RuntimeError(f"surat {n}: {len(rows)} ayat, harusnya {s['jumlahAyat']}")
        out["detail"][str(n)] = rows
        print(f"surat {n}/114", flush=True)
        time.sleep(0.25)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("selesai:", OUT, OUT.stat().st_size, "byte")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("GAGAL:", e, file=sys.stderr)
        OUT.unlink(missing_ok=True)
        sys.exit(1)
