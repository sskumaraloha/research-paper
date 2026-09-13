/* ==========================================================================
   LogSense demo — single source of mock data.
   Every number shown anywhere in the UI (dashboards, chat answers, parts,
   patterns) is derived from these records, so the demo stays internally
   consistent. Deterministic: same question -> same answer.
   ========================================================================== */

window.LS = window.LS || {};

(function () {
  "use strict";

  /* ---------------- Plant & people ---------------- */

  const plant = {
    name: "Demo Pune Manufacturing Plant",
    shortName: "Demo Pune Plant",
    period: "Last 12 months",
    user: { name: "Rajesh Kumar", role: "Maintenance Manager", initials: "RK" },
    // Plant-wide KPI snapshot for the demo corpus (1,842 indexed records —
    // larger than the sample detailed below). Presented as Demo Plant data.
    kpis: {
      mttr: { value: 2.8, unit: "hrs", delta: -18.4, goodWhenDown: true },
      mtbf: { value: 184, unit: "hrs", delta: 12.7, goodWhenDown: false },
      downtime: { value: 142.5, unit: "hrs", delta: -14.2, goodWhenDown: true },
      breakdowns: { value: 87, unit: "", delta: -9.6, goodWhenDown: true },
      recordsIndexed: 1842,
    },
    downtimeCost: "₹1,25,000 / line-hour",
    trend: {
      labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug"],
      values: [24.5, 22.1, 23.4, 19.8, 18.2, 16.5, 14.9, 13.1],
    },
  };

  const technicians = ["Sunil", "Amit", "Rahul", "Prakash", "Vijay"];
  const lines = ["Line 1", "Line 2", "Line 3", "Line 4", "Utilities"];

  /* ---------------- Machines ----------------
     pool: which filler-template pool generated records draw from.
     gen:  how many filler records to generate (hand records are added on top).
     anchor: date of the newest generated record; step: days between them. */

  const machines = [
    { id: "BLST-L1-01", name: "Blister Machine 1", line: "Line 1", type: "Blister Packing", status: "Healthy", pool: "packaging", gen: 7, anchor: "2026-07-28", step: 21 },
    { id: "BLST-L1-02", name: "Blister Machine 2", line: "Line 1", type: "Blister Packing", status: "Recurring", pool: "packaging", gen: 24, anchor: "2026-06-20", step: 17, aliases: ["blister m/c 2", "BLST-2"] },
    { id: "CART-L1-01", name: "Cartonator 1", line: "Line 1", type: "Cartoning Machine", status: "Healthy", pool: "packaging", gen: 6, anchor: "2026-07-15", step: 24 },
    { id: "CAPP-L1-01", name: "Capping Machine 1", line: "Line 1", type: "Capping Machine", status: "Healthy", pool: "packaging", gen: 5, anchor: "2026-06-30", step: 26 },

    { id: "PRSS-L2-01", name: "Press Machine 01", line: "Line 2", type: "Tablet Press", status: "Healthy", pool: "press", gen: 9, anchor: "2026-07-25", step: 20 },
    { id: "PRSS-L2-02", name: "Press Machine 02", line: "Line 2", type: "Tablet Press", status: "Attention", pool: "press", gen: 30, anchor: "2026-07-30", step: 14, aliases: ["press m/c 2", "PRESS-02"] },
    { id: "COAT-L2-01", name: "Coating Machine 1", line: "Line 2", type: "Coating Machine", status: "Healthy", pool: "press", gen: 6, anchor: "2026-07-10", step: 25 },
    { id: "GRAN-L2-01", name: "Granulator 1", line: "Line 2", type: "Granulator", status: "Healthy", pool: "press", gen: 5, anchor: "2026-06-25", step: 27 },

    { id: "CONV-L3-MTR-01", name: "Line 3 Conveyor Motor", line: "Line 3", type: "Conveyor Motor", status: "Attention", pool: "rotary", gen: 0, anchor: "2026-08-14", step: 0, aliases: ["L3 conv", "CONV L3 MTR", "conveyor motor line 3"] },
    { id: "GRBX-L3-01", name: "L3 Gearbox Unit", line: "Line 3", type: "Gearbox", status: "Healthy", pool: "rotary", gen: 7, anchor: "2026-07-18", step: 22 },
    { id: "BLWR-L3-01", name: "L3 Blower Fan", line: "Line 3", type: "Blower Fan", status: "Attention", pool: "rotary", gen: 7, anchor: "2026-08-02", step: 21 },
    { id: "PLTZ-L3-01", name: "Palletizer L3", line: "Line 3", type: "Palletizer", status: "Healthy", pool: "packaging", gen: 7, anchor: "2026-07-22", step: 23 },
    { id: "LBLR-L3-01", name: "Labeling Machine 3", line: "Line 3", type: "Labeling Machine", status: "Healthy", pool: "packaging", gen: 6, anchor: "2026-07-05", step: 25 },

    { id: "PACK-L4-04", name: "Packing Machine 4", line: "Line 4", type: "Packing Machine", status: "Attention", pool: "packaging", gen: 18, anchor: "2026-07-20", step: 15, aliases: ["packing m/c 4", "PKG-4"] },
    { id: "SEAL-L4-01", name: "Sealing Machine 4", line: "Line 4", type: "Sealing Machine", status: "Healthy", pool: "packaging", gen: 6, anchor: "2026-07-12", step: 24 },
    { id: "CHKW-L4-01", name: "Checkweigher 4", line: "Line 4", type: "Checkweigher", status: "Healthy", pool: "packaging", gen: 5, anchor: "2026-06-28", step: 27 },

    { id: "COMP-UT-01", name: "Compressor #1", line: "Utilities", type: "Air Compressor", status: "Healthy", pool: "fluid", gen: 7, anchor: "2026-07-26", step: 22 },
    { id: "COMP-UT-02", name: "Compressor #2", line: "Utilities", type: "Air Compressor", status: "Healthy", pool: "fluid", gen: 14, anchor: "2026-07-14", step: 16, aliases: ["comp 2", "comp-02"] },
    { id: "PUMP-UT-201", name: "Pump P-201", line: "Utilities", type: "Centrifugal Pump", status: "Attention", pool: "fluid", gen: 8, anchor: "2026-07-28", step: 20, aliases: ["P-201"] },
    { id: "CTWR-UT-01", name: "Cooling Tower Fan", line: "Utilities", type: "Cooling Tower Fan", status: "Healthy", pool: "rotary", gen: 5, anchor: "2026-07-08", step: 28 },
    { id: "BOIL-UT-01", name: "Boiler Feed Pump", line: "Utilities", type: "Boiler Feed Pump", status: "Healthy", pool: "fluid", gen: 6, anchor: "2026-07-02", step: 26 },
    { id: "ADRY-UT-01", name: "Air Dryer Unit", line: "Utilities", type: "Air Dryer", status: "Healthy", pool: "fluid", gen: 4, anchor: "2026-06-18", step: 30 },
  ];

  /* ---------------- Hand-authored maintenance records ----------------
     kind: breakdown | pm | inspection
     src:  { f: file, s: sheet/section, r: row/page } */

  function R(id, m, date, mode, kind, raw, action, part, dt, tech, src) {
    return { id, machineId: m, date, mode, kind, raw, normalized: action, part, downtime: dt, tech, src };
  }

  const XLS26 = "maintenance_log_2026.xlsx";
  const XLS25 = "maintenance_log_2025.xlsx";
  const SAP = "SAP_PM_export.csv";
  const REG26 = "scanned_register_2026.pdf";
  const REG25 = "scanned_register_2025.pdf";

  const handRecords = [
    /* ---- Line 3 Conveyor Motor: 23 records, Apr 2025 – Aug 2026 ----
       Bearing replacements on an exact ~92-day cycle:
       11 Nov 2025 -> 11 Feb 2026 -> 14 May 2026 -> 14 Aug 2026. */
    R(1231, "CONV-L3-MTR-01", "2026-08-14", "Bearing", "breakdown",
      "MTR brng noise L3 conv, replcd 6205ZZ, algnmnt chk, OK",
      "Bearing replaced", "6205ZZ", 2.0, "Sunil", { f: XLS26, s: "Aug-2026", r: 143 }),
    R(1226, "CONV-L3-MTR-01", "2026-07-21", "Preventive", "pm",
      "L3 conv mtr PM done. greasing + terminal tightening. ok",
      "Preventive maintenance completed", null, 0, "Amit", { f: SAP, s: "PM orders", r: 88 }),
    R(1219, "CONV-L3-MTR-01", "2026-06-30", "Overheating", "breakdown",
      "L3 conv MTR chal raha but garam. cooling fan jam tha, clean kiya, temp normal",
      "Cooling fan cleaned", null, 1.1, "Rahul", { f: XLS26, s: "Jun-2026", r: 61 }),
    R(1213, "CONV-L3-MTR-01", "2026-06-08", "Inspection", "inspection",
      "vibration reading L3 conv motor - 4.1 mm/s NDE. monitor.",
      "Vibration inspection recorded", null, 0, "Vijay", { f: REG26, s: "Register", r: "page 12" }),
    R(1207, "CONV-L3-MTR-01", "2026-05-14", "Bearing", "breakdown",
      "brng noise again L3 conv mtr. 6205ZZ replaced. algnmnt checked ok",
      "Bearing replaced", "6205ZZ", 2.4, "Sunil", { f: XLS26, s: "May-2026", r: 37 }),
    R(1198, "CONV-L3-MTR-01", "2026-04-28", "Coupling", "breakdown",
      "L3 conveyor coupling spider worn. rubber replcd. ok",
      "Coupling spider replaced", "Coupling Spider", 1.6, "Prakash", { f: XLS26, s: "Apr-2026", r: 29 }),
    R(1192, "CONV-L3-MTR-01", "2026-04-10", "Preventive", "pm",
      "monthly PM L3 conv motor. greasing done. mounting bolts ok",
      "Preventive maintenance completed", null, 0, "Amit", { f: SAP, s: "PM orders", r: 74 }),
    R(1185, "CONV-L3-MTR-01", "2026-03-19", "Alignment", "breakdown",
      "Motor vibration L3 conv. alignment kiya dial gauge se. vibration kam ho gaya",
      "Alignment corrected", null, 1.8, "Vijay", { f: XLS26, s: "Mar-2026", r: 18 }),
    R(1176, "CONV-L3-MTR-01", "2026-02-11", "Bearing", "breakdown",
      "CONV L3 MTR BRG noise. bearing chng 6205ZZ. ok",
      "Bearing replaced", "6205ZZ", 2.5, "Sunil", { f: XLS26, s: "Feb-2026", r: 9 }),
    R(1170, "CONV-L3-MTR-01", "2026-01-25", "VFD", "breakdown",
      "CONV TRIP L3 VFD",
      "VFD fault reset, parameters verified", null, 1.4, "Amit", { f: SAP, s: "Breakdown orders", r: 512 }),
    R(1164, "CONV-L3-MTR-01", "2026-01-08", "Preventive", "pm",
      "PM L3 conveyor motor. insulation test ok. greasing done",
      "Preventive maintenance completed", null, 0, "Amit", { f: SAP, s: "PM orders", r: 63 }),
    R(1159, "CONV-L3-MTR-01", "2025-12-18", "Coupling", "breakdown",
      "coupling bolt loose L3 conv mtr. tight kiya + locktite lagaya",
      "Coupling bolts tightened", null, 0.9, "Prakash", { f: XLS25, s: "Dec-2025", r: 121 }),
    R(1152, "CONV-L3-MTR-01", "2025-12-02", "Preventive", "pm",
      "L3 conv motor PM. cleaning + greasing. ok",
      "Preventive maintenance completed", null, 0, "Sunil", { f: SAP, s: "PM orders", r: 55 }),
    R(1148, "CONV-L3-MTR-01", "2025-11-11", "Bearing", "breakdown",
      "bearing gaya L3 conveyor motor. naya brg 6205ZZ lagaya. 3 hr gaya",
      "Bearing replaced", "6205ZZ", 3.0, "Rahul", { f: REG25, s: "Register", r: "page 31" }),
    R(1141, "CONV-L3-MTR-01", "2025-10-27", "Preventive", "pm",
      "PM done L3 conv mtr. no abnormality",
      "Preventive maintenance completed", null, 0, "Amit", { f: SAP, s: "PM orders", r: 48 }),
    R(1137, "CONV-L3-MTR-01", "2025-10-09", "Electrical trip", "breakdown",
      "L3 conv MTR overload trip. OLR setting check. reset. chal gaya",
      "Overload relay checked and reset", null, 0.8, "Amit", { f: XLS25, s: "Oct-2025", r: 96 }),
    R(1132, "CONV-L3-MTR-01", "2025-09-22", "Inspection", "inspection",
      "motor bearing sound complaint L3 conv. checked. minor. monitor karna hai",
      "Noise inspection — monitoring", null, 0, "Vijay", { f: REG25, s: "Register", r: "page 24" }),
    R(1127, "CONV-L3-MTR-01", "2025-09-04", "Preventive", "pm",
      "monthly PM L3 conveyor motor done",
      "Preventive maintenance completed", null, 0, "Sunil", { f: SAP, s: "PM orders", r: 41 }),
    R(1120, "CONV-L3-MTR-01", "2025-08-12", "Lubrication", "pm",
      "greasing overdue L3 conv mtr. lubrication done",
      "Lubrication completed", null, 0, "Rahul", { f: XLS25, s: "Aug-2025", r: 72 }),
    R(1114, "CONV-L3-MTR-01", "2025-07-19", "Other", "breakdown",
      "guard vibration noise L3 conv. guard bolts tightened",
      "Guard bolts tightened", null, 0.5, "Prakash", { f: XLS25, s: "Jul-2025", r: 58 }),
    R(1109, "CONV-L3-MTR-01", "2025-06-24", "Preventive", "pm",
      "PM L3 conv motor. greasing + cleaning",
      "Preventive maintenance completed", null, 0, "Amit", { f: SAP, s: "PM orders", r: 33 }),
    R(1103, "CONV-L3-MTR-01", "2025-05-30", "Overheating", "breakdown",
      "MTR temp high L3 conv. vent jaali choke thi. clean. ok",
      "Ventilation cleaned", null, 0.9, "Rahul", { f: XLS25, s: "May-2025", r: 44 }),
    R(1092, "CONV-L3-MTR-01", "2025-04-15", "Inspection", "inspection",
      "quarterly vibration survey L3 conv motor. normal",
      "Vibration survey — normal", null, 0, "Vijay", { f: REG25, s: "Register", r: "page 8" }),

    /* ---- L3 Blower Fan: two 6205ZZ bearing replacements ---- */
    R(1240, "BLWR-L3-01", "2026-07-08", "Bearing", "breakdown",
      "L3 blower fan brg khatam. 6205ZZ change kiya. 2.2 hr",
      "Bearing replaced", "6205ZZ", 2.2, "Rahul", { f: XLS26, s: "Jul-2026", r: 84 }),
    R(1155, "BLWR-L3-01", "2025-12-10", "Bearing", "breakdown",
      "blower L3 BRG noise. bearing replaced 6205ZZ",
      "Bearing replaced", "6205ZZ", 2.4, "Sunil", { f: XLS25, s: "Dec-2025", r: 117 }),

    /* ---- L3 Gearbox: one bearing failure (6207) ---- */
    R(1204, "GRBX-L3-01", "2026-05-02", "Bearing", "breakdown",
      "L3 gearbox input side bearing failure. brg 6207 replaced. shaft ok",
      "Input bearing replaced", "6207 Bearing", 4.2, "Prakash", { f: XLS26, s: "May-2026", r: 22 }),

    /* ---- Compressor #2: 6205ZZ motor bearings ---- */
    R(1245, "COMP-UT-02", "2026-08-08", "Bearing", "breakdown",
      "comp 2 motor DE brg noise. 6205ZZ replcd",
      "Drive-end bearing replaced", "6205ZZ", 1.6, "Sunil", { f: XLS26, s: "Aug-2026", r: 131 }),
    R(1188, "COMP-UT-02", "2026-03-25", "Bearing", "breakdown",
      "compressor #2 NDE bearing change 6205ZZ. ok",
      "Non-drive-end bearing replaced", "6205ZZ", 1.5, "Vijay", { f: XLS26, s: "Mar-2026", r: 26 }),
    R(1145, "COMP-UT-02", "2025-11-02", "Bearing", "breakdown",
      "comp2 brg vibration. bearing replaced 6205ZZ",
      "Bearing replaced", "6205ZZ", 1.8, "Sunil", { f: XLS25, s: "Nov-2025", r: 103 }),
    R(1101, "COMP-UT-02", "2025-05-20", "Bearing", "breakdown",
      "compressor 2 motor bearing 6205ZZ replcd during overhaul",
      "Bearing replaced during overhaul", "6205ZZ", 1.7, "Rahul", { f: XLS25, s: "May-2025", r: 39 }),

    /* ---- Blister Machine 2: sensor saga (temporary fix -> recurrence) ---- */
    R(1229, "BLST-L1-02", "2026-08-06", "Sensor", "breakdown",
      "Blister m/c 2 stopping intermittent. proximity sensor changed. running.",
      "Proximity sensor replaced", "Proximity Sensor", 1.2, "Amit", { f: XLS26, s: "Aug-2026", r: 127 }),
    R(1222, "BLST-L1-02", "2026-07-19", "Sensor", "breakdown",
      "blister 2 phir se ruk raha. sensor clean kiya. baad me again stopped.",
      "Sensor cleaned (temporary fix)", null, 0.8, "Amit", { f: XLS26, s: "Jul-2026", r: 96 }),
    R(1216, "BLST-L1-02", "2026-07-05", "Sensor", "breakdown",
      "Blister m/c 2 stopping intermittent. sensor cleaned. chalu.",
      "Sensor cleaned (temporary fix)", null, 0.6, "Rahul", { f: XLS26, s: "Jul-2026", r: 71 }),

    /* ---- Packing Machine 4: belt slip ---- */
    R(1227, "PACK-L4-04", "2026-08-04", "Belt", "breakdown",
      "packing m/c 4 ka belt slip ho raha tha, tension diya, phir bhi slip, belt change kiya, 1.5 hr gaya",
      "Belt replaced after re-tension failed", "V-Belt A42", 1.5, "Prakash", { f: XLS26, s: "Aug-2026", r: 119 }),

    /* ---- Pump P-201: recurring seal leak (technician hypothesis in raw text) ---- */
    R(1233, "PUMP-UT-201", "2026-08-16", "Leakage", "breakdown",
      "Pump P-201 seal leak again 3rd time this yr. Gland packing replaced. Yadav sir bolte hai shaft me runout hai. Check karna padega.",
      "Gland packing replaced", "Gland Packing", 1.4, "Vijay", { f: XLS26, s: "Aug-2026", r: 148 }),

    /* ---- Press Machine 02 ---- */
    R(1236, "PRSS-L2-02", "2026-08-11", "Leakage", "breakdown",
      "press 02 hyd oil leak from ram seal. seal kit change. 2.6 hr",
      "Hydraulic ram seal kit replaced", "Seal Kit", 2.6, "Prakash", { f: XLS26, s: "Aug-2026", r: 138 }),
  ];

  /* ---------------- Generated filler records ----------------
     Deterministic: template pools cycle by index, dates step back from the
     machine's anchor. No Math.random anywhere. */

  const POOLS = {
    rotary: [
      { mode: "Preventive", kind: "pm", raw: "PM done. greasing + visual check. ok", action: "Preventive maintenance completed", part: null, dt: 0 },
      { mode: "Lubrication", kind: "pm", raw: "greasing schedule. lubrication done", action: "Lubrication completed", part: null, dt: 0 },
      { mode: "Overheating", kind: "breakdown", raw: "running hot. cooling passage clean kiya. temp normal", action: "Cooling system cleaned", part: null, dt: 1.0 },
      { mode: "Inspection", kind: "inspection", raw: "routine vibration check. within limit", action: "Inspection completed", part: null, dt: 0 },
      { mode: "Electrical trip", kind: "breakdown", raw: "overload trip. contactor check. reset kiya", action: "Contactor checked, reset", part: "Contactor", dt: 0.7 },
    ],
    packaging: [
      { mode: "Preventive", kind: "pm", raw: "weekly PM done. cleaning + settings verify", action: "Preventive maintenance completed", part: null, dt: 0 },
      { mode: "Belt", kind: "breakdown", raw: "belt slip. tension adjust kiya. ok", action: "Belt re-tensioned", part: "V-Belt A42", dt: 1.0 },
      { mode: "Sensor", kind: "breakdown", raw: "sensor fault stop. sensor clean + realign", action: "Sensor cleaned and realigned", part: "Proximity Sensor", dt: 0.7 },
      { mode: "Inspection", kind: "inspection", raw: "changeover inspection. no abnormality", action: "Inspection completed", part: null, dt: 0 },
      { mode: "Electrical trip", kind: "breakdown", raw: "MCB trip. wiring check. reset", action: "Breaker reset after check", part: null, dt: 0.6 },
    ],
    press: [
      { mode: "Preventive", kind: "pm", raw: "PM done. lubrication + punch inspection", action: "Preventive maintenance completed", part: null, dt: 0 },
      { mode: "Overheating", kind: "breakdown", raw: "oil temp high. cooler clean kiya", action: "Oil cooler cleaned", part: null, dt: 1.2 },
      { mode: "Electrical trip", kind: "breakdown", raw: "main drive trip. contactor replaced", action: "Contactor replaced", part: "Contactor", dt: 0.9 },
      { mode: "Inspection", kind: "inspection", raw: "tooling inspection done. ok", action: "Inspection completed", part: null, dt: 0 },
      { mode: "Lubrication", kind: "pm", raw: "lubrication points topped up", action: "Lubrication completed", part: null, dt: 0 },
    ],
    fluid: [
      { mode: "Preventive", kind: "pm", raw: "PM done. filter + oil level check", action: "Preventive maintenance completed", part: null, dt: 0 },
      { mode: "Leakage", kind: "breakdown", raw: "seepage observed. gland tightened", action: "Gland tightened", part: "Gland Packing", dt: 0.5 },
      { mode: "Overheating", kind: "breakdown", raw: "discharge temp high. cooler tubes cleaned", action: "Cooler tubes cleaned", part: null, dt: 1.1 },
      { mode: "Inspection", kind: "inspection", raw: "routine round reading normal", action: "Inspection completed", part: null, dt: 0 },
      { mode: "Lubrication", kind: "pm", raw: "oil top-up done", action: "Oil top-up completed", part: null, dt: 0 },
    ],
  };

  const GEN_SOURCES = [
    { f: XLS26, s: "2026", r: null },
    { f: SAP, s: "PM orders", r: null },
    { f: XLS25, s: "2025", r: null },
    { f: REG25, s: "Register", r: null },
  ];

  function isoAddDays(iso, delta) {
    const d = new Date(iso + "T00:00:00");
    d.setDate(d.getDate() + delta);
    return d.toISOString().slice(0, 10);
  }

  function hashStr(s) {
    let h = 0;
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
    return h;
  }

  const generated = [];
  let genId = 101;
  machines.forEach((m) => {
    const pool = POOLS[m.pool];
    const off = hashStr(m.id);
    for (let i = 0; i < m.gen; i++) {
      const t = pool[(i + off) % pool.length];
      const date = isoAddDays(m.anchor, -i * m.step);
      const srcT = GEN_SOURCES[(i + off) % GEN_SOURCES.length];
      const src = { f: srcT.f, s: srcT.s, r: srcT.r || 20 + ((off + i * 13) % 320) };
      generated.push({
        id: genId++,
        machineId: m.id,
        date,
        mode: t.mode,
        kind: t.kind,
        raw: t.raw,
        normalized: t.action,
        part: t.part,
        downtime: t.dt === 0 ? 0 : Math.round((t.dt + ((off + i) % 5) * 0.1) * 10) / 10,
        tech: technicians[(i + off) % technicians.length],
        src,
      });
    }
  });

  const baseRecords = handRecords.concat(generated);

  /* ---------------- Record added through the WhatsApp agent ---------------- */

  const todayIso = new Date().toISOString().slice(0, 10);
  const whatsappRecord = {
    id: 2048,
    machineId: "CONV-L3-MTR-01",
    date: todayIso,
    mode: "Bearing",
    kind: "breakdown",
    raw: "Line 3 ka conveyor motor band tha, bearing change kiya, alignment check kiya, ab chal raha hai. Downtime: 2 ghante.",
    normalized: "Bearing replaced, alignment checked",
    part: "6205ZZ",
    downtime: 2.0,
    tech: "Ramesh (Technician)",
    src: { f: "WhatsApp Entry Agent", s: "Conversation", r: "#2048" },
    isNew: true,
  };

  /* ---------------- Validation queue ----------------
     43 records need review after import: 37 share the unresolved machine
     alias "Conv Motor-3" (bulk-mappable), 6 need individual review. */

  const validationItems = [
    {
      id: "vq-1", conf: 92,
      raw: "MTR brng noise L3 conv,\nreplcd 6205ZZ,\nalgnmnt chk, OK",
      src: "maintenance_log_2023.xlsx → Sheet March-2023 → Row 143",
      fields: { Machine: "Line 3 Conveyor Motor", "Failure Mode": "Bearing Noise", Part: "6205ZZ", Action: "Bearing replaced", Downtime: "2.5 hrs" },
      note: "Machine matched via alias “L3 conv”. Downtime inferred from shift log gap.",
    },
    {
      id: "vq-2", conf: 88,
      raw: "packing m/c 4 ka belt slip ho raha tha,\ntension diya, phir bhi slip,\nbelt change kiya, 1.5 hr gaya",
      src: "maintenance_log_2023.xlsx → Sheet July-2023 → Row 82",
      fields: { Machine: "Packing Machine 4", "Failure Mode": "Belt Slip", Part: "V-Belt A42", Action: "Belt replaced", Downtime: "1.5 hrs" },
      note: "Two actions detected: re-tension (failed) then replacement. Kept final action.",
    },
    {
      id: "vq-3", conf: 64,
      raw: "CONV TRIP L3 VFD",
      src: "maintenance_log_2023.xlsx → Sheet Feb-2023 → Row 17",
      fields: { Machine: "Line 3 Conveyor Motor", "Failure Mode": "VFD Trip", Part: "—", Action: "Not stated", Downtime: "Not stated" },
      note: "Very short entry. Action and downtime missing — needs human input.",
    },
    {
      id: "vq-4", conf: 85,
      raw: "Pump P-201 seal leak again 3rd time this yr.\nGland packing replaced.\nYadav sir bolte hai shaft me runout hai. Check karna padega.",
      src: "maintenance_log_2023.xlsx → Sheet Sep-2023 → Row 201",
      fields: { Machine: "Pump P-201", "Failure Mode": "Seal Leakage", Part: "Gland Packing", Action: "Gland packing replaced", Downtime: "Not stated" },
      note: "Technician hypothesis (“shaft runout”) captured as a note, not as a confirmed cause.",
    },
    {
      id: "vq-5", conf: 90,
      raw: "Blister m/c 2 stopping intermittent. sensor cleaned. again stopped.\nproximity sensor changed. running.",
      src: "maintenance_log_2023.xlsx → Sheet Nov-2023 → Row 240",
      fields: { Machine: "Blister Machine 2", "Failure Mode": "Sensor Fault", Part: "Proximity Sensor", Action: "Sensor replaced after cleaning failed", Downtime: "Not stated" },
      note: "Entry describes two events. Suggest splitting into 2 records on approval.",
    },
    {
      id: "vq-6", conf: 71,
      raw: "comp 2 unload valve leak, kit change, 1 hr approx",
      src: "maintenance_log_2023.xlsx → Sheet May-2023 → Row 133",
      fields: { Machine: "Compressor #2", "Failure Mode": "Valve Leakage", Part: "Valve Kit", Action: "Unloader valve kit replaced", Downtime: "1.0 hr (approx.)" },
      note: "Downtime marked approximate in the original entry.",
    },
  ];

  const aliasGroup = {
    alias: "Conv Motor-3",
    count: 37,
    target: "CONV-L3-MTR-01",
    targetName: "Line 3 Conveyor Motor",
    samples: [
      "Conv Motor-3 brg noise chk",
      "Conv Motor-3 trip VFD reset",
      "Conv Motor-3 greasing done",
    ],
  };

  const importSummary = { detected: 1842, usable: 1790, skipped: 52, review: 43, file: "maintenance_log_2023.xlsx", size: "2.4 MB" };

  /* ---------------- Derived data API ---------------- */

  const state = () => LS.state; // set up in app.js before views run

  function allRecords() {
    const recs = baseRecords.slice();
    if (LS.state && LS.state.get("whatsappAdded")) recs.push(whatsappRecord);
    return recs;
  }

  function recordsFor(machineId) {
    return allRecords()
      .filter((r) => r.machineId === machineId)
      .sort((a, b) => (a.date < b.date ? 1 : -1));
  }

  function recordById(id) {
    return allRecords().find((r) => String(r.id) === String(id)) || null;
  }

  function machineById(id) {
    return machines.find((m) => m.id === id) || null;
  }

  function machineStatus(m) {
    if (m.id === "CONV-L3-MTR-01" && LS.state && LS.state.get("whatsappAdded")) return "Recurring";
    return m.status;
  }

  function machineAliases(m) {
    const a = (m.aliases || []).slice();
    if (m.id === "CONV-L3-MTR-01" && LS.state && LS.state.get("aliasMapped")) a.push(aliasGroup.alias);
    return a;
  }

  const round1 = (n) => Math.round(n * 10) / 10;

  function machineStats(machineId) {
    const recs = recordsFor(machineId);
    const breakdowns = recs.filter((r) => r.kind === "breakdown");
    const downtime = round1(breakdowns.reduce((s, r) => s + r.downtime, 0));
    const mttr = breakdowns.length ? round1(downtime / breakdowns.length) : 0;
    // MTBF over the observed record window (calendar hours between breakdowns)
    let mtbf = null;
    if (breakdowns.length >= 2) {
      const dates = breakdowns.map((r) => new Date(r.date)).sort((a, b) => a - b);
      const spanHrs = (dates[dates.length - 1] - dates[0]) / 36e5;
      mtbf = Math.round(spanHrs / (breakdowns.length - 1));
    }
    const modeCounts = {};
    breakdowns.forEach((r) => { modeCounts[r.mode] = (modeCounts[r.mode] || 0) + 1; });
    const repeats = Object.values(modeCounts).filter((c) => c >= 2).reduce((s, c) => s + c, 0);
    return {
      records: recs.length,
      breakdowns: breakdowns.length,
      downtime,
      mttr,
      mtbf,
      lastEvent: recs[0] ? recs[0].date : null,
      lastFailure: breakdowns[0] ? breakdowns[0].date : null,
      repeatFailures: repeats,
      modeCounts,
    };
  }

  function pareto(machineId) {
    const recs = machineId ? recordsFor(machineId) : allRecords();
    const counts = {};
    recs.filter((r) => r.kind === "breakdown").forEach((r) => {
      counts[r.mode] = (counts[r.mode] || 0) + 1;
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }

  function topMachinesByDowntime(n) {
    const rows = machines.map((m) => {
      const st = machineStats(m.id);
      return { machine: m, downtime: st.downtime, breakdowns: st.breakdowns };
    });
    rows.sort((a, b) => b.downtime - a.downtime);
    return rows.slice(0, n || 5);
  }

  /* Bearing analytics used by the assistant (deterministic) */

  function bearingRecords(scope) {
    return allRecords().filter((r) => {
      if (r.mode !== "Bearing" || r.kind !== "breakdown") return false;
      if (scope === "line3") {
        const m = machineById(r.machineId);
        return m && m.line === "Line 3";
      }
      return true;
    });
  }

  function bearingStats(scope) {
    const recs = bearingRecords(scope).sort((a, b) => (a.date < b.date ? 1 : -1));
    const withDt = recs.filter((r) => r.downtime > 0);
    return {
      records: recs,
      count: recs.length,
      withDowntime: withDt.length,
      downtime: round1(recs.reduce((s, r) => s + r.downtime, 0)),
    };
  }

  function bearingRecurrence() {
    // Bearing replacements on the L3 conveyor motor, oldest -> newest
    const recs = recordsFor("CONV-L3-MTR-01")
      .filter((r) => r.mode === "Bearing" && r.kind === "breakdown")
      .sort((a, b) => (a.date > b.date ? 1 : -1));
    if (recs.length < 2) return null;
    const gaps = [];
    for (let i = 1; i < recs.length; i++) {
      gaps.push((new Date(recs[i].date) - new Date(recs[i - 1].date)) / 864e5);
    }
    const avg = Math.round(gaps.reduce((s, g) => s + g, 0) / gaps.length);
    const spanMonths = Math.max(1, Math.round(
      (new Date(recs[recs.length - 1].date) - new Date(recs[0].date)) / (30.44 * 864e5)
    ));
    return { records: recs, count: recs.length, avgDays: avg, spanMonths, first: recs[0].date, last: recs[recs.length - 1].date };
  }

  /* Spare parts */

  function partStats() {
    const map = {};
    allRecords().forEach((r) => {
      if (!r.part) return;
      const p = (map[r.part] = map[r.part] || { part: r.part, machines: new Set(), count: 0, last: null, records: [] });
      p.machines.add(r.machineId);
      p.count += 1;
      p.records.push(r);
      if (!p.last || r.date > p.last) p.last = r.date;
    });
    return Object.values(map)
      .map((p) => ({
        part: p.part,
        machines: [...p.machines],
        count: p.count,
        last: p.last,
        records: p.records.sort((a, b) => (a.date < b.date ? 1 : -1)),
        trend: p.count >= 9 ? "High" : p.count >= 6 ? "Medium" : "Stable",
      }))
      .sort((a, b) => b.count - a.count);
  }

  function partByName(name) {
    const q = name.toLowerCase();
    return partStats().find((p) => p.part.toLowerCase().includes(q)) || null;
  }

  /* Hybrid search with technician-shorthand synonyms */

  const SYNONYMS = {
    brng: "bearing", brg: "bearing", bearng: "bearing", "bearing gaya": "bearing",
    mtr: "motor", conv: "conveyor", "m/c": "machine", mc: "machine",
    replcd: "replaced", chng: "changed", algnmnt: "alignment", chk: "check",
    comp: "compressor", hyd: "hydraulic",
  };

  function normalizeQuery(q) {
    let s = " " + q.toLowerCase() + " ";
    Object.entries(SYNONYMS).forEach(([k, v]) => {
      s = s.split(" " + k + " ").join(" " + v + " ");
    });
    return s.trim();
  }

  function expandTerm(term) {
    const out = new Set([term]);
    Object.entries(SYNONYMS).forEach(([k, v]) => {
      if (v === term) out.add(k);
      if (k === term) out.add(v);
    });
    if (term === "bearing") ["brng", "brg", "bearng"].forEach((t) => out.add(t));
    return [...out];
  }

  function searchRecords(query, limit) {
    const terms = normalizeQuery(query).split(/\s+/).filter((t) => t.length >= 2);
    if (!terms.length) return [];
    const scored = [];
    allRecords().forEach((r) => {
      const m = machineById(r.machineId);
      const hay = (r.raw + " " + r.normalized + " " + r.mode + " " + (r.part || "") + " " + (m ? m.name + " " + m.type + " " + m.line : "")).toLowerCase();
      let score = 0;
      const why = new Set();
      terms.forEach((t) => {
        const variants = expandTerm(t);
        const hit = variants.some((v) => hay.includes(v));
        if (!hit) return;
        score += 1;
        if (r.mode.toLowerCase().includes(t) || variants.some((v) => r.raw.toLowerCase().includes(v) && v !== t)) why.add("Same failure mode");
        if (m && (m.type.toLowerCase().includes(t) || m.name.toLowerCase().includes(t))) why.add("Same machine type");
        if (r.part && r.part.toLowerCase().includes(t)) why.add("Shared part");
        if (variants.length > 1 && variants.some((v) => v !== t && r.raw.toLowerCase().includes(v))) why.add("Semantic similarity (shorthand)");
      });
      if (score > 0) scored.push({ record: r, score, why: [...why] });
    });
    scored.sort((a, b) => b.score - a.score || (a.record.date < b.record.date ? 1 : -1));
    return scored.slice(0, limit || 8);
  }

  function searchMachines(query) {
    const q = normalizeQuery(query);
    return machines.filter((m) => {
      const hay = (m.name + " " + m.id + " " + m.type + " " + m.line + " " + machineAliases(m).join(" ")).toLowerCase();
      return q.split(/\s+/).some((t) => t.length >= 2 && hay.includes(t));
    });
  }

  /* Formatting helpers */

  const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  function fmtDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso + "T00:00:00");
    return d.getDate() + " " + MONTHS[d.getMonth()] + " " + d.getFullYear();
  }
  function isToday(iso) { return iso === todayIso; }
  function fmtHrs(n) { return (Math.round(n * 10) / 10) + " hrs"; }

  /* Exports */

  LS.data = {
    plant, technicians, lines, machines,
    validationItems, aliasGroup, importSummary, whatsappRecord,
    allRecords, recordsFor, recordById, machineById, machineStatus, machineAliases,
    machineStats, pareto, topMachinesByDowntime,
    bearingStats, bearingRecurrence, partStats, partByName,
    searchRecords, searchMachines, normalizeQuery,
    fmtDate, fmtHrs, isToday, todayIso, round1,
  };
})();
