#!/usr/bin/env python3
"""Periksa pustaka native 64-bit di APK: segmen PT_LOAD harus rata minimal 16 KB (0x4000) untuk Google Play.
Pemakaian: python3 tools/check_16kb.py app/build/outputs/apk/release/app-release.apk"""
import struct, sys, zipfile

def load_aligns(b):
    if b[:4] != b"\x7fELF" or b[5] != 1:
        return None
    if b[4] == 2:  # ELF64
        phoff = struct.unpack_from("<Q", b, 0x20)[0]; phentsize, phnum = struct.unpack_from("<HH", b, 0x36)
        return [struct.unpack_from("<Q", b, phoff + i * phentsize + 48)[0]
                for i in range(phnum) if struct.unpack_from("<I", b, phoff + i * phentsize)[0] == 1]
    phoff = struct.unpack_from("<I", b, 0x1C)[0]; phentsize, phnum = struct.unpack_from("<HH", b, 0x2A)
    return [struct.unpack_from("<I", b, phoff + i * phentsize + 28)[0]
            for i in range(phnum) if struct.unpack_from("<I", b, phoff + i * phentsize)[0] == 1]

def main(paths):
    bad = 0; seen = 0
    for path in paths:
        with zipfile.ZipFile(path) as z:
            for name in z.namelist():
                if not name.endswith(".so") or not (name.startswith("lib/arm64-v8a/") or name.startswith("lib/x86_64/")):
                    continue
                al = load_aligns(z.read(name)); seen += 1
                ok = bool(al) and min(al) >= 0x4000
                print(("OK    " if ok else "GAGAL ") + name + f"  align={[hex(a) for a in (al or [])]}")
                bad += 0 if ok else 1
    print(f"\n{seen} pustaka 64-bit diperiksa, {bad} belum 16 KB." if seen else "Tidak ada pustaka native 64-bit.")
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]) if len(sys.argv) > 1 else 2)
