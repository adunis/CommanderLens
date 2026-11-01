#!/usr/bin/env python3
import json
import subprocess
import sys
import tempfile
from pathlib import Path

script = Path(__file__).with_name("build_localized_names.py")
fixture = {"data": {"Sol Ring": [{"name": "Sol Ring", "foreignData": [
    {"language": "Italian", "name": "Anello Solare"},
    {"language": "Spanish", "name": "Anillo solar"},
    {"language": "French", "name": "Anneau solaire"},
]}], "Island": [{"name": "Island", "foreignData": [
    {"language": "Italian", "name": "Isola"},
    {"language": "Spanish", "name": "Isla"},
]}]}}
with tempfile.TemporaryDirectory() as d:
    source, destination = Path(d) / "fixture.json", Path(d) / "index.tsv"
    source.write_text(json.dumps(fixture), encoding="utf-8")
    subprocess.run([sys.executable, str(script), "--input", str(source), "--output", str(destination), "--allow-small"], check=True)
    lines = destination.read_text(encoding="utf-8").splitlines()
    assert "it\tAnello Solare\tSol Ring" in lines
    assert "es\tAnillo solar\tSol Ring" in lines
    assert "it\tIsola\tIsland" in lines
    assert not any("Anneau solaire" in row for row in lines)
print("PASS: Italian/Spanish MTGJSON index generator")
