#!/usr/bin/env python3
"""Lists user-visible English strings in a React source file, so each one can be translated.

Prints three kinds: JSX text nodes, attribute literals (placeholder/title/aria-label/alt/label),
and quoted strings that contain English words, which are typically alert/toast/confirm messages.
It is a discovery aid: every reported string is then reviewed and translated explicitly.
"""
import re
import sys

ENGLISH = re.compile(r"[A-Za-z]{2,}")
CLASSY = re.compile(r"(\b(flex|grid|absolute|relative|fixed|overflow|items-|justify-|gap-|rounded|shadow|transition|cursor-|font-|text-(xs|sm|base|lg|xl|\[|\d|muted|text|primary|success|danger|warning|accent|emerald|rose|orange|white|black)|bg-|border|px-|py-|p-\d|w-\d|h-\d|m[trblxy]?-\d|space-|leading-|tracking-|backdrop|animate-|hover:|dark:|sm:|md:|lg:|group-|focus:|disabled:|ring-|max-w|min-h|inset-|z-\d|opacity-|duration-)|\$\{)")

def main(path):
    src = open(path, encoding='utf-8').read()
    seen = []
    # JSX text between tags (may span lines; no braces or tags inside)
    for m in re.finditer(r">([^<>{}]+)<", src):
        t = ' '.join(m.group(1).split())
        if ENGLISH.search(t) and len(t) > 1 and not t.startswith('//'):
            seen.append(('jsx', t))
    for m in re.finditer(r'\b(placeholder|title|aria-label|alt|label)="([^"{}]*[A-Za-z][^"{}]*)"', src):
        seen.append(('attr:' + m.group(1), m.group(2)))
    for m in re.finditer(r"(?<![A-Za-z_])(['\"`])((?:(?!\1).)*?[A-Za-z]{2,}[^'\"`\n]*?)\1", src):
        s = m.group(2)
        if ' ' in s and ENGLISH.search(s) and not s.startswith(('http', '/', '#', '.')):
            if CLASSY.search(s) or s.count(' ') >= 6 and not re.search(r'[.!?:]', s):
                continue
            seen.append(('str', s))
    out, uniq = [], set()
    for kind, s in seen:
        key = (kind, s)
        if key in uniq:
            continue
        uniq.add(key)
        out.append(f"{kind:12} | {s}")
    print('\n'.join(out))
    print(f"-- {len(out)} candidates in {path}")

if __name__ == '__main__':
    main(sys.argv[1])
