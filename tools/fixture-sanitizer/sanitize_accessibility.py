#!/usr/bin/env python3
"""Create a privacy-reduced copy of a Bastehyab accessibility-tree fixture.

The source file is never modified. This tool is a defence-in-depth helper, not a
proof that a dump is anonymous; review every sanitized fixture before sharing it.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Dict, Tuple

ATTR = re.compile(r'(\w+)="((?:[^"\\]|\\.)*)"')
PHONE = re.compile(r'(?<!\d)(?:(?:\+|00)?98|0)?9[0-9۰-۹]{9}(?!\d)')
EMAIL = re.compile(r'(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b')
UUID = re.compile(r'(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b')
LONG_NUMBER = re.compile(r'(?<!\d)[0-9۰-۹]{11,}(?!\d)')
OPAQUE = re.compile(r'(?<![A-Za-z0-9])[A-Za-z0-9_-]{28,}(?![A-Za-z0-9])')
SENSITIVE_WORDS = re.compile(
    r'(?i)(password|passcode|otp|verification|account\s*(?:id|number)|national\s*id|'
    r'card\s*number|رمز|گذرواژه|کد\s*(?:تأیید|تایید|ملی)|شناسه\s*(?:حساب|کاربری)|'
    r'شماره\s*(?:حساب|کارت|موبایل|تلفن)|اطلاعات\s*بانکی)'
)
EDITABLE_OR_SECRET = re.compile(r'(?i)(?:password|editable|isPassword|inputType=password|secureTextEntry)\s*[=:]\s*(?:1|true|yes|password)', re.I)


def decode_attr(raw: str) -> str:
    return raw.replace(r'\"', '"').replace(r'\\', '\\')


def encode_attr(value: str) -> str:
    return value.replace('\\', r'\\').replace('"', r'\"').replace('\n', r'\n').replace('\r', r'\r')


def scrub_value(value: str, stats: Dict[str, int], force: bool) -> str:
    if force or SENSITIVE_WORDS.search(value):
        stats['sensitive-field'] = stats.get('sensitive-field', 0) + 1
        return '[REDACTED]'

    patterns: Tuple[Tuple[str, re.Pattern[str]], ...] = (
        ('phone', PHONE),
        ('email', EMAIL),
        ('uuid', UUID),
        ('long-number', LONG_NUMBER),
        ('opaque-token', OPAQUE),
    )
    result = value
    for label, pattern in patterns:
        result, count = pattern.subn('[REDACTED]', result)
        if count:
            stats[label] = stats.get(label, 0) + count
    return result


def sanitize(text: str) -> tuple[str, Dict[str, int]]:
    stats: Dict[str, int] = {}
    output = []
    for line in text.splitlines(keepends=True):
        force_redact = bool(EDITABLE_OR_SECRET.search(line))

        def replace(match: re.Match[str]) -> str:
            key = match.group(1)
            raw_value = match.group(2)
            if key not in {'text', 'desc', 'id'}:
                return match.group(0)
            value = decode_attr(raw_value)
            # For resource ids, only scrub obvious opaque identifiers, not ordinary Android ids.
            force = force_redact and key in {'text', 'desc'}
            clean = scrub_value(value, stats, force)
            if key == 'id' and clean == value:
                clean, count = OPAQUE.subn('[REDACTED]', clean)
                if count:
                    stats['opaque-resource-id'] = stats.get('opaque-resource-id', 0) + count
            return f'{key}="{encode_attr(clean)}"'

        output.append(ATTR.sub(replace, line))
    return ''.join(output), stats


def main() -> int:
    parser = argparse.ArgumentParser(description='Sanitize a copy of an accessibility tree dump.')
    parser.add_argument('input', type=Path, help='original *_tree.txt file')
    parser.add_argument('output', type=Path, help='new sanitized output file; must differ from input')
    args = parser.parse_args()
    source = args.input.expanduser().resolve()
    target = args.output.expanduser().resolve()
    if source == target:
        parser.error('output must be a different file; the original is never overwritten')
    if not source.is_file():
        parser.error(f'input file does not exist: {source}')

    raw = source.read_text(encoding='utf-8', errors='replace')
    clean, stats = sanitize(raw)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(clean, encoding='utf-8', newline='')
    try:
        target.chmod(0o600)
    except OSError:
        pass

    print(f'Sanitized copy: {target}')
    if stats:
        print('Redactions: ' + ', '.join(f'{key}={value}' for key, value in sorted(stats.items())))
    else:
        print('No known sensitive patterns were found; manual review is still required.')
    print('WARNING: automated masking is not a guarantee of anonymity. Inspect the output before sharing.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
