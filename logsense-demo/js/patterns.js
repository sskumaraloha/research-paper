/* ==========================================================================
   LogSense demo — Patterns & Insights + Spare Parts Intelligence
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  /* ---------------- Patterns ---------------- */

  LS.views.patterns = {
    render(host) {
      const rec = D().bearingRecurrence();
      const sensor = D().recordsFor("BLST-L1-02").filter((r) => r.mode === "Sensor" && r.kind === "breakdown");
      const part = D().partByName("6205ZZ");
      const line3Machines = D().machines.filter((m) => m.line === "Line 3");
      const line3Downtime = D().round1(line3Machines.reduce((s, m) => s + D().machineStats(m.id).downtime, 0));
      const plantDowntime = D().round1(D().machines.reduce((s, m) => s + D().machineStats(m.id).downtime, 0));
      const line3Share = Math.round((line3Downtime / plantDowntime) * 100);
      const pump = D().recordById(1233);
      const isNew = LS.state.get("whatsappAdded");

      host.innerHTML =
        '<div class="page-head"><div><h1>Failure Patterns</h1>' +
        '<p class="page-sub">Patterns detected from the recorded history, with evidence counts. Hypotheses stay hypotheses — nothing here is a confirmed root cause.</p></div>' +
        '<div class="page-actions"><span class="flex">' + LS.trustBadge("FACT") + LS.trustBadge("CALCULATED") + LS.trustBadge("HYPOTHESIS") + "</span></div></div>" +

        '<div class="grid grid-2">' +

        '<article class="insight-card insight-recur">' +
        '<div class="insight-kicker"><span>Recurring failure</span>' + LS.trustBadge("HYPOTHESIS") + "</div>" +
        "<h3>Bearing recurrence — ~" + rec.avgDays + " days</h3>" +
        '<a class="insight-machine" href="#/machine/CONV-L3-MTR-01">Line 3 Conveyor Motor</a>' +
        '<div class="pattern-evidence"><span><b>' + rec.count + "</b>events</span><span><b>" + rec.spanMonths + "</b>months</span><span><b>~" + rec.avgDays + "</b>day interval</span></div>" +
        "<p>Bearing replaced " + rec.count + " times" + (isNew ? " (including today’s WhatsApp record #2048)" : "") +
        ". Alignment was checked multiple times but the failure returned. Possible mounting/alignment issue — requires engineering validation." +
        (isNew ? ' <span class="badge badge-review">' + icon("eye", "ic-sm") + "Engineering review required</span>" : "") + "</p>" +
        '<div class="cite-row">' + rec.records.map((r) => '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button>").join("") + "</div>" +
        "</article>" +

        '<article class="insight-card">' +
        '<div class="insight-kicker"><span>Temporary fix recurrence</span>' + LS.trustBadge("FACT") + "</div>" +
        "<h3>Sensor cleaning followed by repeat stoppage</h3>" +
        '<a class="insight-machine" href="#/machine/BLST-L1-02">Blister Machine 2</a>' +
        '<div class="pattern-evidence"><span><b>' + sensor.length + "</b>stoppages</span><span><b>2</b>temporary fixes</span><span><b>1</b>replacement</span></div>" +
        "<p>Sensor cleaning was recorded twice; each time the machine stopped again within two weeks, until the proximity sensor was replaced on " +
        D().fmtDate(sensor[0].date) + ".</p>" +
        '<div class="cite-row">' + sensor.map((r) => '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button>").join("") + "</div>" +
        "</article>" +

        '<article class="insight-card insight-part">' +
        '<div class="insight-kicker"><span>Repeated spare part</span>' + LS.trustBadge("CALCULATED") + "</div>" +
        "<h3>6205ZZ bearing used across machines</h3>" +
        '<a class="insight-machine" href="#/parts">Spare Parts Intelligence</a>' +
        '<div class="pattern-evidence"><span><b>' + part.machines.length + "</b>machines</span><span><b>" + part.count + "</b>replacements</span><span><b>" + D().fmtDate(part.last) + "</b>last used</span></div>" +
        "<p>The same bearing spec appears on the L3 conveyor motor, L3 blower fan and Compressor #2 — a candidate for stocking review and failure comparison.</p>" +
        '<div class="cite-row">' + part.records.slice(0, 5).map((r) => '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button>").join("") + "</div>" +
        "</article>" +

        '<article class="insight-card">' +
        '<div class="insight-kicker"><span>Downtime hotspot</span>' + LS.trustBadge("CALCULATED") + "</div>" +
        "<h3>Line 3 — highest downtime contribution</h3>" +
        '<a class="insight-machine" href="#/machines">Machine Master · Line 3</a>' +
        '<div class="pattern-evidence"><span><b>' + line3Downtime.toFixed(1) + "</b>hrs recorded</span><span><b>" + line3Share + "%</b>of sampled downtime</span><span><b>" + line3Machines.length + "</b>machines</span></div>" +
        "<p>Line 3 accounts for the largest share of recorded downtime in the sampled history, led by the conveyor motor and gearbox unit.</p>" +
        "</article>" +

        '<article class="insight-card">' +
        '<div class="insight-kicker"><span>Technician knowledge captured</span>' + LS.trustBadge("HYPOTHESIS") + "</div>" +
        "<h3>“Shaft runout” note on Pump P-201</h3>" +
        '<a class="insight-machine" href="#/machine/PUMP-UT-201">Pump P-201</a>' +
        '<div class="pattern-evidence"><span><b>3</b>seal leaks this year</span><span><b>1</b>technician hypothesis</span></div>' +
        "<p>A technician noted in the log: “Yadav sir bolte hai shaft me runout hai.” LogSense preserved this as a hypothesis attached to the recurring seal-leak pattern — not as a confirmed cause.</p>" +
        '<div class="cite-row"><button class="cite-chip" onclick="LS.openRecord(' + pump.id + ')">' + icon("file") + "#" + pump.id + "</button></div>" +
        "</article>" +
        "</div>";
    },
  };

  /* ---------------- Spare Parts ---------------- */

  function partDrawer(partName) {
    const p = D().partByName(partName);
    if (!p) return;
    LS.openDrawer(
      '<div class="drawer-head"><h2>' + esc(p.part) + '</h2><button class="icon-btn" data-close-drawer aria-label="Close">' + icon("x") + "</button></div>" +
      '<div class="drawer-body">' +
      '<div class="import-result" style="grid-template-columns:1fr 1fr">' +
      '<div class="ir-stat"><b>' + p.machines.length + "</b><span>machines</span></div>" +
      '<div class="ir-stat"><b>' + p.count + "</b><span>recorded replacements</span></div>" +
      "</div>" +
      '<div><div class="raw-label">' + icon("box", "ic-sm") + "Machines using this part</div>" +
      "<ul style='margin:0;padding-left:18px;display:grid;gap:4px'>" + p.machines.map((mid) => {
        const m = D().machineById(mid);
        const n = p.records.filter((r) => r.machineId === mid).length;
        return '<li><a href="#/machine/' + m.id + '" onclick="LS.closeDrawer()"><b>' + esc(m.name) + "</b></a> (" + m.line + ") — " + n + " uses</li>";
      }).join("") + "</ul></div>" +
      '<div><div class="raw-label">' + icon("clock", "ic-sm") + "Replacement timeline</div>" +
      '<div class="timeline">' + p.records.map((r) => {
        const m = D().machineById(r.machineId);
        return '<div class="tl-item tl-breakdown"><span class="tl-dot"></span>' +
          '<div class="tl-date">' + (D().isToday(r.date) ? "Today" : D().fmtDate(r.date)) + "</div>" +
          '<div class="tl-card"><div class="tl-title" style="font-size:13px">' + esc(m ? m.name : "") + "</div>" +
          '<div class="tl-meta"><span>' + esc(r.normalized) + "</span>" +
          '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button></div></div></div>";
      }).join("") + "</div></div>" +
      "<div class='mt-8'>" + LS.trustBadge("CALCULATED") + "</div>" +
      "</div>"
    );
  }
  LS.openPart = partDrawer;

  LS.views.parts = {
    render(host) {
      const parts = D().partStats();
      host.innerHTML =
        '<div class="page-head"><div><h1>Spare Parts Intelligence</h1>' +
        '<p class="page-sub">Part consumption reconstructed from maintenance records — which parts, on which machines, how often.</p></div>' +
        '<div class="page-actions">' + LS.trustBadge("CALCULATED") + "</div></div>" +
        '<div class="card"><div class="table-wrap"><table class="tbl" aria-label="Spare parts">' +
        "<thead><tr><th>Part</th><th class='num'>Machines</th><th class='num'>Usage</th><th>Last used</th><th>Trend</th><th></th></tr></thead><tbody>" +
        parts.map((p) =>
          '<tr class="row-link" tabindex="0" data-part="' + esc(p.part) + '">' +
          "<td><b>" + esc(p.part) + "</b></td>" +
          '<td class="num">' + p.machines.length + "</td>" +
          '<td class="num">' + p.count + "</td>" +
          "<td>" + D().fmtDate(p.last) + "</td>" +
          "<td>" + trendBadge(p.trend) + "</td>" +
          '<td><button class="btn btn-sm btn-ghost">' + icon("eye") + "Details</button></td></tr>"
        ).join("") +
        "</tbody></table></div></div>" +
        '<p class="small muted mt-14">Usage counts come from records that explicitly mention the part — a conservative lower bound on real consumption.</p>';

      host.addEventListener("click", (e) => {
        const tr = e.target.closest("tr[data-part]");
        if (tr) partDrawer(tr.dataset.part);
      });
      host.addEventListener("keydown", (e) => {
        const tr = e.target.closest("tr[data-part]");
        if (tr && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); partDrawer(tr.dataset.part); }
      });

      function trendBadge(t) {
        const map = { High: "status-recurring", Medium: "status-attention", Stable: "status-healthy" };
        const icons = { High: "trending", Medium: "activity", Stable: "check" };
        return '<span class="status-badge ' + map[t] + '">' + icon(icons[t]) + t + "</span>";
      }
    },
  };
})();
