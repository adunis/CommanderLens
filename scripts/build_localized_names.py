#!/usr/bin/env python3
"""Generate a small Italian/Spanish -> English index from MTGJSON AtomicCards.

No runtime network or full dataset is needed on the phone. The GitHub workflow
fetches AtomicCards once, builds a compact TSV, and bundles that TSV in the APK.
"""
import argparse
import json
import lzma
import pathlib
import tempfile
import urllib.request
from collections import Counter

SOURCE = "https://mtgjson.com/api/v5/AtomicCards.json.xz"
LANGUAGES = {"italian": "it", "it": "it", "spanish": "es", "es": "es"}


def rows_from_data(data):
    rows = set()
    for english_key, printings in data.items():
        if not isinstance(printings, list):
            continue
        for card in printings:
            canonical = card.get("name") or english_key
            if not isinstance(canonical, str) or not canonical.strip():
                continue
            for foreign in card.get("foreignData") or []:
                lang = LANGUAGES.get(str(foreign.get("language", "")).lower())
                if not lang:
                    continue
                for localized in (foreign.get("name"), foreign.get("faceName")):
                    if not isinstance(localized, str) or not localized.strip():
                        continue
                    # A printed DFC name can contain both sides; either face may be scanned.
                    for face in localized.split(" // "):
                        if face.strip():
                            rows.add((lang, face.strip(), canonical))
    return sorted(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=pathlib.Path, help="Optional MTGJSON file for tests")
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--allow-small", action="store_true", help="Only for test fixtures")
    args = parser.parse_args()

    if args.input is None:
        with tempfile.TemporaryDirectory() as directory:
            archive = pathlib.Path(directory) / "AtomicCards.json.xz"
            req = urllib.request.Request(SOURCE, headers={"User-Agent": "CommanderLens-build/0.2"})
            with urllib.request.urlopen(req, timeout=240) as response, archive.open("wb") as target:
                import shutil
                shutil.copyfileobj(response, target)
            with lzma.open(archive, "rt", encoding="utf-8") as source:
                data = json.load(source)["data"]
    elif args.input.suffix == ".xz":
        with lzma.open(args.input, "rt", encoding="utf-8") as source:
            data = json.load(source)["data"]
    else:
        with args.input.open(encoding="utf-8") as source:
            data = json.load(source)["data"]

    rows = rows_from_data(data)
    counts = Counter(lang for lang, _, _ in rows)
    print("Localized entries:", dict(counts))
    if not args.allow_small and min(counts.get("it", 0), counts.get("es", 0)) < 1000:
        raise RuntimeError("Localized index unexpectedly small; refusing to build a broken APK")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as target:
        for lang, localized, canonical in rows:
            target.write(f"{lang}\t{localized}\t{canonical}\n")
    print(f"Wrote {len(rows):,} entries to {args.output} ({args.output.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
