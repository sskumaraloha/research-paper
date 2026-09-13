/* ==========================================================================
   LogSense demo — Machine Master, Machine Detail, Maintenance History
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  /* ---------------- Machine Master ---------------- */

  const mstate = { q: "", line: "", status: "", sort: "records" };

  function machineRows() {
    let rows = D().machines.map((m) => ({ m, st: D().machineStats(m.id), status: D().machineStatus(m) }));
    if (mstate.q) {
      const q = D().normalizeQuery(mstate.q);
      rows = rows.filter((r) => {
        const hay = (r.m.name + " " + r.m.id + " " + r.m.type + " " + D().machineAliases(r.m).join(" ")).toLowerCase();
        return q.split(/\s+/).every((t) => !t || hay.includes(t));
      });
    }
    if (mstate.line) rows = rows.filter((r) => r.m.line === mstate.line);
    if (mstate.status) rows = rows.filter((r) => r.status === mstate.status);
    const sorters = {
      records: (a, b) => b.st.records - a.st.records,
      downtime: (a, b) => b.st.downtime - a.st.downtime,
      recent: (a, b) => (a.st.lastEvent < b.st.lastEvent ? 1 : -1),
      name: (a, b) => a.m.name.localeCompare(b.m.name),
    };
    rows.sort(sorters[mstate.sort] || sorters.records);
    return rows;
  }

  function renderMachineTable() {
    const rows = machineRows();
    const body = rows.map((r) =>
      '<tr class="row-link" tabindex="0" role="link" aria-label="Open ' + esc(r.m.name) + '" data-machine="' + r.m.id + '">' +
      "<td><b>" + esc(r.m.name) + '</b><br><span class="muted small mono">' + r.m.id + "</span></td>" +
      "<td>" + r.m.line + "</td>" +
      "<td>" + esc(r.m.type) + "</td>" +
      '<td class="num">' + r.st.records + "</td>" +
      '<td class="num">' + r.st.downtime.toFixed(1) + "</td>" +
      "<td>" + D().fmtDate(r.st.lastEvent) + "</td>" +
      "<td>" + LS.statusBadge(r.status) + "</td></tr>"
    ).join("");
    document.getElementById("machine-tbody").innerHTML =
      body || '<tr><td colspan="7" class="muted" style="text-align:center;padding:26px">No machines match these filters.</td></tr>';
    document.getElementById("machine-count").textContent = rows.length + " of " + D().machines.length + " machines";
  }

  LS.views.machines = {
    render(host) {
      host.innerHTML =
        '<div class="page-head"><div><h1>Machine Master</h1>' +
        '<p class="page-sub">Every machine LogSense resolved from your historical records — aliases, shorthand and all.</p></div>' +
        '<div class="page-actions"><a class="btn" href="#/history">' + icon("clock") + "All maintenance records</a></div></div>" +

        '<div class="filter-bar" role="search">' +
        '<input type="search" id="mm-q" placeholder="Search machines, IDs, aliases…" aria-label="Search machines" value="' + esc(mstate.q) + '">' +
        '<select id="mm-line" aria-label="Filter by line"><option value="">All lines</option>' +
        D().lines.map((l) => '<option' + (mstate.line === l ? " selected" : "") + ">" + l + "</option>").join("") + "</select>" +
        '<select id="mm-status" aria-label="Filter by status"><option value="">All statuses</option>' +
        ["Healthy", "Attention", "Recurring"].map((s) => '<option' + (mstate.status === s ? " selected" : "") + ">" + s + "</option>").join("") + "</select>" +
        '<select id="mm-sort" aria-label="Sort">' +
        '<option value="records">Sort: most records</option><option value="downtime">Sort: downtime</option>' +
        '<option value="recent">Sort: last event</option><option value="name">Sort: name</option></select>' +
        '<span class="tbl-count" id="machine-count"></span></div>' +

        '<div class="card"><div class="table-wrap"><table class="tbl" aria-label="Machine master">' +
        "<thead><tr><th>Machine</th><th>Line</th><th>Type</th><th class='num'>Records</th><th class='num'>Downtime (hrs)</th><th>Last event</th><th>Status</th></tr></thead>" +
        '<tbody id="machine-tbody"></tbody></table></div></div>';

      document.getElementById("mm-q").addEventListener("input", (e) => { mstate.q = e.target.value; renderMachineTable(); });
      document.getElementById("mm-line").addEventListener("change", (e) => { mstate.line = e.target.value; renderMachineTable(); });
      document.getElementById("mm-status").addEventListener("change", (e) => { mstate.status = e.target.value; renderMachineTable(); });
      document.getElementById("mm-sort").addEventListener("change", (e) => { mstate.sort = e.target.value; renderMachineTable(); });
      document.getElementById("mm-sort").value = mstate.sort;
      host.addEventListener("click", (e) => {
        const tr = e.target.closest("tr[data-machine]");
        if (tr) LS.go("machine/" + tr.dataset.machine);
      });
      host.addEventListener("keydown", (e) => {
        const tr = e.target.closest("tr[data-machine]");
        if (tr && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); LS.go("machine/" + tr.dataset.machine); }
      });
      renderMachineTable();
    },
  };

  /* ---------------- Machine Detail ---------------- */

  function timelineItem(rec) {
    const isNew = !!rec.isNew;
    const dateLabel = D().isToday(rec.date) ? "Today" : D().fmtDate(rec.date);
    return '<div class="tl-item ' + (rec.kind === "breakdown" ? "tl-breakdown" : "") + (isNew ? " tl-new" : "") + '">' +
      '<span class="tl-dot" aria-hidden="true"></span>' +
      '<div class="tl-date">' + dateLabel + (isNew ? " " + LS.trustBadge("NEW") : "") + "</div>" +
      '<div class="tl-card">' +
      '<div class="tl-title">' + esc(rec.mode === "Preventive" || rec.kind !== "breakdown" ? rec.normalized : rec.mode) +
      (rec.kind === "breakdown" ? ' <span class="badge badge-review" style="text-transform:none">Breakdown</span>' : "") + "</div>" +
      '<div class="tl-raw">' + esc(rec.raw) + "</div>" +
      '<div class="tl-meta">' +
      "<span>Action: <b>" + esc(rec.normalized) + "</b></span>" +
      (rec.part ? "<span>Part: <b>" + esc(rec.part) + "</b></span>" : "") +
      (rec.downtime > 0 ? "<span>Downtime: <b>" + D().fmtHrs(rec.downtime) + "</b></span>" : "") +
      "<span>Technician: <b>" + esc(rec.tech) + "</b></span>" +
      LS.srcTag(rec) +
      "</div>" +
      '<div class="tl-actions"><button class="btn btn-sm" onclick="LS.openRecord(' + rec.id + ')">' + icon("eye") + "View source</button></div>" +
      "</div></div>";
  }

  LS.views.machine = {
    render(host, machineId) {
      const m = D().machineById(machineId) || D().machineById("CONV-L3-MTR-01");
      const st = D().machineStats(m.id);
      const recs = D().recordsFor(m.id);
      const pareto = D().pareto(m.id);
      const aliases = D().machineAliases(m);
      const isConv = m.id === "CONV-L3-MTR-01";
      const rec = isConv ? D().bearingRecurrence() : null;
      const showRecurrenceAlert = isConv && LS.state.get("whatsappAdded");

      const askQ = isConv
        ? "Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?"
        : "What happened on " + m.name + " recently?";

      host.innerHTML =
        '<div class="crumb"><a href="#/machines">Machines</a>' + icon("chevright", "ic-sm") + m.line + icon("chevright", "ic-sm") + esc(m.name) + "</div>" +
        '<div class="page-head"><div>' +
        "<h1>" + esc(m.name) + " " + LS.statusBadge(D().machineStatus(m)) + "</h1>" +
        '<p class="page-sub">Machine ID <span class="mono">' + m.id + "</span> · " + m.line + " · " + esc(m.type) +
        " · <b>" + st.records + " records</b> · last breakdown " + D().fmtDate(st.lastFailure) +
        (aliases.length ? ' · aliases: <span class="mono">' + aliases.map(esc).join(", ") + "</span>" : "") + "</p></div>" +
        '<div class="page-actions">' +
        '<button class="btn btn-primary" onclick="LS.askAssistant(\'' + esc(askQ).replace(/'/g, "\\'") + '\')">' + icon("chat") + "Ask AI about this machine</button>" +
        '<button class="btn" onclick="LS.askAssistant(\'Show similar failures\')">' + icon("search") + "Find similar failures</button>" +
        "</div></div>" +

        (showRecurrenceAlert
          ? '<div class="banner banner-critical mb-20" role="alert">' + icon("repeat") +
            "<div><strong>Recurrence alert — " + rec.count + "th bearing replacement in " + rec.spanMonths + " months</strong>" +
            "Today’s record #2048 is the " + rec.count + "th bearing replacement on this motor (previous interval ~" + rec.avgDays + " days). " +
            "Status: <b>Requires engineering review</b> — LogSense flags the pattern; it does not confirm a root cause. " +
            '<div class="banner-actions"><button class="btn btn-sm" onclick="LS.openRecord(2048)">' + icon("eye") + "View record #2048</button>" +
            '<a class="btn btn-sm" href="#/patterns">' + icon("activity") + "Open pattern</a></div></div></div>"
          : "") +

        '<section class="grid grid-kpi mb-20">' +
        stat("MTTR", st.mttr + " hrs", "avg repair time (breakdowns)") +
        stat("MTBF", st.mtbf ? st.mtbf + " hrs" : "—", "between breakdowns, observed window") +
        stat("Breakdowns", st.breakdowns, "in record history") +
        stat("Total downtime", st.downtime.toFixed(1) + " hrs", "sum of recorded downtime") +
        stat("Last failure", D().fmtDate(st.lastFailure), "") +
        stat("Repeat failures", st.repeatFailures, "events in modes seen ≥2×") +
        "</section>" +

        '<section class="grid grid-main-side mb-20">' +

        '<div class="card"><div class="card-head"><h2>Maintenance timeline</h2><span class="card-note">' + recs.length + " records · newest first</span></div>" +
        '<div class="card-body"><div class="timeline">' + recs.map(timelineItem).join("") + "</div></div></div>" +

        '<div style="display:grid;gap:14px">' +
        '<div class="card"><div class="card-head"><h2>Failure mode Pareto</h2><span class="card-note">' + LS.trustBadge("CALCULATED") + "</span></div>" +
        '<div class="card-body"><div class="chart-box" style="height:' + Math.max(170, pareto.length * 34 + 60) + 'px"><canvas id="ch-mpareto" role="img" aria-label="Failure mode pareto for ' + esc(m.name) + '"></canvas></div></div></div>' +

        (isConv && rec
          ? '<div class="card"><div class="card-head"><h2>Detected pattern</h2>' + LS.trustBadge("HYPOTHESIS") + "</div>" +
            '<div class="card-body" style="display:grid;gap:9px">' +
            "<p style='font-size:13.5px'><b>" + rec.count + " bearing replacements</b> between " + D().fmtDate(rec.first) + " and " + D().fmtDate(rec.last) +
            " — average interval <b>~" + rec.avgDays + " days</b>.</p>" +
            "<p class='small muted'>Repeated bearing replacement despite alignment checks may indicate a mounting/alignment issue. This is a hypothesis and requires engineering validation.</p>" +
            '<div class="cite-row">' + rec.records.map((r) => '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "Record #" + r.id + "</button>").join("") + "</div>" +
            "</div></div>"
          : "") +
        "</div></section>";

      LS.chart("ch-mpareto", {
        type: "bar",
        data: {
          labels: pareto.map((x) => x[0]),
          datasets: [{
            data: pareto.map((x) => x[1]),
            backgroundColor: "#2a78d6",
            borderRadius: 4,
            borderSkipped: "start",
            barThickness: 14,
          }],
        },
        options: {
          indexAxis: "y",
          maintainAspectRatio: false,
          scales: {
            x: { beginAtZero: true, grid: { color: "#e5e6e2" }, border: { display: false }, ticks: { precision: 0 } },
            y: { grid: { display: false }, border: { color: "#c3c2b7" }, ticks: { color: "#52514e" } },
          },
        },
      });

      function stat(label, value, foot) {
        return '<div class="kpi" style="cursor:default"><div class="kpi-label">' + label + '</div><div class="kpi-value" style="font-size:21px">' + value + "</div>" +
          (foot ? '<div class="kpi-foot">' + foot + "</div>" : "") + "</div>";
      }
    },
  };

  /* ---------------- Maintenance History (all records) ---------------- */

  const hstate = { q: "", machine: "", mode: "", source: "" };

  function historyRows() {
    let rows = D().allRecords().slice().sort((a, b) => (a.date < b.date ? 1 : -1));
    if (hstate.machine) rows = rows.filter((r) => r.machineId === hstate.machine);
    if (hstate.mode) rows = rows.filter((r) => r.mode === hstate.mode);
    if (hstate.source) rows = rows.filter((r) => (r.src.f || "").indexOf(hstate.source) !== -1);
    if (hstate.q) {
      const hits = new Set(D().searchRecords(hstate.q, 500).map((x) => x.record.id));
      rows = rows.filter((r) => hits.has(r.id));
    }
    return rows;
  }

  function renderHistoryTable() {
    const rows = historyRows();
    document.getElementById("hist-tbody").innerHTML = rows.slice(0, 60).map((r) => {
      const m = D().machineById(r.machineId);
      return '<tr class="row-link" tabindex="0" data-record="' + r.id + '">' +
        "<td class='mono small'>#" + r.id + "</td>" +
        "<td>" + (D().isToday(r.date) ? "<b>Today</b>" : D().fmtDate(r.date)) + "</td>" +
        "<td><b>" + (m ? esc(m.name) : "") + "</b><br><span class='muted small'>" + (m ? m.line : "") + "</span></td>" +
        "<td>" + esc(r.mode) + "</td>" +
        '<td class="raw-cell" title="' + esc(r.raw) + '">' + esc(r.raw) + "</td>" +
        '<td class="num">' + (r.downtime > 0 ? r.downtime.toFixed(1) : "—") + "</td>" +
        "<td>" + LS.srcTag(r) + "</td></tr>";
    }).join("") || '<tr><td colspan="7" class="muted" style="text-align:center;padding:26px">No records match. Try shorthand too — “brng”, “bearing gaya”…</td></tr>';
    document.getElementById("hist-count").textContent =
      rows.length + " records" + (rows.length > 60 ? " (showing first 60)" : "");
  }

  LS.views.history = {
    render(host) {
      const modes = [...new Set(D().allRecords().map((r) => r.mode))].sort();
      const sources = ["maintenance_log_2026.xlsx", "maintenance_log_2025.xlsx", "SAP_PM_export.csv", "scanned_register", "WhatsApp"];
      host.innerHTML =
        '<div class="page-head"><div><h1>Maintenance History</h1>' +
        '<p class="page-sub">Normalized records from every source. Search understands technician shorthand and Hinglish — try <b>“bearing gaya”</b> or <b>“brng noise conveyor”</b>.</p></div></div>' +

        '<div class="filter-bar" role="search">' +
        '<input type="search" id="h-q" placeholder="Search records… (brng, bearing gaya, VFD, 6205ZZ)" aria-label="Search records" style="min-width:260px" value="' + esc(hstate.q) + '">' +
        '<select id="h-machine" aria-label="Filter by machine"><option value="">All machines</option>' +
        D().machines.map((m) => '<option value="' + m.id + '"' + (hstate.machine === m.id ? " selected" : "") + ">" + esc(m.name) + "</option>").join("") + "</select>" +
        '<select id="h-mode" aria-label="Filter by failure mode"><option value="">All failure modes</option>' +
        modes.map((mo) => '<option' + (hstate.mode === mo ? " selected" : "") + ">" + mo + "</option>").join("") + "</select>" +
        '<select id="h-source" aria-label="Filter by source"><option value="">All sources</option>' +
        sources.map((s) => '<option value="' + s + '"' + (hstate.source === s ? " selected" : "") + ">" + s + "</option>").join("") + "</select>" +
        '<span class="tbl-count" id="hist-count"></span></div>' +

        '<div class="card"><div class="table-wrap"><table class="tbl" aria-label="Maintenance records">' +
        "<thead><tr><th>ID</th><th>Date</th><th>Machine</th><th>Failure mode</th><th>Original entry</th><th class='num'>Downtime</th><th>Source</th></tr></thead>" +
        '<tbody id="hist-tbody"></tbody></table></div></div>';

      ["h-q", "h-machine", "h-mode", "h-source"].forEach((id) => {
        const el = document.getElementById(id);
        el.addEventListener(id === "h-q" ? "input" : "change", () => {
          hstate.q = document.getElementById("h-q").value;
          hstate.machine = document.getElementById("h-machine").value;
          hstate.mode = document.getElementById("h-mode").value;
          hstate.source = document.getElementById("h-source").value;
          renderHistoryTable();
        });
      });
      host.addEventListener("click", (e) => {
        const tr = e.target.closest("tr[data-record]");
        if (tr) LS.openRecord(tr.dataset.record);
      });
      host.addEventListener("keydown", (e) => {
        const tr = e.target.closest("tr[data-record]");
        if (tr && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); LS.openRecord(tr.dataset.record); }
      });
      renderHistoryTable();
    },
  };
})();
