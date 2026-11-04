#!/usr/bin/env python3
"""API parity checker: runs identical requests against the Go reference and the Java backend.

For every scenario it compares:
  * HTTP status code (must match exactly)
  * JSON structure: same keys, same value types, same array-vs-object shape
  * error envelope: same type and status, title compared when the scenario asks for it
Generated values (ids, timestamps, tokens) are compared by type, not by value.

Usage:
  python3 parity.py --go http://localhost:8081 --java http://localhost:8082 \
      --admin-id EMP-ADMIN-1 --admin-password '...'

Exit status is non-zero if any scenario differs.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request


import random


class Target:
    # One client address per harness run, so per-IP rate limits from earlier runs do not interfere.
    RUN_IP = "10.{}.{}.{}".format(random.randint(1, 250), random.randint(1, 250), random.randint(1, 250))

    def __init__(self, name, base):
        self.name = name
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, raw_headers=None):
        data = None if body is None else json.dumps(body).encode()
        headers = {"Content-Type": "application/json", "X-Forwarded-For": Target.RUN_IP}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if raw_headers:
            headers.update(raw_headers)
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.status, resp.read(), dict(resp.headers)
        except urllib.error.HTTPError as e:
            return e.code, e.read(), dict(e.headers)


def shape(value):
    """Structural signature: keys and value kinds, with leaf values reduced to their type."""
    if isinstance(value, dict):
        return {k: shape(v) for k, v in sorted(value.items())}
    if isinstance(value, list):
        return [shape(value[0])] if value else []
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "bool"
    if isinstance(value, (int, float)):
        return "number"
    return "string"


def parse(body):
    if not body:
        return None
    try:
        return json.loads(body)
    except ValueError:
        return body.decode("utf-8", "replace")


class Report:
    def __init__(self):
        self.results = []

    def add(self, name, ok, detail=""):
        self.results.append((name, ok, detail))
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {name}" + (f"  -- {detail}" if detail and not ok else ""))

    def summary(self):
        failed = [r for r in self.results if not r[1]]
        print(f"\n{len(self.results) - len(failed)}/{len(self.results)} scenarios match")
        return 1 if failed else 0


def compare(report, name, go, java, check_shape=True, check_title=False):
    gs, gb, _ = go
    js, jb, _ = java
    if gs != js:
        report.add(name, False, f"status go={gs} java={js} body_go={gb[:160]!r} body_java={jb[:160]!r}")
        return
    gj, jj = parse(gb), parse(jb)
    if check_shape and shape(gj) != shape(jj):
        report.add(name, False, f"shape differs\n  go:   {shape(gj)}\n  java: {shape(jj)}")
        return
    if isinstance(gj, dict) and "type" in gj and gs >= 400:
        if gj.get("type") != jj.get("type"):
            report.add(name, False, f"error type go={gj.get('type')} java={jj.get('type')}")
            return
        if check_title and gj.get("title") != jj.get("title"):
            report.add(name, False, f"title go={gj.get('title')!r} java={jj.get('title')!r}")
            return
    report.add(name, True)


def login(target, identifier, password):
    status, body, _ = target.call("POST", "/api/auth/login", {"identifier": identifier, "password": password})
    if status != 200:
        raise SystemExit(f"{target.name}: login failed with {status}: {body[:200]!r}")
    return parse(body)["accessToken"]


ADMIN_ID = "EMP-ADMIN-1"
ADMIN_PW = ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--go", required=True)
    ap.add_argument("--java", required=True)
    ap.add_argument("--admin-id", default="EMP-ADMIN-1")
    ap.add_argument("--admin-password", required=True)
    ap.add_argument("--db-url", default=None,
                    help="psql URL of the TEST database (never production). Enables resetting the Master PIN row between runs.")
    args = ap.parse_args()
    global PARITY_DB_URL
    PARITY_DB_URL = args.db_url

    global ADMIN_ID, ADMIN_PW
    ADMIN_ID, ADMIN_PW = args.admin_id, args.admin_password
    go = Target("go", args.go)
    java = Target("java", args.java)
    report = Report()

    # Public routes
    for path in ["/api/health"]:
        compare(report, f"GET {path}", go.call("GET", path), java.call("GET", path), check_shape=True)

    compare(report, "GET /api/auth/setup-status",
            go.call("GET", "/api/auth/setup-status"), java.call("GET", "/api/auth/setup-status"))

    # Login: wrong password, unknown user, malformed body
    for name, body in [
        ("POST /api/auth/login wrong password", {"identifier": args.admin_id, "password": "wrong-password"}),
        ("POST /api/auth/login unknown user", {"identifier": "nobody-here", "password": "whatever123"}),
        ("POST /api/auth/login missing fields", {"identifier": ""}),
    ]:
        compare(report, name, go.call("POST", "/api/auth/login", body),
                java.call("POST", "/api/auth/login", body), check_title=False)

    # Authentication envelope
    compare(report, "GET /api/medicines without token",
            go.call("GET", "/api/medicines"), java.call("GET", "/api/medicines"))
    compare(report, "GET /api/medicines bad token",
            go.call("GET", "/api/medicines", token="not.a.jwt"),
            java.call("GET", "/api/medicines", token="not.a.jwt"))

    # Authenticated, admin
    gt = login(go, args.admin_id, args.admin_password)
    jt = login(java, args.admin_id, args.admin_password)
    compare(report, "POST /api/auth/login admin success",
            go.call("POST", "/api/auth/login", {"identifier": args.admin_id, "password": args.admin_password}),
            java.call("POST", "/api/auth/login", {"identifier": args.admin_id, "password": args.admin_password}))
    compare(report, "GET /api/auth/me",
            go.call("GET", "/api/auth/me", token=gt), java.call("GET", "/api/auth/me", token=jt))

    # Tokens must be interchangeable: a Go-issued token works on the Java backend and vice versa.
    compare(report, "Go token accepted by Java (GET /api/auth/me)",
            (200, b"", {}), java.call("GET", "/api/auth/me", token=gt), check_shape=False)
    compare(report, "Java token accepted by Go (GET /api/auth/me)",
            (200, b"", {}), go.call("GET", "/api/auth/me", token=jt), check_shape=False)

    # Read-only collections (shape only; contents differ by data)
    for path in ["/api/medicines", "/api/medicine-groups", "/api/patients", "/api/due-khata",
                 "/api/sales", "/api/expenses", "/api/needed-meds", "/api/opd-visits",
                 "/api/distributors", "/api/marketing-campaigns", "/api/worksheet-tasks",
                 "/api/employees", "/api/daily-register", "/api/invoice-config",
                 "/api/security/master-pin", "/api/ai-config", "/api/backup/config",
                 "/api/backup/status", "/api/backup"]:
        compare(report, f"GET {path} (admin)",
                go.call("GET", path, token=gt), java.call("GET", path, token=jt))

    run_flows(report, go, java, gt, jt)
    run_more_flows(report, go, java)
    return report.summary()


PARITY_DB_URL = None


def reset_pin_state():
    """Clears the Master PIN row on the test database so each backend starts from the same state."""
    if not PARITY_DB_URL:
        return
    import subprocess
    subprocess.run(["psql", PARITY_DB_URL, "-q", "-c",
                    "UPDATE security_settings SET master_pin_hash = NULL, failed_attempts = 0, locked_until = NULL"],
                   check=True)


def pin_sequence(t, tok):
    """Ordered Master PIN steps for one backend. Returns [(step name, (status, body, headers)), ...]."""
    steps = []

    def step(name, method, path, body=None):
        res = t.call(method, path, body, token=tok)
        steps.append((f"{name} [{t.name}]", res))
        return res

    step("PUT /api/security/master-pin first set", "PUT", "/api/security/master-pin", {"newPin": "4321"})
    step("PUT /api/security/master-pin invalid PIN", "PUT", "/api/security/master-pin", {"newPin": "12a"})
    step("POST /api/security/master-pin/verify correct", "POST", "/api/security/master-pin/verify", {"pin": "4321"})
    for i in range(4):
        step(f"POST /api/security/master-pin/verify wrong #{i + 1}", "POST",
             "/api/security/master-pin/verify", {"pin": "0000"})
    step("POST /api/security/master-pin/verify wrong #5 locks", "POST",
         "/api/security/master-pin/verify", {"pin": "0000"})
    step("POST /api/security/master-pin/verify while locked (423)", "POST",
         "/api/security/master-pin/verify", {"pin": "4321"})
    step("GET /api/security/master-pin shows lock", "GET", "/api/security/master-pin")
    step("PUT /api/security/master-pin rotate needs current", "PUT", "/api/security/master-pin",
         {"newPin": "9999"})
    return steps


def run_flows(report, go, java, gt, jt):
    """Write flows executed against both backends with the same payloads.

    Each backend gets its own generated IDs, so the two runs never collide. Each step compares
    status and JSON shape. Steps that depend on IDs use the IDs that each backend returned.
    """
    import uuid

    def both(name, method, path_tpl, body=None, expect_ids=None, check_title=False):
        """Run one request on both targets. path_tpl may contain {id}, filled per target."""
        results = {}
        for tgt, tok in ((go, gt), (java, jt)):
            path = path_tpl
            if expect_ids is not None:
                path = path_tpl.replace("{id}", expect_ids[tgt.name])
            b = body(tgt.name) if callable(body) else body
            results[tgt.name] = tgt.call(method, path, b, token=tok)
        compare(report, name, results["go"], results["java"], check_title=check_title)
        return results

    def created_id(res, name):
        status, body, _ = res
        data = parse(body)
        return data.get("id") if isinstance(data, dict) else None

    # --- medicines
    def med_body(who):
        return {"name": f"Paracetamol {who}", "company": "Acme", "dist": "D", "hsn": "3004",
                "batch": "B1", "pack": "10x10", "salt": "Paracetamol", "group": "General",
                "rack": "R1", "stock": 12, "rate": 10.5, "omrp": 0, "mrp": 15, "scheme": "",
                "gst": 12, "disc": 0, "tabsPerStrip": 10, "expiry": "2028-01", "isLabTest": False}
    r = both("POST /api/medicines create", "POST", "/api/medicines", med_body)
    ids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("GET /api/medicines/{id}", "GET", "/api/medicines/{id}", expect_ids=ids)
    both("PUT /api/medicines/{id} update", "PUT", "/api/medicines/{id}",
         lambda who: {**med_body(who), "stock": 20}, expect_ids=ids)
    both("POST /api/medicines missing name", "POST", "/api/medicines", {"company": "x"}, check_title=False)
    both("GET /api/medicines/{id} missing", "GET", "/api/medicines/does-not-exist")
    both("DELETE /api/medicines/{id}", "DELETE", "/api/medicines/{id}", expect_ids=ids)

    # --- medicine groups and distributors
    r = both("POST /api/medicine-groups create", "POST", "/api/medicine-groups",
             lambda who: {"name": f"Group {who} {uuid.uuid4().hex[:6]}"})
    gids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("DELETE /api/medicine-groups/{id}", "DELETE", "/api/medicine-groups/{id}", expect_ids=gids)
    r = both("POST /api/distributors create", "POST", "/api/distributors",
             lambda who: {"name": f"Dist {who}", "gstin": "06AAAAA0000A1Z5", "phone": "1", "addr": "x"})
    dids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("PUT /api/distributors/{id}", "PUT", "/api/distributors/{id}",
         lambda who: {"name": f"Dist {who} renamed", "gstin": "", "phone": "", "addr": ""}, expect_ids=dids)
    both("DELETE /api/distributors/{id}", "DELETE", "/api/distributors/{id}", expect_ids=dids)

    # --- patients: sequence, create, update, due-khata, sales
    both("GET /api/patients/next-id", "GET", "/api/patients/next-id")
    r = both("POST /api/patients create", "POST", "/api/patients",
             lambda who: {"name": f"Patient {who} {uuid.uuid4().hex[:6]}", "phone": "9999",
                          "totalDue": 0, "purchaseHistory": [], "bloodTests": ["CBC"]})
    pids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("GET /api/patients/{id}", "GET", "/api/patients/{id}", expect_ids=pids)
    both("PUT /api/patients/{id}", "PUT", "/api/patients/{id}",
         lambda who: {"name": f"Patient {who} edited", "phone": "1", "totalDue": 5,
                      "purchaseHistory": [{"date": "2026-01-01", "items": "x", "amount": 5}],
                      "bloodTests": []}, expect_ids=pids)
    r = both("POST /api/due-khata create", "POST", "/api/due-khata",
             lambda who: {"name": f"Due {who}", "phone": "1", "due": 50.5,
                          "lastDate": "2026-01-02", "patientId": None})
    dueids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("PUT /api/due-khata/{id}", "PUT", "/api/due-khata/{id}",
         lambda who: {"name": f"Due {who} x", "due": 10}, expect_ids=dueids)
    both("DELETE /api/due-khata/{id}", "DELETE", "/api/due-khata/{id}", expect_ids=dueids)
    r = both("POST /api/sales create", "POST", "/api/sales",
             lambda who: {"date": "2026-11-04", "mode": "Cash", "amt": 100, "total": 100,
                          "itemsDetail": [{"name": "x", "qty": 1, "price": 100, "total": 100}]})
    sids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("GET /api/sales/{id}", "GET", "/api/sales/{id}", expect_ids=sids)
    both("POST /api/sales missing mode", "POST", "/api/sales", {"date": "2026-11-04"})
    both("POST /api/sales unknown patient (FK)", "POST", "/api/sales",
         {"date": "2026-11-04", "mode": "Cash", "patientId": "no-such-patient", "itemsDetail": []})

    # --- expenses, medicine orders, OPD, campaigns, tasks
    r = both("POST /api/expenses create", "POST", "/api/expenses",
             lambda who: {"date": "2026-11-04", "cat": "Rent", "desc": who, "amt": 250})
    eids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("PUT /api/expenses/{id}", "PUT", "/api/expenses/{id}",
         lambda who: {"date": "2026-11-04", "cat": "Rent", "desc": who, "amt": 300}, expect_ids=eids)
    both("DELETE /api/expenses/{id}", "DELETE", "/api/expenses/{id}", expect_ids=eids)
    both("POST /api/needed-meds bad status", "POST", "/api/needed-meds", {"med": "x", "status": "Bogus"})
    r = both("POST /api/needed-meds create", "POST", "/api/needed-meds",
             lambda who: {"med": f"Med {who}", "qty": 2, "status": "Pending"})
    nids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("PUT /api/needed-meds/{id}", "PUT", "/api/needed-meds/{id}",
         lambda who: {"med": "Med x", "qty": 3, "status": "Delivered"}, expect_ids=nids)
    both("DELETE /api/needed-meds/{id}", "DELETE", "/api/needed-meds/{id}", expect_ids=nids)
    r = both("POST /api/opd-visits create", "POST", "/api/opd-visits",
             lambda who: {"name": f"OPD {who}", "vdate": "2026-11-04"})
    oids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("DELETE /api/opd-visits/{id}", "DELETE", "/api/opd-visits/{id}", expect_ids=oids)
    r = both("POST /api/marketing-campaigns create", "POST", "/api/marketing-campaigns",
             lambda who: {"action": f"Campaign {who}", "status": "Planned"})
    cids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("POST /api/marketing-campaigns bad status", "POST", "/api/marketing-campaigns",
         {"action": "x", "status": "Nope"})
    both("DELETE /api/marketing-campaigns/{id}", "DELETE", "/api/marketing-campaigns/{id}", expect_ids=cids)
    r = both("POST /api/worksheet-tasks create", "POST", "/api/worksheet-tasks",
             lambda who: {"desc": f"Task {who}", "status": "Pending"})
    tids = {n: created_id(r[n], n) for n in ("go", "java")}
    both("DELETE /api/worksheet-tasks/{id}", "DELETE", "/api/worksheet-tasks/{id}", expect_ids=tids)

    # --- registers and configuration
    both("PUT /api/daily-register", "PUT", "/api/daily-register",
         {"date": "2026-11-04", "prevBD": 100, "todaySell": 50, "denominations": {"500": 2}, "isLocked": False})
    both("GET /api/daily-register after put", "GET", "/api/daily-register")
    both("PUT /api/invoice-config", "PUT", "/api/invoice-config",
         {"name": "Store", "dl": "DL1", "autoPurgeOldInvoices": True})
    both("PUT /api/invoice-config missing name", "PUT", "/api/invoice-config", {"dl": "x"})
    both("PUT /api/security/master-pin (no PIN yet)", "PUT", "/api/security/master-pin", {"newPin": "12"})
    both("POST /api/security/master-pin/verify (none set yet)", "POST", "/api/security/master-pin/verify", {"pin": "1234"})

    # --- employees and admin guards
    r = both("POST /api/employees create", "POST", "/api/employees",
             lambda who: {"name": f"Clerk {who} {uuid.uuid4().hex[:6]}", "password": "clerkpass1",
                          "role": "staff", "permissions": ["patients"]})
    eids2 = {n: created_id(r[n], n) for n in ("go", "java")}
    both("POST /api/employees short password", "POST", "/api/employees", {"name": "x", "password": "short"})
    both("POST /api/employees unknown permission", "POST", "/api/employees",
         {"name": "x", "password": "longenough1", "permissions": ["nope"]})
    both("DELETE /api/employees/{id}", "DELETE", "/api/employees/{id}", expect_ids=eids2)
    both("DELETE /api/employees self-delete guard", "DELETE", "/api/employees/EMP-ADMIN-1")
    both("PUT /api/employees/EMP-ADMIN-1 demote last admin", "PUT", "/api/employees/EMP-ADMIN-1",
         {"name": "Master Admin", "role": "staff", "permissions": []})
    both("GET /api/employees/{id} missing", "GET", "/api/employees/none-here")

    # --- authorization on write routes for a restricted user
    r = both("POST /api/employees (restricted user)", "POST", "/api/employees",
             lambda who: {"name": f"Limited {who} {uuid.uuid4().hex[:6]}", "password": "limited123",
                          "role": "staff", "permissions": ["expenses"]})
    rest = {}
    for tgt in (go, java):
        pass
    return report


def run_more_flows(report, go, java):
    """Permission matrix, Master PIN lockout, AI/OCR fallbacks, backup validation and download."""
    import uuid
    targets = {"go": go, "java": java}

    def both(name, method, path, body=None, token_for=None, raw=False):
        results = {}
        for n, t in targets.items():
            tok = token_for[n] if token_for else None
            results[n] = t.call(method, path, body, token=tok)
        if raw:
            ok = results["go"][0] == results["java"][0]
            report.add(name, ok, f"status go={results['go'][0]} java={results['java'][0]}")
        else:
            compare(report, name, results["go"], results["java"])
        return results

    # Restricted user: only the expenses tab.
    uname = f"limited-{uuid.uuid4().hex[:6]}"
    admin_tokens = {n: login(t, ADMIN_ID, ADMIN_PW) for n, t in targets.items()}
    for n, t in targets.items():
        t.call("POST", "/api/employees", {"id": uname, "name": uname, "password": "limited123",
                                           "role": "staff", "permissions": ["expenses"]}, token=admin_tokens[n])
    lim = {n: login(t, uname, "limited123") for n, t in targets.items()}
    both("restricted GET /api/medicines (403)", "GET", "/api/medicines", token_for=lim)
    both("restricted GET /api/expenses (200)", "GET", "/api/expenses", token_for=lim)
    both("restricted GET /api/employees (403)", "GET", "/api/employees", token_for=lim)
    both("restricted POST /api/employees (403)", "POST", "/api/employees",
         {"name": "x", "password": "longenough1"}, token_for=lim)
    both("restricted GET /api/backup (403)", "GET", "/api/backup", token_for=lim)
    both("restricted GET /api/ai-config (403)", "GET", "/api/ai-config", token_for=lim)
    both("restricted GET /api/auth/me (200)", "GET", "/api/auth/me", token_for=lim)
    both("restricted PUT /api/auth/password wrong current (401)", "PUT", "/api/auth/password",
         {"currentPassword": "nope-nope", "newPassword": "another-pass1"}, token_for=lim)

    # Master PIN: both backends share one database row, so each runs its own sequence after a reset.
    pin_results = {}
    for n, t in targets.items():
        reset_pin_state()
        pin_results[n] = pin_sequence(t, admin_tokens[n])
    reset_pin_state()
    for (step, go_res), (_, java_res) in zip(pin_results["go"], pin_results["java"]):
        compare(report, step, go_res, java_res)

    # AI and OCR fallbacks (no provider configured in the test environment).
    both("POST /api/ai/ask without provider (fallback)", "POST", "/api/ai/ask",
         {"prompt": "paracetamol dose"}, token_for=admin_tokens)
    both("POST /api/ai/ask missing prompt", "POST", "/api/ai/ask", {}, token_for=admin_tokens)
    both("POST /api/ocr/parse-bill without provider (fallback)", "POST", "/api/ocr/parse-bill",
         {"textContent": "bill text"}, token_for=admin_tokens)
    both("POST /api/ocr/parse-bill no input", "POST", "/api/ocr/parse-bill", {}, token_for=admin_tokens)
    both("POST /api/ai-config invalid body", "POST", "/api/ai-config", {"baseUrl": "x"}, token_for=admin_tokens)

    # Backup: validation paths only (nothing is sent to any S3 endpoint).
    both("POST /api/backup no destination", "POST", "/api/backup", token_for=admin_tokens)
    both("POST /api/backup/{id}/restore without confirm", "POST", "/api/backup/no-such-id/restore",
         {"confirm": "yes"}, token_for=admin_tokens)
    both("POST /api/backup/{id}/restore unknown id (404)", "POST", "/api/backup/no-such-id/restore",
         {"confirm": "RESTORE"}, token_for=admin_tokens)
    both("POST /api/backup/config invalid body", "POST", "/api/backup/config", {"endpoint": "x"},
         token_for=admin_tokens)
    both("GET /api/backup/download (admin, headers)", "GET", "/api/backup/download", token_for=admin_tokens, raw=True)
    both("GET /api/backup/download without token", "GET", "/api/backup/download")
    return report


if __name__ == "__main__":
    sys.exit(main())
