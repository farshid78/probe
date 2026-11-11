#!/usr/bin/env python3
"""Check semantic text/accent contrast in each built-in Bastehyab theme."""
from pathlib import Path
import re

UI = Path(__file__).resolve().parents[2] / "app/src/main/java/app/bastehyab/UI.java"
source = UI.read_text(encoding="utf-8")


def luminance(hex_color: str) -> float:
    rgb = [int(hex_color[i:i + 2], 16) / 255.0 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in rgb]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def contrast(a: str, b: str) -> float:
    high, low = sorted((luminance(a), luminance(b)), reverse=True)
    return (high + 0.05) / (low + 0.05)


light_start = source.index("if (light) {")
mono_start = source.index("} else if (mono) {", light_start)
dark_start = source.index("} else {", mono_start)
end = source.index("        }\n        if (BODY", dark_start)
blocks = {
    "light": source[light_start:mono_start],
    "mono": source[mono_start:dark_start],
    "dark": source[dark_start:end],
}

for theme, block in blocks.items():
    values = {}
    for token in ("CARD", "TEXT", "MUTED", "PRIMARY", "ACCENT", "DANGER", "SUCCESS"):
        match = re.search(rf"\b{token}\s*=\s*(0xFF[0-9A-Fa-f]{{6}})", block)
        if not match:
            raise SystemExit(f"palette token {token} missing in {theme} theme")
        values[token] = "#" + match.group(1)[4:]
    for token in ("TEXT", "MUTED", "PRIMARY", "ACCENT", "DANGER", "SUCCESS"):
        value = contrast(values[token], values["CARD"])
        if value < 4.5:
            raise SystemExit(f"{theme} {token} contrast is {value:.2f}:1 (requires 4.5:1)")
    button_text = "#182338" if luminance(values["PRIMARY"]) > 0.42 else "#FFFFFF"
    button_ratio = contrast(button_text, values["PRIMARY"])
    if button_ratio < 4.5:
        raise SystemExit(f"{theme} primary button text contrast is {button_ratio:.2f}:1")
    print(f"PASS: {theme} palette contrast (minimum text/token contrast >= 4.5:1)")
