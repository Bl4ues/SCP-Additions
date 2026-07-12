from __future__ import annotations
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def write(path: str, text: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")

def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one replacement, found {count}\n--- OLD ---\n{old}")
    write(path, text.replace(old, new, 1))

def remove_once(path: str, old: str) -> None:
    replace_once(path, old, "")

def ensure_import(path: str, import_line: str, after_import: str) -> None:
    text = read(path)
    if import_line in text:
        return
    if after_import not in text:
        raise RuntimeError(f"{path}: import anchor not found: {after_import}")
    write(path, text.replace(after_import, after_import + import_line, 1))

def matching_paren(text: str, open_index: int) -> int:
    depth = 0
    quote = None
    escaped = False
    for i in range(open_index, len(text)):
        ch = text[i]
        if quote is not None:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == quote:
                quote = None
            continue
        if ch in ('"', "'"):
            quote = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
    raise RuntimeError("Unbalanced Java call")

def top_level_argument_spans(inner: str):
    spans = []
    start = 0
    paren = bracket = brace = 0
    quote = None
    escaped = False
    for i, ch in enumerate(inner):
        if quote is not None:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == quote:
                quote = None
            continue
        if ch in ('"', "'"):
            quote = ch
        elif ch == "(":
            paren += 1
        elif ch == ")":
            paren -= 1
        elif ch == "[":
            bracket += 1
        elif ch == "]":
            bracket -= 1
        elif ch == "{":
            brace += 1
        elif ch == "}":
            brace -= 1
        elif ch == "," and paren == bracket == brace == 0:
            spans.append((start, i))
            start = i + 1
    spans.append((start, len(inner)))
    return spans

def wrap_call_argument(text: str, marker: str, argument_index: int,
                       wrapper: str = "ScpFonts.roboto"):
    cursor = 0
    changed = 0
    while True:
        marker_index = text.find(marker, cursor)
        if marker_index < 0:
            break
        open_index = marker_index + len(marker) - 1
        close_index = matching_paren(text, open_index)
        inner = text[open_index + 1:close_index]
        spans = top_level_argument_spans(inner)
        if argument_index >= len(spans):
            cursor = close_index + 1
            continue
        start, end = spans[argument_index]
        raw = inner[start:end]
        leading = len(raw) - len(raw.lstrip())
        trailing = len(raw) - len(raw.rstrip())
        core_end = len(raw) - trailing if trailing else len(raw)
        core = raw[leading:core_end]
        if not core or "ScpFonts.roboto(" in core:
            cursor = close_index + 1
            continue
        wrapped = raw[:leading] + f"{wrapper}({core})" + (raw[core_end:] if trailing else "")
        new_inner = inner[:start] + wrapped + inner[end:]
        text = text[:open_index + 1] + new_inner + text[close_index:]
        changed += 1
        cursor = open_index + len(new_inner) + 2
    return text, changed

def robotoize(path: str, add_import: bool) -> None:
    text = read(path)
    if add_import and "import com.bl4ues.scpinventory.client.ScpFonts;" not in text:
        package_end = text.find("\n", text.find("package "))
        text = text[:package_end + 1] + "\nimport com.bl4ues.scpinventory.client.ScpFonts;\n" + text[package_end + 1:]
    draws = 0
    for marker in (".drawString(", ".drawCenteredString("):
        text, count = wrap_call_argument(text, marker, 1)
        draws += count
    text, widths = wrap_call_argument(text, ".font.width(", 0)
    if draws == 0 and "ScpFonts.roboto(" not in text:
        raise RuntimeError(f"{path}: no text calls were converted")
    write(path, text)
    print(f"Robotoized {path}: {draws} draws, {widths} widths")
