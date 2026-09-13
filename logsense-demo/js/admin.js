/* ==========================================================================
   LogSense demo — Users & Roles, Settings, How It Works, Closing slide
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  /* ---------------- Users & Roles ---------------- */

  const ROLES = [
    {
      name: "Admin", who: "IT Head / plant systems owner", ic: "shield",
      perms: [["Full access — all modules", true], ["User & role management", true], ["Plant configuration & taxonomy", true], ["Data import & deletion", true]],
    },
    {
      name: "Maintenance Manager", who: "e.g. Rajesh Kumar (this demo login)", ic: "users",
      perms: [["Dashboards & analytics", true], ["Validation queue", true], ["Machines & history", true], ["AI assistant", true], ["User management", false]],
    },
    {
      name: "Engineer", who: "Reliability / maintenance engineer", ic: "wrench",
      perms: [["Validation queue", true], ["Machines & history", true], ["AI assistant", true], ["Plant configuration", false], ["User management", false]],
    },
    {
      name: "Technician", who: "Shop-floor technician", ic: "phone",
      perms: [["Machine history (read)", true], ["New maintenance entry (WhatsApp)", true], ["Validation queue", false], ["Analytics dashboards", false]],
    },
  ];

  LS.views.users = {
    render(host) {
      host.innerHTML =
        '<div class="page-head"><div><h1>' + LS.t("users.title") + "</h1>" +
        '<p class="page-sub">Role-based access for the plant team. UI simulation — accounts are not editable in this demo.</p></div></div>' +
        '<div class="grid grid-2">' +
        ROLES.map((r) =>
          '<div class="card card-pad role-card">' +
          "<h3><span class='trust-ic'>" + icon(r.ic) + "</span>" + r.name + "</h3>" +
          '<div class="role-desc">' + esc(r.who) + "</div>" +
          '<div class="perm-grid">' + r.perms.map(([p, yes]) =>
            '<div class="perm-row ' + (yes ? "yes" : "no") + '">' + icon(yes ? "check" : "x") + p + "</div>").join("") + "</div>" +
          "</div>"
        ).join("") + "</div>" +
        '<div class="card card-pad mt-14"><h2 style="font-size:14.5px;margin-bottom:10px">Active users — ' + esc(D().plant.shortName) + "</h2>" +
        '<div class="table-wrap"><table class="tbl"><thead><tr><th>User</th><th>Role</th><th>Last active</th><th>Channel</th></tr></thead><tbody>' +
        [
          ["Rajesh Kumar", "Maintenance Manager", "Now (this session)", "Web"],
          ["Priya Deshmukh", "Engineer", "Today", "Web"],
          ["Sunil", "Technician", "Today", "WhatsApp"],
          ["Amit", "Technician", "Yesterday", "WhatsApp"],
          ["Ramesh", "Technician", "Today", "WhatsApp"],
          ["S. Iyer", "Admin", "3 days ago", "Web"],
        ].map((u) => "<tr><td><b>" + u[0] + "</b></td><td>" + u[1] + "</td><td>" + u[2] + "</td><td>" + u[3] + "</td></tr>").join("") +
        "</tbody></table></div></div>";
    },
  };

  /* ---------------- Settings ---------------- */

  LS.views.settings = {
    render(host) {
      const aliasMapped = LS.state.get("aliasMapped");
      host.innerHTML =
        '<div class="page-head"><div><h1>' + LS.t("settings.title") + "</h1>" +
        '<p class="page-sub">Plant configuration for ' + esc(D().plant.name) + ".</p></div>" +
        '<div class="page-actions"><button class="btn" onclick="LS.state.reset()">' + icon("refresh") + "Reset demo</button></div></div>" +

        '<div class="grid grid-2" style="align-items:start">' +
        '<div class="card card-pad"><h2 style="font-size:15px;margin-bottom:6px">Plant configuration</h2>' +
        row("Plant", D().plant.name) +
        row("Date format", "DD/MM/YYYY") +
        row("Shift configuration", "A / B / C") +
        row("Operating hours", "24×7") +
        row("Downtime cost", D().plant.downtimeCost) +
        row("Environment", "● Demo — preloaded data, destructive actions disabled") +
        "</div>" +

        '<div style="display:grid;gap:14px">' +
        '<div class="card card-pad"><h2 style="font-size:15px;margin-bottom:6px">Taxonomy — failure modes</h2>' +
        '<div class="flex" style="margin-top:8px">' +
        ["Bearing", "VFD", "Sensor", "Belt", "Coupling", "Alignment", "Overheating", "Electrical trip", "Leakage", "Lubrication"].map((m) =>
          '<span class="file-type">' + m + "</span>").join("") + "</div>" +
        '<p class="small muted mt-8">The normalizer maps shorthand (“brng”, “BRG”, “bearing gaya”) into these canonical modes.</p></div>' +

        '<div class="card card-pad"><h2 style="font-size:15px;margin-bottom:6px">Machine aliases</h2>' +
        '<div class="table-wrap"><table class="tbl"><thead><tr><th>Alias</th><th>Resolves to</th></tr></thead><tbody>' +
        '<tr><td class="mono small">L3 conv · CONV L3 MTR' + (aliasMapped ? " · Conv Motor-3" : "") + "</td><td><b>Line 3 Conveyor Motor</b></td></tr>" +
        '<tr><td class="mono small">packing m/c 4 · PKG-4</td><td><b>Packing Machine 4</b></td></tr>' +
        '<tr><td class="mono small">comp 2 · comp-02</td><td><b>Compressor #2</b></td></tr>' +
        '<tr><td class="mono small">blister m/c 2 · BLST-2</td><td><b>Blister Machine 2</b></td></tr>' +
        "</tbody></table></div>" +
        (aliasMapped
          ? '<p class="small mt-8" style="color:var(--good-text)"><b>“Conv Motor-3”</b> was mapped from the validation queue in this session (37 records).</p>'
          : '<p class="small muted mt-8">Tip: the validation queue is suggesting a new alias — “Conv Motor-3”.</p>') +
        "</div></div></div>";

      function row(label, value) {
        return '<div class="settings-row"><div><div class="s-label">' + esc(label) + '</div></div><div class="s-value">' + esc(value) + "</div></div>";
      }
    },
  };

  /* ---------------- How It Works (technical architecture, business-first) ---------------- */

  LS.views["how-it-works"] = {
    render(host) {
      const stages = [
        { ic: "file", cls: "pipe-src", t: "Excel / CSV / PDF / registers / WhatsApp", s: "Your existing records, exactly as they are today" },
        { ic: "upload", t: "Ingestion", s: "Files parsed sheet by sheet, page by page — nothing is discarded" },
        { ic: "zap", t: "Data normalization", s: "Shorthand, Hinglish and messy text expanded into clean fields" },
        { ic: "box", t: "Machine resolution", s: "“Conv Motor-3”, “L3 conv”, “CONV L3 MTR” → one machine" },
        { ic: "database", t: "Structured maintenance data", s: "Date, machine, failure mode, action, part, downtime, technician" },
        { ic: "search", t: "Search + analytics + AI agent", s: "Hybrid search, deterministic statistics, conversational answers" },
        { ic: "activity", cls: "pipe-out", t: "Machine history · insights · answers", s: "Every answer traceable to its source record" },
      ];
      host.innerHTML =
        '<div class="page-head"><div><h1>' + LS.t("how.title") + "</h1>" +
        '<p class="page-sub">From messy factory history to evidence-backed answers — in 4 weeks, without a single sensor.</p></div></div>' +

        '<div class="grid grid-main-side">' +
        '<div class="pipeline">' +
        stages.map((st, i) =>
          '<div class="pipe-stage ' + (st.cls || "") + '">' + icon(st.ic) + "<div><strong>" + st.t + "</strong><span>" + st.s + "</span></div></div>" +
          (i < stages.length - 1 ? '<div class="pipe-arrow">' + icon("arrowdown") + "</div>" : "")
        ).join("") + "</div>" +

        '<div class="card card-pad" style="display:grid;gap:16px">' +
        '<h2 style="font-size:15px">Why you can trust the answers</h2>' +
        trust("chat", "AI understands language", "The language model handles shorthand, Hinglish, messy text and machine aliases — the hard human part.") +
        trust("barchart", "Statistics are calculated, not generated", "KPI values like downtime totals and MTTR come from deterministic calculations over structured records. The AI never invents numbers.") +
        trust("database", "Every fact is traceable", "Answers cite record IDs. One click opens the original entry — file, sheet and row included.") +
        trust("alert", "Hypotheses stay hypotheses", "Possible root causes are labelled HYPOTHESIS and require engineering validation. They are never auto-promoted to confirmed causes.") +
        trust("checksq", "Humans validate low-confidence data", "Records below the confidence threshold go to the validation queue before entering the index.") +
        '<div class="flex mt-8">' + LS.trustBadge("FACT") + LS.trustBadge("CALCULATED") + LS.trustBadge("HYPOTHESIS") + "</div>" +
        "</div></div>";

      function trust(ic, h, p) {
        return '<div class="trust-item"><div class="trust-ic">' + icon(ic) + "</div><div><h3>" + h + "</h3><p>" + p + "</p></div></div>";
      }
    },
  };

  /* ---------------- Closing slide ---------------- */

  LS.views.closing = {
    render(host) {
      const t = LS.t;
      host.innerHTML =
        '<div class="closing" role="dialog" aria-label="Closing slide">' +
        '<div class="closing-inner">' +
        '<div class="brand" style="border:none;padding:0"><div class="brand-mark">' + icon("layers") + '</div>' +
        '<div><div class="brand-name">LOGSENSE</div><div class="brand-sub">MAINTENANCE INTELLIGENCE</div></div></div>' +
        "<h1>" + t("close.h1") + "</h1>" +
        '<div class="closing-nos">' +
        '<span class="closing-no">' + t("close.no1") + "</span>" +
        '<span class="closing-no">' + t("close.no2") + "</span>" +
        '<span class="closing-no">' + t("close.no3") + "</span>" +
        "</div>" +
        '<p class="closing-line">' + t("close.line") + "</p>" +
        '<button class="btn btn-lg" style="background:#fff;border-color:#fff;color:#14293e" onclick="LS.go(\'dashboard\')">' + icon("grid") + t("btn.backToDash") + "</button>" +
        "</div></div>";
    },
  };
})();
