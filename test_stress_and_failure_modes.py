import urllib.request
import urllib.error
import http.cookiejar
import json
import uuid
import sys
import threading
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding="utf-8")

BASE_URL = "http://localhost:8080/api/v1"

def create_client():
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    return cj, opener

def api_call(endpoint, method="GET", data=None, token=None, idempotency_key=None, client=None):
    if client is None:
        cj, opener = create_client()
    else:
        cj, opener = client

    url = f"{BASE_URL}{endpoint}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key

    if method in ["POST", "PUT", "DELETE", "PATCH"]:
        # Fetch or retrieve CSRF token
        csrf_val = None
        for c in cj:
            if c.name == "XSRF-TOKEN":
                csrf_val = c.value
        if not csrf_val:
            try:
                csrf_req = urllib.request.Request(f"{BASE_URL}/auth/csrf")
                with opener.open(csrf_req) as resp:
                    res = json.loads(resp.read().decode("utf-8"))
                    csrf_val = res.get("data")
            except Exception:
                pass
        if csrf_val:
            headers["X-XSRF-TOKEN"] = csrf_val

    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with opener.open(req) as resp:
            status = resp.status
            resp_body = resp.read().decode("utf-8")
            return status, json.loads(resp_body) if resp_body else None
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(err_body)
        except Exception:
            return e.code, {"error": err_body}

def login(username, password, client=None):
    code, res = api_call("/auth/login", method="POST", data={"username": username, "password": password}, client=client)
    if code == 200:
        return res["data"]["accessToken"], res["data"]["user"]
    return None, res

def run_failure_mode_tests():
    print("=" * 85)
    print(" PAPERERP ENTERPRISE -- ADVERSARIAL STRESS & FAILURE MODES TEST SUITE")
    print("=" * 85)

    client = create_client()
    op_token, op_user = login("operator", "Admin@123", client=client)
    qc_token, qc_user = login("inspector", "Admin@123", client=client)
    wh_token, wh_user = login("warehouse", "Admin@123", client=client)
    plant_id = op_user["defaultPlantId"]

    # -------------------------------------------------------------------------
    # TEST 1: Brute Force & Account Lockout (Rate Limiting Fail-Closed)
    # -------------------------------------------------------------------------
    print("\n[TEST 1] Security: Brute Force Attack Simulation & Account Lockout")
    target_victim = f"victim_{uuid.uuid4().hex[:6]}"
    # Register victim user via master data or simulate attempts on non-existent / existing account
    # In RateLimitingService, failed attempts on any account key are tracked
    victim_client = create_client()
    locked_out = False
    lockout_msg = ""
    for attempt in range(1, 8):
        code, res = api_call("/auth/login", method="POST", data={"username": target_victim, "password": "WrongPassword!"}, client=victim_client)
        print(f"   * Attempt {attempt} -> HTTP {code} | {res.get('errorCode')}: {res.get('message')[:60]}")
        if code == 429:
            locked_out = True
            lockout_msg = res.get('message')
            break

    assert locked_out, "FAIL: Rate limiting did not trigger account lockout on repeated failures!"
    print(f" -> RESULT: PASS (Account locked out after threshold with HTTP 429: '{lockout_msg}')")

    # -------------------------------------------------------------------------
    # TEST 2: Multi-Tenant PostgreSQL RLS Isolation & Boundary Defense
    # -------------------------------------------------------------------------
    print("\n[TEST 2] Security: PostgreSQL Row-Level Security (RLS) Tenant Boundary Defense")
    # First, register a secret jumbo roll in Tenant A
    sec_jumbo_payload = {
        "plantId": plant_id,
        "machineId": "c0000000-0000-0000-0000-000000000001",
        "gradeId": "d0000000-0000-0000-0000-000000000001",
        "coreSpecId": "e0000000-0000-0000-0000-000000000001",
        "locationId": "f0000000-0000-0000-0000-000000000002",
        "grossWeightKg": 3500.00,
        "tareWeightKg": 100.00,
        "deckleWidthMm": 2400.00,
        "diameterMm": 1500.00
    }
    code, sec_res = api_call("/production/jumbo-rolls", method="POST", data=sec_jumbo_payload, token=op_token, client=client)
    secret_jumbo_id = sec_res["data"]["id"]
    print(f"   * Created confidential Jumbo Roll in Tenant A: {secret_jumbo_id}")

    # Now login as adversary from Tenant B
    rival_client = create_client()
    rival_token, rival_user = login("rival_operator", "Admin@123", client=rival_client)
    assert rival_token is not None, "Failed to login as rival_operator"
    print(f"   * Logged in as Rival Tenant Operator: Tenant ID = {rival_user['tenantId']}")

    # Adversary attempts to query Jumbo Rolls from Tenant A
    code, rival_list = api_call(f"/production/jumbo-rolls?plantId={plant_id}", token=rival_token, client=rival_client)
    print(f"   * Rival queries Tenant A plant rolls -> HTTP {code} | Items returned: {rival_list['data']['totalElements']}")
    assert rival_list["data"]["totalElements"] == 0, "FAIL: Tenant B was able to see Tenant A rolls!"

    # Adversary attempts direct ID access to confidential roll
    code, rival_direct = api_call(f"/production/jumbo-rolls/{secret_jumbo_id}", token=rival_token, client=rival_client)
    print(f"   * Rival direct ID access -> HTTP {code} | Error: {rival_direct.get('errorCode')}")
    assert code == 404, f"FAIL: Direct cross-tenant access expected 404, got {code}"
    print(" -> RESULT: PASS (Tenant boundary 100% impenetrable; zero cross-tenant leakage)")

    # -------------------------------------------------------------------------
    # TEST 3: RBAC Role & Privilege Enforcement
    # -------------------------------------------------------------------------
    print("\n[TEST 3] Security: Role-Based Access Control (RBAC) Privilege Enforcement")
    # Warehouse Keeper tries to execute a Slitting Run (Forbidden)
    fake_slit_payload = {
        "parentJumboId": secret_jumbo_id,
        "machineId": "c0000000-0000-0000-0000-000000000002",
        "idempotencyKey": str(uuid.uuid4()),
        "recordedTrimWasteKg": 50.00,
        "wasteDisposition": "BALED_FOR_RECYCLE",
        "cuts": [{"cutSequenceNumber": 1, "cutWidthMm": 1000.0, "grossWeightKg": 1000.0, "tareWeightKg": 10.0}]
    }
    code, rbac_res1 = api_call("/production/slitting-operations", method="POST", data=fake_slit_payload, token=wh_token, client=client)
    print(f"   * Warehouse Keeper executes Slitting -> HTTP {code} | {rbac_res1.get('errorCode')}")
    assert code == 403, f"FAIL: Expected 403 Forbidden for Warehouse Keeper slitting, got {code}"

    # Production Operator tries to execute Customer Dispatch (Forbidden)
    dispatch_payload = {
        "rollType": "JUMBO_ROLL",
        "jumboRollId": secret_jumbo_id,
        "slitRollId": None,
        "customerReference": "CUST-HACK",
        "dispatchNotes": "Unauthorized dispatch test"
    }
    code, rbac_res2 = api_call("/inventory/dispatch", method="POST", data=dispatch_payload, token=op_token, client=client)
    print(f"   * Production Operator executes Dispatch -> HTTP {code} | {rbac_res2.get('errorCode')}")
    assert code == 403, f"FAIL: Expected 403 Forbidden for Operator dispatch, got {code}"
    print(" -> RESULT: PASS (All unauthorized role actions rejected with HTTP 403 FORBIDDEN)")

    # -------------------------------------------------------------------------
    # TEST 4: Physical Invariants & Input Validation (HTTP 422)
    # -------------------------------------------------------------------------
    print("\n[TEST 4] Industrial Invariants: Negative Weights and Dimensions Validation")
    invalid_jumbo = {
        "plantId": plant_id,
        "machineId": "c0000000-0000-0000-0000-000000000001",
        "gradeId": "d0000000-0000-0000-0000-000000000001",
        "grossWeightKg": -500.00,  # Negative gross weight!
        "tareWeightKg": -50.00,    # Negative tare!
        "deckleWidthMm": 0.00      # Zero width!
    }
    code, val_res = api_call("/production/jumbo-rolls", method="POST", data=invalid_jumbo, token=op_token, client=client)
    print(f"   * Negative weights payload -> HTTP {code} | Errors: {val_res.get('validationErrors')}")
    assert code == 422, f"FAIL: Expected 422 Validation Error, got {code}"
    assert "grossWeightKg" in val_res.get("validationErrors", {}), "Missing validation for grossWeightKg"
    assert "deckleWidthMm" in val_res.get("validationErrors", {}), "Missing validation for deckleWidthMm"
    print(" -> RESULT: PASS (Physical invariant violations blocked with HTTP 422)")

    # -------------------------------------------------------------------------
    # TEST 5: Quality Gate Enforcement (Blocking Unapproved Inventory Dispatch)
    # -------------------------------------------------------------------------
    print("\n[TEST 5] Quality Gate: Quarantine & Unreleased Dispatch Prevention")
    # Mark the secret jumbo roll as QUARANTINED
    quarantine_payload = {
        "plantId": plant_id,
        "rollType": "JUMBO_ROLL",
        "jumboRollId": secret_jumbo_id,
        "measuredGsm": 180.00,  # Extreme off-spec GSM
        "measuredMoisturePct": 14.50, # High moisture defect
        "measuredCaliperUm": 310.00,
        "measuredBurstStrength": 1.20,
        "disposition": "QUARANTINE",
        "notes": "انحراف شديد في الجراماج والرطوبة - حجر صحي صارم"
    }
    code, q_res = api_call("/quality/inspections", method="POST", data=quarantine_payload, token=qc_token, client=client)
    print(f"   * Quarantine Inspection -> HTTP {code} | Response: {q_res}")
    assert code == 200, "Failed to record quarantine inspection"
    print(f"   * QC placed Roll under QUARANTINE -> HTTP {code} | Disposition: {q_res['data']['disposition']}")

    # Warehouse Keeper attempts to dispatch the quarantined roll to a customer
    dispatch_quarantine = {
        "rollType": "JUMBO_ROLL",
        "jumboRollId": secret_jumbo_id,
        "slitRollId": None,
        "customerReference": "CUST-VIP",
        "dispatchNotes": "محاولة صرف بكرة تحت الحجر"
    }
    code, disp_res = api_call("/inventory/dispatch", method="POST", data=dispatch_quarantine, token=wh_token, client=client)
    print(f"   * Dispatch attempt on Quarantined Roll -> HTTP {code} | {disp_res.get('errorCode')}: {disp_res.get('message')}")
    assert code in [400, 409, 422], f"FAIL: Quarantined roll dispatch was not blocked! Got {code}"
    print(" -> RESULT: PASS (Quality Gate strictly prevented release of non-approved roll)")

    # -------------------------------------------------------------------------
    # TEST 6: Slitting Deckle Overcut Prevention
    # -------------------------------------------------------------------------
    print("\n[TEST 6] Industrial Domain Rule: Deckle Width Overcut Prevention")
    # Release secret jumbo for slitting test
    api_call(f"/quality/inspections/{q_res['data']['id']}/disposition", method="POST",
             data={"disposition": "RELEASED", "reason": "تم إعادة الفحص والمطابقة"}, token=qc_token, client=client)

    # Parent deckle is 2400 mm. Attempt slitting with cuts totaling 2600 mm (overcut by 200 mm!)
    overcut_payload = {
        "parentJumboId": secret_jumbo_id,
        "machineId": "c0000000-0000-0000-0000-000000000002",
        "idempotencyKey": str(uuid.uuid4()),
        "recordedTrimWasteKg": 50.00,
        "wasteDisposition": "BALED_FOR_RECYCLE",
        "cuts": [
            {"cutSequenceNumber": 1, "cutWidthMm": 1300.0, "grossWeightKg": 1600.0, "tareWeightKg": 20.0},
            {"cutSequenceNumber": 2, "cutWidthMm": 1300.0, "grossWeightKg": 1600.0, "tareWeightKg": 20.0} # 1300 + 1300 = 2600 mm > 2400 mm!
        ]
    }
    code, overcut_res = api_call("/production/slitting-operations", method="POST", data=overcut_payload, token=op_token, client=client)
    print(f"   * Overcut slitting attempt (2600mm on 2400mm parent) -> HTTP {code} | {overcut_res.get('errorCode')}: {overcut_res.get('message')}")
    assert code in [400, 422], f"FAIL: Slitting overcut was not rejected! Got {code}"
    print(" -> RESULT: PASS (Physical deckle limits enforced by Slitting Kernel)")

    # -------------------------------------------------------------------------
    # TEST 7: Idempotency Protection Against Replay / Network Duplication
    # -------------------------------------------------------------------------
    print("\n[TEST 7] Concurrency & Resiliency: Idempotency Key Replay Protection")
    idempotent_key = f"IDEM-KEY-{uuid.uuid4().hex[:8]}"
    idem_payload = {
        "plantId": plant_id,
        "machineId": "c0000000-0000-0000-0000-000000000001",
        "gradeId": "d0000000-0000-0000-0000-000000000001",
        "grossWeightKg": 2800.00,
        "tareWeightKg": 80.00,
        "deckleWidthMm": 2200.00,
        "diameterMm": 1400.00
    }
    # Request 1
    code1, res1 = api_call("/production/jumbo-rolls", method="POST", data=idem_payload,
                           token=op_token, idempotency_key=idempotent_key, client=client)
    jumbo_uuid1 = res1["data"]["id"]

    # Request 2 (Exact duplicate replay with same Idempotency-Key)
    code2, res2 = api_call("/production/jumbo-rolls", method="POST", data=idem_payload,
                           token=op_token, idempotency_key=idempotent_key, client=client)
    jumbo_uuid2 = res2["data"]["id"]

    print(f"   * Call 1 ID: {jumbo_uuid1} | HTTP {code1}")
    print(f"   * Call 2 ID: {jumbo_uuid2} | HTTP {code2}")
    assert code1 in [200, 201] and code2 in [200, 201], "Idempotent requests failed"
    assert jumbo_uuid1 == jumbo_uuid2, "FAIL: Idempotency did not return identical entity ID!"
    print(" -> RESULT: PASS (Idempotency cache protected database from duplicate record creation)")

    # -------------------------------------------------------------------------
    # TEST 8: High Concurrency Race Condition (Double-Spending of Jumbo Roll)
    # -------------------------------------------------------------------------
    print("\n[TEST 8] Stress: High Concurrency Double-Slitting Race Condition Test")
    # Release the idempotent jumbo
    api_call("/quality/inspections", method="POST", data={
        "plantId": plant_id,
        "rollType": "JUMBO_ROLL",
        "jumboRollId": jumbo_uuid1,
        "measuredGsm": 150.0,
        "disposition": "RELEASED",
        "notes": "Approved for race test"
    }, token=qc_token, client=client)

    race_results = []
    concurrency_count = 10
    print(f"   * Launching {concurrency_count} simultaneous threads trying to slit the EXACT same Jumbo Roll...")

    def attempt_concurrent_slitting(idx):
        thread_client = create_client()
        thread_payload = {
            "parentJumboId": jumbo_uuid1,
            "machineId": "c0000000-0000-0000-0000-000000000002",
            "idempotencyKey": f"RACE-KEY-{idx}-{uuid.uuid4().hex[:6]}",
            "recordedTrimWasteKg": 100.00,
            "wasteDisposition": "BALED_FOR_RECYCLE",
            "cuts": [{"cutSequenceNumber": 1, "cutWidthMm": 2100.0, "grossWeightKg": 2640.0, "tareWeightKg": 20.0}]
        }
        c, r = api_call("/production/slitting-operations", method="POST", data=thread_payload,
                        token=op_token, client=thread_client)
        race_results.append((idx, c, r.get("errorCode", "OK")))

    threads = []
    for i in range(concurrency_count):
        t = threading.Thread(target=attempt_concurrent_slitting, args=(i,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    successes = [r for r in race_results if r[1] == 200]
    failures = [r for r in race_results if r[1] != 200]
    print(f"   * Race Results: {len(successes)} succeeded, {len(failures)} safely rejected")
    for r in race_results:
        print(f"     Thread #{r[0]}: HTTP {r[1]} [{r[2]}]")

    assert len(successes) == 1, f"FAIL: Expected exactly 1 successful slitting run, but got {len(successes)}!"
    assert len(failures) == concurrency_count - 1, "FAIL: Concurrent requests did not fail safely"
    print(" -> RESULT: PASS (Optimistic locking & state machine prevented double-spending of Jumbo Roll)")

    print("\n" + "=" * 85)
    print(" ALL 8 ADVERSARIAL STRESS & FAILURE MODE TESTS PASSED WITH 100% SUCCESS!")
    print("=" * 85)

if __name__ == "__main__":
    run_failure_mode_tests()
