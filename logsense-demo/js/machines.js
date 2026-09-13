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
      body || '<tr><td colspan="7" class="muted" style="text-align:center;padding:26px">—</td></tr>';
    document.getElementById("machine-count").textContent = rows.length + " / " + D().machines.length;
  }

  LS.views.machines = {
    render(host) {
      const t = LS.t;
      host.innerHTML =
        '<div class="page-head"><div><h1>' + t("machines.title") + "</h1>" +
        '<p class="page-sub">' + t("machines.sub") + "</p></div>" +
        '<div class="page-actions"><a class="btn" href="#/history">' + icon("clock") + t("nav.history") + "</a></div></div>" +

        '<div class="filter-bar" role="search">' +
        '<input type="search" id="mm-q" placeholder="' + t("machines.searchPh") + '" aria-label="Search machines" value="' + esc(mstate.q) + '">' +
        '<select id="mm-line" aria-label="Filter by line"><option value="">' + t("machines.allLines") + "</option>" +
        D().lines.map((l) => '<option' + (mstate.line === l ? " selected" : "") + ">" + l + "</option>").join("") + "</select>" +
        '<select id="mm-status" aria-label="Filter by status"><option value="">' + t("machines.allStatuses") + "</option>" +
        ["Healthy", "Attention", "Recurring"].map((s) => '<option value="' + s + '"' + (mstate.status === s ? " selected" : "") + ">" + t("status." + s) + "</option>").join("") + "</select>" +
        '<select id="mm-sort" aria-label="Sort">' +
        '<option value="records">' + t("lbl.records") + '</option><option value="downtime">' + t("lbl.downtime") + "</option>" +
        '<option value="recent">' + t("lbl.lastEvent") + '</option><option value="name">' + t("lbl.machine") + "</option></select>" +
        '<span class="tbl-count" id="machine-count"></span></div>' +

        '<div class="card"><div class="table-wrap"><table class="tbl" aria-label="Machine master">' +
        "<thead><tr><th>" + t("lbl.machine") + "</th><th>" + t("lbl.line") + "</th><th>" + t("lbl.type") + "</th><th class='num'>" + t("lbl.records") + "</th><th class='num'>" + t("lbl.downtimeHrs") + "</th><th>" + t("lbl.lastEvent") + "</th><th>" + t("lbl.status") + "</th></tr></thead>" +
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
    const t = LS.t;
    const isNew = !!rec.isNew;
    const dateLabel = D().isToday(rec.date) ? t("lbl.today") : D().fmtDate(rec.date);
    return '<div class="tl-item ' + (rec.kind === "breakdown" ? "tl-breakdown" : "") + (isNew ? " tl-new" : "") + '">' +
      '<span class="tl-dot" aria-hidden="true"></span>' +
      '<div class="tl-date">' + dateLabel + (isNew ? " " + LS.trustBadge("NEW") : "") + "</div>" +
      '<div class="tl-card">' +
      '<div class="tl-title">' + (rec.mode === "Preventive" || rec.kind !== "breakdown" ? esc(rec.normalized) : LS.tMode(rec.mode)) +
      (rec.kind === "breakdown" ? ' <span class="badge badge-review" style="text-transform:none">' + t("lbl.breakdown") + "</span>" : "") + "</div>" +
      '<div class="tl-raw">' + esc(rec.raw) + "</div>" +
      '<div class="tl-meta">' +
      "<span>" + t("lbl.action") + ": <b>" + esc(rec.normalized) + "</b></span>" +
      (rec.part ? "<span>" + t("lbl.part") + ": <b>" + esc(rec.part) + "</b></span>" : "") +
      (rec.downtime > 0 ? "<span>" + t("lbl.downtime") + ": <b>" + D().fmtHrs(rec.downtime) + "</b></span>" : "") +
      "<span>" + t("lbl.technician") + ": <b>" + esc(rec.tech) + "</b></span>" +
      LS.srcTag(rec) +
      "</div>" +
      '<div class="tl-actions"><button class="btn btn-sm" onclick="LS.openRecord(' + rec.id + ')">' + icon("eye") + t("btn.viewSource") + "</button></div>" +
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

      const t = LS.t, tf = LS.tf;
      const suggs = LS.i18n.suggestions();
      const askQ = isConv ? suggs[0] : "What happened on " + m.name + " recently?";

      host.innerHTML =
        '<div class="crumb"><a href="#/machines">' + t("nav.machines") + "</a>" + icon("chevright", "ic-sm") + m.line + icon("chevright", "ic-sm") + esc(m.name) + "</div>" +
        '<div class="page-head"><div>' +
        "<h1>" + esc(m.name) + " " + LS.statusBadge(D().machineStatus(m)) + "</h1>" +
        '<p class="page-sub">ID <span class="mono">' + m.id + "</span> · " + m.line + " · " + esc(m.type) +
        " · <b>" + st.records + " " + t("lbl.records") + "</b> · " + t("md.stat.lastFailure") + ": " + D().fmtDate(st.lastFailure) +
        (aliases.length ? ' · <span class="mono">' + aliases.map(esc).join(", ") + "</span>" : "") + "</p></div>" +
        '<div class="page-actions">' +
        '<button class="btn btn-primary" onclick="LS.askAssistant(\'' + esc(askQ).replace(/'/g, "\\'") + '\')">' + icon("chat") + t("btn.askAi") + "</button>" +
        '<button class="btn" onclick="LS.askAssistant(\'Show similar failures\')">' + icon("search") + t("btn.findSimilar") + "</button>" +
        "</div></div>" +

        (showRecurrenceAlert
          ? '<div class="banner banner-critical mb-20" role="alert">' + icon("repeat") +
            "<div><strong>" + tf("md.recurTitle", { n: rec.count, m: rec.spanMonths }) + "</strong>" +
            tf("md.recurBody", { n: rec.count, d: rec.avgDays }) + " " +
            '<div class="banner-actions"><button class="btn btn-sm" onclick="LS.openRecord(2048)">' + icon("eye") + t("btn.viewSource") + " #2048</button>" +
            '<a class="btn btn-sm" href="#/patterns">' + icon("activity") + t("btn.allPatterns") + "</a></div></div></div>"
          : "") +

        '<section class="grid grid-kpi mb-20">' +
        stat("MTTR", st.mttr + " " + t("unit.hrs"), t("md.stat.mttrFoot")) +
        stat("MTBF", st.mtbf ? st.mtbf + " " + t("unit.hrs") : "—", t("md.stat.mtbfFoot")) +
        stat(t("md.stat.breakdowns"), st.breakdowns, t("md.stat.breakdownsFoot")) +
        stat(t("md.stat.totalDt"), st.downtime.toFixed(1) + " " + t("unit.hrs"), t("md.stat.totalDtFoot")) +
        stat(t("md.stat.lastFailure"), D().fmtDate(st.lastFailure), "") +
        stat(t("md.stat.repeat"), st.repeatFailures, t("md.stat.repeatFoot")) +
        "</section>" +

        '<section class="grid grid-main-side mb-20">' +

        '<div class="card"><div class="card-head"><h2>' + t("md.timeline") + '</h2><span class="card-note">' + recs.length + " " + t("md.newestFirst") + "</span></div>" +
        '<div class="card-body"><div class="timeline">' + recs.map(timelineItem).join("") + "</div></div></div>" +

        '<div style="display:grid;gap:14px">' +
        '<div class="card"><div class="card-head"><h2>' + t("dash.paretoTitle") + '</h2><span class="card-note">' + LS.trustBadge("CALCULATED") + "</span></div>" +
        '<div class="card-body"><div class="chart-box" style="height:' + Math.max(170, pareto.length * 34 + 60) + 'px"><canvas id="ch-mpareto" role="img" aria-label="Failure mode pareto for ' + esc(m.name) + '"></canvas></div></div></div>' +

        (isConv && rec
          ? '<div class="card"><div class="card-head"><h2>' + t("md.pattern") + "</h2>" + LS.trustBadge("HYPOTHESIS") + "</div>" +
            '<div class="card-body" style="display:grid;gap:9px">' +
            "<p style='font-size:13.5px'>" + tf("md.patternBody", { n: rec.count, from: D().fmtDate(rec.first), to: D().fmtDate(rec.last), d: rec.avgDays }) + "</p>" +
            "<p class='small muted'>" + t("ai.hypoBearing") + " " + t("ai.hypoDisclaimer") + "</p>" +
            '<div class="cite-row">' + rec.records.map((r) => '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button>").join("") + "</div>" +
            "</div></div>"
          : "") +
        "</div></section>";

      LS.chart("ch-mpareto", {
        type: "bar",
        data: {
          labels: pareto.map((x) => LS.tMode(x[0])),
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
        "<td>" + (D().isToday(r.date) ? "<b>" + LS.t("lbl.today") + "</b>" : D().fmtDate(r.date)) + "</td>" +
        "<td><b>" + (m ? esc(m.name) : "") + "</b><br><span class='muted small'>" + (m ? m.line : "") + "</span></td>" +
        "<td>" + LS.tMode(r.mode) + "</td>" +
        '<td class="raw-cell" title="' + esc(r.raw) + '">' + esc(r.raw) + "</td>" +
        '<td class="num">' + (r.downtime > 0 ? r.downtime.toFixed(1) : "—") + "</td>" +
        "<td>" + LS.srcTag(r) + "</td></tr>";
    }).join("") || '<tr><td colspan="7" class="muted" style="text-align:center;padding:26px">— (“brng”, “bearing gaya”…)</td></tr>';
    document.getElementById("hist-count").textContent =
      rows.length + " " + LS.t("lbl.records") + (rows.length > 60 ? " (1–60)" : "");
  }

  LS.views.history = {
    render(host) {
      const t = LS.t;
      const modes = [...new Set(D().allRecords().map((r) => r.mode))].sort();
      const sources = ["maintenance_log_2026.xlsx", "maintenance_log_2025.xlsx", "SAP_PM_export.csv", "scanned_register", "WhatsApp"];
      host.innerHTML =
        '<div class="page-head"><div><h1>' + t("history.title") + "</h1>" +
        '<p class="page-sub">' + t("history.sub") + "</p></div></div>" +

        '<div class="filter-bar" role="search">' +
        '<input type="search" id="h-q" placeholder="brng, bearing gaya, VFD, 6205ZZ…" aria-label="Search records" style="min-width:260px" value="' + esc(hstate.q) + '">' +
        '<select id="h-machine" aria-label="Filter by machine"><option value="">' + t("lbl.machine") + ": —</option>" +
        D().machines.map((m) => '<option value="' + m.id + '"' + (hstate.machine === m.id ? " selected" : "") + ">" + esc(m.name) + "</option>").join("") + "</select>" +
        '<select id="h-mode" aria-label="Filter by failure mode"><option value="">' + t("lbl.failureMode") + ": —</option>" +
        modes.map((mo) => '<option value="' + mo + '"' + (hstate.mode === mo ? " selected" : "") + ">" + LS.tMode(mo) + "</option>").join("") + "</select>" +
        '<select id="h-source" aria-label="Filter by source"><option value="">' + t("lbl.source") + ": —</option>" +
        sources.map((s) => '<option value="' + s + '"' + (hstate.source === s ? " selected" : "") + ">" + s + "</option>").join("") + "</select>" +
        '<span class="tbl-count" id="hist-count"></span></div>' +

        '<div class="card"><div class="table-wrap"><table class="tbl" aria-label="Maintenance records">' +
        "<thead><tr><th>ID</th><th>" + t("lbl.date") + "</th><th>" + t("lbl.machine") + "</th><th>" + t("lbl.failureMode") + "</th><th>" + t("lbl.originalEntry") + "</th><th class='num'>" + t("lbl.downtime") + "</th><th>" + t("lbl.source") + "</th></tr></thead>" +
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
