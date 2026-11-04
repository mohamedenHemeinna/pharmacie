#!/usr/bin/env python3
"""Verifies the translation keys: every t('key') used in src must exist in ar.json AND en.json,
and every key in the dictionaries must be used somewhere (unused keys are removed with --fix)."""
import json, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src')
LOCALES = os.path.join(ROOT, 'i18n', 'locales')

def flatten(d, prefix=''):
    out = {}
    for k, v in d.items():
        key = prefix + k
        if isinstance(v, dict):
            out.update(flatten(v, key + '.'))
        else:
            out[key] = v
    return out

def unflatten_remove(d, dotted):
    parts = dotted.split('.')
    cur = d
    for p in parts[:-1]:
        cur = cur.get(p, {})
    cur.pop(parts[-1], None)

def main(fix=False):
    en = json.load(open(os.path.join(LOCALES, 'en.json'), encoding='utf-8'))
    ar = json.load(open(os.path.join(LOCALES, 'ar.json'), encoding='utf-8'))
    used = set()
    for dp, _, files in os.walk(ROOT):
        for f in files:
            if f.endswith(('.ts', '.tsx')):
                s = open(os.path.join(dp, f), encoding='utf-8').read()
                used.update(re.findall(r"""\bt\(\s*['"`]([A-Za-z0-9_.\-]+)['"`]""", s))
                used.update(re.findall(r"""i18n\.t\(\s*['"`]([A-Za-z0-9_.\-]+)['"`]""", s))
                # keys referenced from lookup tables (e.g. statusLabels.ts, expenseCategories.ts)
                used.update(re.findall(r"""['"]([a-z][A-Za-z0-9]*(?:\.[A-Za-z0-9]+)+)['"]""", s))
    EXT=('.dump','.js','.ts','.tsx','.json','.css','.png','.csv','.xlsx','.pdf','.woff2','.wasm','.gz','.svg','.html','.md','.py')
    used={k for k in used if not k.endswith(EXT)}
    fe, fa = flatten(en), flatten(ar)
    missing_en = sorted(k for k in used if k not in fe)
    missing_ar = sorted(k for k in used if k not in fa)
    unused = sorted(k for k in fe if k not in used and not k.startswith(('header.tabs.', 'nav.groups.')))
    print(f"keys used in code: {len(used)} | en: {len(fe)} | ar: {len(fa)}")
    print(f"missing in en: {len(missing_en)} {missing_en[:20]}")
    print(f"missing in ar: {len(missing_ar)} {missing_ar[:20]}")
    print(f"unused in dictionaries: {len(unused)}")
    untranslated = sorted(k for k in fa if k in fe and fa[k] == fe[k] and re.search(r'[A-Za-z]{3,}', fe[k]) and k not in ('nav.brand', 'header.appName'))
    print(f"ar values identical to English (check): {len(untranslated)} {untranslated[:15]}")
    if fix and unused:
        for k in unused:
            unflatten_remove(en, k); unflatten_remove(ar, k)
        json.dump(en, open(os.path.join(LOCALES, 'en.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
        json.dump(ar, open(os.path.join(LOCALES, 'ar.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
        print(f"removed {len(unused)} unused keys")
    return 1 if (missing_en or missing_ar) else 0

if __name__ == '__main__':
    sys.exit(main(fix='--fix' in sys.argv))
