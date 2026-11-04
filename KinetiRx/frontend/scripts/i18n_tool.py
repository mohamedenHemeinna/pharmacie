#!/usr/bin/env python3
"""Applies translation edits to a React source file and records the keys in the locale files.

Each operation replaces an exact English snippet in the source with a t() call, and stores the
English and Arabic text under a dotted key. The English file keeps the original text (reference),
the Arabic file holds the professional translation. Operations that match nothing are reported,
so a missed string can never be silently left in English.

Usage from another script:
    from i18n_tool import run
    run('src/components/X.tsx', 'x', ops, hook_after='  const [a, setA] = useState(...);')
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
LOCALE_DIR = os.path.join(HERE, '..', 'src', 'i18n', 'locales')
EN = os.path.join(LOCALE_DIR, 'en.json')
AR = os.path.join(LOCALE_DIR, 'ar.json')


def _load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def _save(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=False)
        f.write('\n')


def _set(d, dotted, value):
    parts = dotted.split('.')
    cur = d
    for p in parts[:-1]:
        cur = cur.setdefault(p, {})
    cur[parts[-1]] = value


def text(en, ar, key):
    """English text node between tags, e.g. <span>Dashboard</span>."""
    return {'kind': 'text', 'en': en, 'ar': ar, 'key': key}


def attr(name, en, ar, key):
    """Attribute with a literal value: name="English" -> name={t('key')}."""
    return {'kind': 'attr', 'name': name, 'en': en, 'ar': ar, 'key': key}


def lit(en, ar, key):
    """String literal in JavaScript: 'English' / "English" / `English` -> t('key')."""
    return {'kind': 'lit', 'en': en, 'ar': ar, 'key': key}


def raw(old, new, entries):
    """Replace an exact code snippet. entries = {key: (en, ar)} recorded in the locale files."""
    return {'kind': 'raw', 'old': old, 'new': new, 'entries': entries}


HOOK_LINE = '  const { t } = useTranslation();'
IMPORT_LINE = "import { useTranslation } from 'react-i18next';"


def _apply_one(src, op, report):
    kind = op['kind']
    if kind == 'text':
        # Whitespace around the text may include line breaks (JSX text is trimmed by React anyway).
        pattern = re.compile(r'>\s*' + re.escape(op['en']) + r'\s*<')
        replacement = '>{t(\'' + op['key'] + '\')}<'
        out, n = pattern.subn(replacement, src)
        if n == 0:
            report.append(('MISSING text', op['en']))
        return out, n
    if kind == 'attr':
        old = op['name'] + '="' + op['en'] + '"'
        new = op['name'] + "={t('" + op['key'] + "')}"
        n = src.count(old)
        if n == 0:
            report.append(('MISSING attr', op['name'], op['en']))
        return src.replace(old, new), n
    if kind == 'lit':
        n = 0
        out = src
        for q in ("'", '"', '`'):
            old = q + op['en'] + q
            new = "t('" + op['key'] + "')"
            c = out.count(old)
            n += c
            out = out.replace(old, new)
        if n == 0:
            report.append(('MISSING literal', op['en']))
        return out, n
    if kind == 'raw':
        n = src.count(op['old'])
        if n == 0:
            report.append(('MISSING raw', op['old'][:90]))
        return src.replace(op['old'], op['new']), n
    raise ValueError(kind)


def _ensure_hook(src, hook_after):
    """Adds the useTranslation import and the t() hook. Returns the new source."""
    if "useTranslation" not in src:
        # Insert after the last top-level import line.
        imports = list(re.finditer(r"^import [^;]*;\n", src, re.M | re.S))
        pos = imports[-1].end() if imports else 0
        src = src[:pos] + IMPORT_LINE + '\n' + src[pos:]
    if 'const { t } = useTranslation();' not in src:
        if hook_after is None or hook_after not in src:
            raise SystemExit('hook anchor not found: %r' % hook_after)
        idx = src.index(hook_after) + len(hook_after)
        src = src[:idx] + '\n' + HOOK_LINE + src[idx:]
    return src


def run(path, ns, ops, hook_after=None, hook=True):
    """Applies ops to path, records keys under namespace ns, and reports unmatched snippets."""
    with open(path, encoding='utf-8') as f:
        src = f.read()
    en = _load(EN)
    ar = _load(AR)
    report = []
    total = 0
    for op in ops:
        if op['kind'] != 'raw':
            # One canonical key form everywhere: ns.key (an already-prefixed key is kept as is).
            full = op['key'] if op['key'].startswith(ns + '.') else ns + '.' + op['key']
            op = dict(op, key=full)
        src, n = _apply_one(src, op, report)
        total += n
        if n == 0:
            continue  # never record a key whose English snippet was not found
        if op['kind'] == 'raw':
            for key, (e, a) in op['entries'].items():
                _set(en, key, e)
                _set(ar, key, a)
        else:
            _set(en, op['key'], op['en'])
            _set(ar, op['key'], op['ar'])
    if hook:
        src = _ensure_hook(src, hook_after)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)
    _save(EN, en)
    _save(AR, ar)
    print('%s: %d replacements, %d ops' % (os.path.relpath(path), total, len(ops)))
    for r in report:
        print('  !! ', *r)
    return report
