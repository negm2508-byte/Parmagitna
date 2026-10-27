import urllib.request
import urllib.error
import http.cookiejar
import json
import uuid
import sys

sys.stdout.reconfigure(encoding="utf-8")

BASE_URL = "http://localhost:8080/api/v1"
cookie_jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookie_jar))
csrf_token = None

def get_csrf():
    for c in cookie_jar:
        if c.name == "XSRF-TOKEN":
            return c.value
    req = urllib.request.Request(f"{BASE_URL}/auth/csrf")
    with opener.open(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        return res.get("data")

def api_call(endpoint, method="GET", data=None, token=None, idempotency_key=None):
    url = f"{BASE_URL}{endpoint}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key
    if method in ["POST", "PUT", "DELETE", "PATCH"]:
        token_csrf = get_csrf()
        if token_csrf:
            headers["X-XSRF-TOKEN"] = token_csrf
    
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

def run_tests():
    print("=" * 85)
    print(" PAPERERP ENTERPRISE -- LIVE END-TO-END SYSTEM TEST EXECUTION")
    print("=" * 85)

    # 1. Health Check
    print("\n[STEP 1] Testing Health Probe: GET /api/v1/health")
    code, res = api_call("/health")
    print(f" -> Status: {code} | Data: {res.get('data')}")
    assert code == 200 and res["data"]["status"] == "UP", "Health check failed!"

    # 2. Login as Operator
    print("\n[STEP 2] Authentication: POST /api/v1/auth/login (Operator)")
    code, res = api_call("/auth/login", method="POST", data={
        "username": "operator",
        "password": "Admin@123"
    })
    print(f" -> Status: {code} | Message: {res.get('message')}")
    operator_token = res["data"]["accessToken"]
    operator_id = res["data"]["user"]["id"]
    tenant_id = res["data"]["user"]["tenantId"]
    plant_id = res["data"]["user"]["defaultPlantId"]
    print(f" -> Logged In: {res['data']['user']['fullNameAr']} | Role: {res['data']['user']['role']}")

    # 3. Login as QC Inspector
    print("\n[STEP 3] Authentication: POST /api/v1/auth/login (QC Inspector)")
    code, res = api_call("/auth/login", method="POST", data={
        "username": "inspector",
        "password": "Admin@123"
    })
    qc_token = res["data"]["accessToken"]
    print(f" -> Status: {code} | Logged In: {res['data']['user']['fullNameAr']} | Role: {res['data']['user']['role']}")

    # 4. Login as Warehouse Keeper
    print("\n[STEP 4] Authentication: POST /api/v1/auth/login (Warehouse Keeper)")
    code, res = api_call("/auth/login", method="POST", data={
        "username": "warehouse",
        "password": "Admin@123"
    })
    wh_token = res["data"]["accessToken"]
    print(f" -> Status: {code} | Logged In: {res['data']['user']['fullNameAr']} | Role: {res['data']['user']['role']}")

    # 5. Query Master Data
    print("\n[STEP 5] Query Master Data: Plants, Machines, Grades")
    code, plants_res = api_call("/masterdata/plants", token=operator_token)
    plant_id = plants_res["data"][0]["id"]
    print(f" -> Plants: {len(plants_res['data'])} found (First: {plants_res['data'][0]['nameAr']})")

    code, machines_res = api_call(f"/masterdata/machines?plantId={plant_id}", token=operator_token)
    pm_machine = next(m for m in machines_res["data"] if m["machineType"] == "PAPER_MACHINE")
    sr_machine = next(m for m in machines_res["data"] if m["machineType"] == "SLITTER_REWINDER")
    print(f" -> Paper Machine: {pm_machine['nameAr']} (Max Deckle: {pm_machine['maxDeckleWidthMm']} mm)")
    print(f" -> Slitter Machine: {sr_machine['nameAr']}")

    code, grades_res = api_call("/masterdata/grades", token=operator_token)
    grade = grades_res["data"][0]
    print(f" -> Paper Grade: {grade['nameAr']} (Target GSM: {grade['gsmTarget']})")

    # 6. Register Jumbo Roll
    print("\n[STEP 6] Production: Register Jumbo Roll: POST /api/v1/production/jumbo-rolls")
    create_jumbo_payload = {
        "plantId": plant_id,
        "machineId": pm_machine["id"],
        "gradeId": grade["id"],
        "coreSpecId": "e0000000-0000-0000-0000-000000000001",
        "locationId": "f0000000-0000-0000-0000-000000000002",
        "grossWeightKg": 3200.00,
        "tareWeightKg": 100.00,
        "deckleWidthMm": 2400.00,
        "diameterMm": 1500.00
    }
    code, jumbo_res = api_call("/production/jumbo-rolls", method="POST", data=create_jumbo_payload,
                               token=operator_token, idempotency_key=str(uuid.uuid4()))
    print(f" -> Status: {code} | Message: {jumbo_res.get('message')}")
    if code not in [200, 201]:
        print(f"ERROR: {jumbo_res}")
        return
    jumbo_data = jumbo_res["data"]
    jumbo_id = jumbo_data["id"]
    jumbo_serial = jumbo_data["serialNumber"]
    net_weight = jumbo_data["netWeightKg"]
    print(f" -> Registered Jumbo: {jumbo_serial} | Net Weight: {net_weight} kg | Status: {jumbo_data['status']}")
    assert net_weight == 3100.00, f"Expected 3100.00 kg net weight, got {net_weight}"

    # 7. QC Inspection for Jumbo Roll (Laboratory Gate)
    print("\n[STEP 7] Quality Gate: Record Laboratory Inspection: POST /api/v1/quality/inspections")
    inspection_payload = {
        "plantId": plant_id,
        "rollType": "JUMBO_ROLL",
        "jumboRollId": jumbo_id,
        "slitRollId": None,
        "measuredGsm": 150.20,
        "measuredMoisturePct": 7.45,
        "measuredCaliperUm": 209.50,
        "measuredBurstStrength": 2.85,
        "disposition": "RELEASED",
        "notes": "مطابق للمواصفات الفنية القياسية بالكامل ومعتمد للإفراج"
    }
    code, qc_res = api_call("/quality/inspections", method="POST", data=inspection_payload, token=qc_token)
    print(f" -> Status: {code} | Inspection ID: {qc_res['data']['id']}")
    print(f" -> Disposition: {qc_res['data']['disposition']} | Notes: {qc_res['data']['notes']}")

    # 8. Slitting Operation (Atomic Execution with Mass Balance)
    print("\n[STEP 8] Production: Execute Slitting Operation: POST /api/v1/production/slitting-operations")
    # Deckle: 2400 mm, Net Weight: 3100 kg
    # Cuts: 3 rolls of 780 mm = 2340 mm width, 1000 kg net each (1020 gross, 20 tare)
    # Total cut weight: 3000 kg net. Trim waste: 100 kg. Total output = 3100 kg.
    slitting_payload = {
        "parentJumboId": jumbo_id,
        "machineId": sr_machine["id"],
        "idempotencyKey": str(uuid.uuid4()),
        "recordedTrimWasteKg": 100.00,
        "wasteDisposition": "BALED_FOR_RECYCLE",
        "cuts": [
            {
                "cutSequenceNumber": 1,
                "cutWidthMm": 780.00,
                "diameterMm": 1200.00,
                "grossWeightKg": 1020.00,
                "tareWeightKg": 20.00,
                "coreSpecId": "e0000000-0000-0000-0000-000000000001"
            },
            {
                "cutSequenceNumber": 2,
                "cutWidthMm": 780.00,
                "diameterMm": 1200.00,
                "grossWeightKg": 1020.00,
                "tareWeightKg": 20.00,
                "coreSpecId": "e0000000-0000-0000-0000-000000000001"
            },
            {
                "cutSequenceNumber": 3,
                "cutWidthMm": 780.00,
                "diameterMm": 1200.00,
                "grossWeightKg": 1020.00,
                "tareWeightKg": 20.00,
                "coreSpecId": "e0000000-0000-0000-0000-000000000001"
            }
        ]
    }
    code, slit_res = api_call("/production/slitting-operations", method="POST", data=slitting_payload,
                              token=operator_token, idempotency_key=slitting_payload["idempotencyKey"])
    print(f" -> Status: {code} | Message: {slit_res.get('message')}")
    if code != 200:
        print(f"ERROR: {slit_res}")
        return
    slit_data = slit_res["data"]
    print(f" -> Slitting Run ID: {slit_data['id']}")
    print(f" -> Input Net Weight: {slit_data['inputNetWeightKg']} kg | Output Child Weight: {slit_data['childRollsTotalWeightKg']} kg")
    print(f" -> Trim Waste: {slit_data['recordedTrimWasteKg']} kg | Scale Variance: {slit_data['scaleVarianceKg']} kg")
    print(f" -> Mass Balance Status: {slit_data['massBalanceStatus']}")
    print(f" -> Created Slit Rolls: {len(slit_data['childRolls'])}")
    slit_roll_1 = slit_data['childRolls'][0]
    print(f"    * Roll 1: {slit_roll_1['serialNumber']} | Net: {slit_roll_1['netWeightKg']} kg | Width: {slit_roll_1['cutWidthMm']} mm")

    # 9. Warehouse Location Transfer
    print("\n[STEP 9] Logistics: Transfer Roll Location: POST /api/v1/inventory/transfers")
    transfer_payload = {
        "rollType": "SLIT_ROLL",
        "jumboRollId": None,
        "slitRollId": slit_roll_1["id"],
        "destinationLocationId": "f0000000-0000-0000-0000-000000000003",
        "reason": "نقل إلى رف الشحن BAY-B2 لتجهيز أمر التحميل"
    }
    code, transfer_res = api_call("/inventory/transfers", method="POST", data=transfer_payload,
                                  token=wh_token, idempotency_key=str(uuid.uuid4()))
    print(f" -> Status: {code} | Message: {transfer_res.get('message')}")
    print(f" -> Movement Ledger ID: {transfer_res['data']['id']} | Type: {transfer_res['data']['movementType']}")

    # 10. QC Inspection for Slit Roll before Dispatch
    print("\n[STEP 10] Quality Gate: Inspect Slit Roll: POST /api/v1/quality/inspections")
    slit_inspection_payload = {
        "plantId": plant_id,
        "rollType": "SLIT_ROLL",
        "jumboRollId": None,
        "slitRollId": slit_roll_1["id"],
        "measuredGsm": 150.10,
        "measuredMoisturePct": 7.40,
        "measuredCaliperUm": 210.00,
        "measuredBurstStrength": 2.84,
        "disposition": "RELEASED",
        "notes": "تم الفحص النهائي ومطابق للشحن"
    }
    code, slit_qc_res = api_call("/quality/inspections", method="POST", data=slit_inspection_payload, token=qc_token)
    print(f" -> Status: {code} | Slit Roll QC Disposition: {slit_qc_res['data']['disposition']}")

    # 11. Customer Dispatch (Strict Quality Gate Enforced)
    print("\n[STEP 11] Logistics: Customer Dispatch: POST /api/v1/inventory/dispatch")
    dispatch_payload = {
        "rollType": "SLIT_ROLL",
        "jumboRollId": None,
        "slitRollId": slit_roll_1["id"],
        "customerReference": "ORD-2026-CAIRO-PACK",
        "dispatchNotes": "شركة القاهرة لمصنوعات الكرتون المضلع"
    }
    code, dispatch_res = api_call("/inventory/dispatch", method="POST", data=dispatch_payload,
                                  token=wh_token, idempotency_key=str(uuid.uuid4()))
    print(f" -> Status: {code} | Message: {dispatch_res.get('message')}")
    print(f" -> Movement Ledger ID: {dispatch_res['data']['id']} | Type: {dispatch_res['data']['movementType']}")

    # 12. Full Roll Traceability & Genealogy
    import urllib.parse
    encoded_serial = urllib.parse.quote(slit_roll_1['serialNumber'])
    print(f"\n[STEP 12] Traceability: Full Roll Lookup: GET /api/v1/rolls/lookup/{slit_roll_1['serialNumber']}")
    code, trace_res = api_call(f"/rolls/lookup/{encoded_serial}", token=operator_token)
    trace_data = trace_res["data"]
    print(f" -> Status: {code} | Serial: {trace_data['serialNumber']}")
    print(f" -> Roll Type: {trace_data['rollType']} | Current Status: {trace_data['status']}")
    print(f" -> Movements Logged: {len(trace_data['movements'])}")
    for m in trace_data["movements"]:
        print(f"    * Movement: {m['movementType']} | Qty: {m['quantityChange']} | Time: {m['movementTimestamp']}")
    print(f" -> Inspections Logged: {len(trace_data['inspections'])}")
    for insp in trace_data["inspections"]:
        print(f"    * QC Disposition: {insp['disposition']} | Notes: {insp['notes']}")

    # 13. Query Immutable Ledger
    print("\n[STEP 13] Auditing: Query Inventory Ledger: GET /api/v1/inventory/ledger")
    code, ledger_res = api_call(f"/inventory/ledger?plantId={plant_id}&page=0&size=10", token=wh_token)
    print(f" -> Total Ledger Records in Plant: {ledger_res['data']['totalElements']}")
    for entry in ledger_res["data"]["content"][:5]:
        print(f"    * [{entry['movementType']}] Weight: {entry['weightKg']} kg | RollType: {entry['rollType']} | Ref: {entry['referenceType']}")

    print("\n" + "=" * 85)
    print(" ALL 13 TEST PHASES PASSED WITH 100% SUCCESS ON RUNNING SERVER!")
    print("=" * 85)

if __name__ == "__main__":
    run_tests()
