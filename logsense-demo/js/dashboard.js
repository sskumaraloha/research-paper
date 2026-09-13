/* ==========================================================================
   LogSense demo — Plant Dashboard view
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;

  function kpiCard(label, value, unit, delta, goodWhenDown, foot, route) {
    let deltaHtml = "";
    if (delta != null) {
      const down = delta < 0;
      const good = goodWhenDown ? down : !down;
      deltaHtml = '<div class="kpi-delta ' + (down ? "down" : "up") + (good ? "-good" : "-bad") + '">' +
        icon(down ? "arrowdown" : "arrowup", "ic-sm") + Math.abs(delta).toFixed(1) + '% <span class="vs">vs previous period</span></div>';
    }
    return '<button class="kpi" onclick="LS.go(\'' + route + '\')" aria-label="' + esc(label) + '">' +
      '<div class="kpi-label">' + esc(label) + "</div>" +
      '<div class="kpi-value">' + value + (unit ? "<small>" + unit + "</small>" : "") + "</div>" +
      deltaHtml + (foot ? '<div class="kpi-foot">' + foot + "</div>" : "") +
      "</button>";
  }

  function insightCards() {
    const rec = LS.data.bearingRecurrence();
    const sensor = LS.data.recordsFor("BLST-L1-02").filter((r) => r.mode === "Sensor" && r.kind === "breakdown");
    const part = LS.data.partByName("6205ZZ");
    const line3Share = part ? Math.round(
      part.records.filter((r) => (LS.data.machineById(r.machineId) || {}).line === "Line 3").length / part.count * 100
    ) : 0;
    const isNew = LS.state.get("whatsappAdded");

    return (
      '<div class="grid grid-3">' +

      '<article class="insight-card insight-recur">' +
      '<div class="insight-kicker"><span>Recurring bearing failure</span>' + LS.trustBadge("HYPOTHESIS") + "</div>" +
      '<h3>Possible ~' + rec.avgDays + "-day bearing recurrence</h3>" +
      '<a class="insight-machine" href="#/machine/CONV-L3-MTR-01">Line 3 Conveyor Motor</a>' +
      "<p><b>" + rec.count + " bearing replacements</b> in " + rec.spanMonths + " months" +
      (isNew ? ' — including today’s record <span class="badge badge-new">' + icon("zap", "ic-sm") + "NEW</span>" : "") +
      ". Alignment was checked after each replacement, yet the failure returned. Possible mounting/alignment issue — requires engineering validation.</p>" +
      '<div class="insight-evidence">' + icon("database") + rec.count + " evidence records · " + LS.data.fmtDate(rec.first) + " – " + LS.data.fmtDate(rec.last) + "</div>" +
      '<a class="btn btn-sm" href="#/machine/CONV-L3-MTR-01">' + icon("eye") + "View evidence</a>" +
      "</article>" +

      '<article class="insight-card">' +
      '<div class="insight-kicker"><span>Repeated sensor failure</span>' + LS.trustBadge("FACT") + "</div>" +
      "<h3>Temporary fix followed by recurrence</h3>" +
      '<a class="insight-machine" href="#/machine/BLST-L1-02">Blister Machine 2</a>' +
      "<p><b>" + sensor.length + " sensor-related stoppages</b> since " + LS.data.fmtDate(sensor[sensor.length - 1].date) +
      ". Cleaning was done twice; the machine stopped again each time until the proximity sensor was replaced.</p>" +
      '<div class="insight-evidence">' + icon("database") + sensor.length + " evidence records</div>" +
      '<a class="btn btn-sm" href="#/machine/BLST-L1-02">' + icon("clock") + "View history</a>" +
      "</article>" +

      '<article class="insight-card insight-part">' +
      '<div class="insight-kicker"><span>Spare part concentration</span>' + LS.trustBadge("CALCULATED") + "</div>" +
      "<h3>6205ZZ bearing consumption</h3>" +
      '<a class="insight-machine" href="#/parts">6205ZZ Deep-Groove Bearing</a>' +
      "<p>Used across <b>" + part.machines.length + " machines</b> (" + part.count + " recorded replacements). Line 3 accounts for <b>" +
      line3Share + "%</b> of recorded consumption.</p>" +
      '<div class="insight-evidence">' + icon("database") + part.count + " evidence records</div>" +
      '<a class="btn btn-sm" href="#/parts">' + icon("wrench") + "View part usage</a>" +
      "</article>" +
      "</div>"
    );
  }

  LS.views.dashboard = {
    render(host) {
      const p = LS.data.plant;
      const k = p.kpis;
      const pendingV = LS.pendingValidationCount();
      const top = LS.data.topMachinesByDowntime(5);
      const pareto = LS.data.pareto(null).slice(0, 7);

      host.innerHTML =
        '<div class="page-head"><div>' +
        "<h1>Plant Maintenance Intelligence</h1>" +
        '<p class="page-sub">' + esc(p.shortName) + " · " + esc(p.period) + " · demo dataset — not real customer data</p>" +
        "</div>" +
        '<div class="page-actions">' +
        '<a class="btn" href="#/how-it-works">' + icon("cpu") + "How it works</a>" +
        '<a class="btn btn-primary" href="#/upload">' + icon("upload") + "Upload historical data</a>" +
        "</div></div>" +

        '<section class="grid grid-kpi mb-20" aria-label="Key performance indicators">' +
        kpiCard("MTTR", k.mttr.value, " hrs", k.mttr.delta, true, null, "history") +
        kpiCard("MTBF", k.mtbf.value, " hrs", k.mtbf.delta, false, null, "history") +
        kpiCard("Downtime", k.downtime.value, " hrs", k.downtime.delta, true, null, "history") +
        kpiCard("Breakdown events", k.breakdowns.value, "", k.breakdowns.delta, true, null, "history") +
        kpiCard("Records indexed", k.recordsIndexed.toLocaleString("en-IN"), "", null, false, "Excel · SAP · PDF · registers · WhatsApp", "upload") +
        kpiCard("Validation pending", pendingV, "", null, false, "Below auto-approval confidence", "validation") +
        "</section>" +

        '<section class="mb-20">' +
        '<div class="flex spread mb-14"><h2 style="font-size:16px">' + icon("zap") + " AI-detected maintenance insights</h2>" +
        '<a class="btn btn-sm btn-ghost" href="#/patterns">All patterns ' + icon("chevright", "ic-sm") + "</a></div>" +
        insightCards() +
        "</section>" +

        '<section class="grid grid-2 mb-20">' +
        '<div class="card"><div class="card-head"><h2>Downtime trend</h2><span class="card-note">hrs / month · 2026 · ' + LS.trustBadge("CALCULATED") + '</span></div>' +
        '<div class="card-body"><div class="chart-box"><canvas id="ch-trend" role="img" aria-label="Monthly downtime trend chart"></canvas></div></div></div>' +

        '<div class="card"><div class="card-head"><h2>Top machines by downtime</h2><span class="card-note">from sampled records · ' + LS.trustBadge("CALCULATED") + '</span></div>' +
        '<div class="card-body"><div class="chart-box"><canvas id="ch-top" role="img" aria-label="Top machines by downtime chart"></canvas></div></div></div>' +
        "</section>" +

        '<section class="grid grid-2">' +
        '<div class="card"><div class="card-head"><h2>Failure mode Pareto</h2><span class="card-note">breakdown events · plant-wide · ' + LS.trustBadge("CALCULATED") + '</span></div>' +
        '<div class="card-body"><div class="chart-box"><canvas id="ch-pareto" role="img" aria-label="Failure mode pareto chart"></canvas></div></div></div>' +

        '<div class="card"><div class="card-head"><h2>Where the data comes from</h2><span class="card-note">no sensors · no SAP migration</span></div>' +
        '<div class="card-body" style="display:grid;gap:12px">' +
        '<div class="trust-item"><div class="trust-ic">' + icon("file") + '</div><div><h3>Excel, CSV & SAP exports</h3><p>Historical breakdown logs and PM orders imported as-is.</p></div></div>' +
        '<div class="trust-ic-sep"></div>' +
        '<div class="trust-item"><div class="trust-ic">' + icon("eye") + '</div><div><h3>PDFs & scanned registers</h3><p>Handwritten registers digitized and linked back to page numbers.</p></div></div>' +
        '<div class="trust-item"><div class="trust-ic">' + icon("chat") + '</div><div><h3>WhatsApp technician entries</h3><p>New breakdowns captured conversationally in Hindi/English.</p></div></div>' +
        '<a class="btn btn-sm" href="#/how-it-works">' + icon("cpu") + "See the pipeline</a>" +
        "</div></div>" +
        "</section>";

      // Charts
      LS.chart("ch-trend", {
        type: "line",
        data: {
          labels: p.trend.labels,
          datasets: [{
            label: "Downtime (hrs)",
            data: p.trend.values,
            borderColor: "#2a78d6",
            backgroundColor: "rgba(42,120,214,0.10)",
            fill: true,
            borderWidth: 2,
            pointRadius: 3,
            pointBackgroundColor: "#2a78d6",
            tension: 0.35,
          }],
        },
        options: {
          maintainAspectRatio: false,
          scales: {
            x: { grid: { display: false }, border: { color: "#c3c2b7" } },
            y: { beginAtZero: true, grid: { color: "#e5e6e2" }, border: { display: false }, title: { display: true, text: "hrs", color: "#898781" } },
          },
        },
      });

      LS.chart("ch-top", {
        type: "bar",
        data: {
          labels: top.map((t) => t.machine.name),
          datasets: [{
            label: "Downtime (hrs)",
            data: top.map((t) => t.downtime),
            backgroundColor: "#2a78d6",
            borderRadius: 4,
            borderSkipped: "start",
            barThickness: 16,
          }],
        },
        options: {
          indexAxis: "y",
          maintainAspectRatio: false,
          scales: {
            x: { beginAtZero: true, grid: { color: "#e5e6e2" }, border: { display: false }, title: { display: true, text: "hrs", color: "#898781" } },
            y: { grid: { display: false }, border: { color: "#c3c2b7" }, ticks: { color: "#52514e", font: { size: 11 } } },
          },
          onClick: (evt, els) => {
            if (els.length) LS.go("machine/" + top[els[0].index].machine.id);
          },
        },
      });

      LS.chart("ch-pareto", {
        type: "bar",
        data: {
          labels: pareto.map((x) => x[0]),
          datasets: [{
            label: "Breakdown events",
            data: pareto.map((x) => x[1]),
            backgroundColor: "#2a78d6",
            borderRadius: 4,
            borderSkipped: "start",
            barThickness: 20,
          }],
        },
        options: {
          maintainAspectRatio: false,
          scales: {
            x: { grid: { display: false }, border: { color: "#c3c2b7" }, ticks: { color: "#52514e" } },
            y: { beginAtZero: true, grid: { color: "#e5e6e2" }, border: { display: false }, ticks: { precision: 0 } },
          },
        },
      });
    },
  };
})();
