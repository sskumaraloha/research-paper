/* ==========================================================================
   LogSense demo — Maintenance Intelligence assistant.
   Rule-based, deterministic simulation: same question -> same answer, and
   every number is computed from the mock dataset. No external API.
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  const SUGGESTIONS = [
    "Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?",
    "Which machines have repeated bearing failures?",
    "Total downtime bearing failures Line 3 last 2 years",
    "6205 bearing kahan kahan lagi hai?",
    "Which machine has the highest downtime?",
    "bearing noise conveyor",
  ];

  const chat = { messages: [], busy: false };
  LS.assistant = chat;

  /* ---------------- Response builders (all numbers computed) ---------------- */

  function section(title, ic, body) {
    return '<div class="ai-section"><h3>' + icon(ic, "ic-sm") + esc(title) + "</h3>" + body + "</div>";
  }

  function calcNote(text) {
    return '<span class="tip calc-note" tabindex="0">' + icon("barchart") + (text || "Calculated from maintenance records") +
      '<span class="tip-body">Statistics are calculated from structured maintenance records — the AI does not invent KPI values.</span></span>';
  }

  function statTile(label, value, coverage) {
    return '<div class="stat-callout"><span class="kpi-label">' + esc(label) + '</span>' +
      '<span class="stat-main">' + value + "</span>" +
      (coverage ? '<span class="stat-coverage">' + coverage + "</span>" : "") + "</div>";
  }

  function cites(records) {
    return '<div class="cite-row">' + records.map((r) =>
      '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "Record #" + r.id + "</button>"
    ).join("") + "</div>";
  }

  function hypothesis(text) {
    return '<div class="ai-hypo"><div class="flex">' + LS.trustBadge("HYPOTHESIS") +
      '<span class="badge badge-hypo">Possible</span></div><p>' + text +
      "</p><p class='small'>This is a hypothesis and requires engineering validation — it is never promoted to a confirmed root cause automatically.</p></div>";
  }

  /* 1 — Machine history (works for any machine; richest for the L3 conveyor motor) */
  function machineHistoryAnswer(m) {
    const st = D().machineStats(m.id);
    const recs = D().recordsFor(m.id);
    const paretoRows = D().pareto(m.id);
    const isConv = m.id === "CONV-L3-MTR-01";
    const rec = isConv ? D().bearingRecurrence() : null;
    const oldest = recs[recs.length - 1];

    let html = section("Summary", "info",
      "<p><b>" + esc(m.name) + "</b> has <b>" + st.records + " maintenance records</b> between " +
      D().fmtDate(oldest.date) + " and " + D().fmtDate(recs[0].date) + " — " + st.breakdowns +
      " breakdowns plus preventive and inspection entries. " + LS.trustBadge("FACT") + "</p>");

    html += section("Key failures", "alertc",
      "<ul>" + paretoRows.map(([mode, n]) => "<li><b>" + esc(mode) + "</b> — " + n + " incident" + (n > 1 ? "s" : "") + "</li>").join("") + "</ul>");

    html += section("Statistics", "barchart",
      '<div class="ai-stats-grid">' +
      statTile("Total downtime", st.downtime.toFixed(1) + " <small>hrs</small>") +
      statTile("Breakdown events", st.breakdowns) +
      statTile("Avg repair time", st.mttr + " <small>hrs</small>") +
      "</div><div class='mt-8'>" + calcNote() + " · <span class='stat-coverage'>" + st.breakdowns + " breakdown records included · all had recorded downtime</span></div>");

    if (isConv && rec) {
      html += section("Patterns", "activity",
        "<p>A bearing-related recurrence appears approximately every <b>" + rec.avgDays + " days</b> — " +
        rec.count + " bearing replacements between " + D().fmtDate(rec.first) + " and " + D().fmtDate(rec.last) + ". " + LS.trustBadge("CALCULATED") + "</p>");
      html += hypothesis("Repeated bearing replacement despite alignment checks may indicate a mounting/alignment issue on this motor.");
      html += section("Sources", "database", cites(rec.records.slice().reverse()));
    } else {
      const breakdowns = recs.filter((r) => r.kind === "breakdown").slice(0, 4);
      if (breakdowns.length) html += section("Sources", "database", cites(breakdowns));
    }
    return html;
  }

  /* 2 — Repeated bearing failures across machines */
  function repeatedBearingAnswer() {
    const byMachine = {};
    D().bearingStats("all").records.forEach((r) => {
      (byMachine[r.machineId] = byMachine[r.machineId] || []).push(r);
    });
    const repeat = Object.entries(byMachine)
      .map(([mid, rs]) => ({ m: D().machineById(mid), rs }))
      .filter((x) => x.rs.length >= 2)
      .sort((a, b) => b.rs.length - a.rs.length);
    const single = Object.entries(byMachine).filter(([, rs]) => rs.length === 1);

    return section("Summary", "info",
        "<p><b>" + repeat.length + " machines</b> have repeated (≥2) bearing failures in the recorded history. " + LS.trustBadge("FACT") + "</p>") +
      section("Machines with repeated bearing failures", "repeat",
        "<ul>" + repeat.map((x) =>
          '<li><a href="#/machine/' + x.m.id + '"><b>' + esc(x.m.name) + "</b></a> — " + x.rs.length +
          " bearing replacements, " + D().round1(x.rs.reduce((s, r) => s + r.downtime, 0)).toFixed(1) + " hrs downtime</li>"
        ).join("") +
        (single.length ? "<li class='muted'>" + single.length + " more machine(s) had a single bearing event.</li>" : "") + "</ul>") +
      section("Statistics", "barchart", "<div class='mt-8'>" + calcNote() + "</div>") +
      section("Sources", "database", cites(repeat.flatMap((x) => x.rs.slice(0, 2))));
  }

  /* 3 — Deterministic downtime statistic */
  function bearingDowntimeAnswer(scopeLine3) {
    const st = D().bearingStats(scopeLine3 ? "line3" : "all");
    const scopeLabel = scopeLine3 ? "Line 3 · last 2 years" : "plant-wide · recorded history";
    return section("Calculated statistic", "barchart",
        '<div class="ai-stats-grid">' +
        statTile("Total bearing downtime", st.downtime.toFixed(1) + " <small>hrs</small>",
          st.count + " records included · " + st.withDowntime + "/" + st.count + " had valid downtime") +
        statTile("Bearing failure events", st.count, scopeLabel) +
        "</div><div class='mt-8'>" + calcNote() + " " + LS.trustBadge("CALCULATED") + "</div>") +
      section("Breakdown by machine", "box",
        "<ul>" + Object.entries(st.records.reduce((acc, r) => {
          const m = D().machineById(r.machineId);
          const k = m ? m.name : r.machineId;
          acc[k] = acc[k] || { n: 0, dt: 0 };
          acc[k].n += 1; acc[k].dt += r.downtime;
          return acc;
        }, {})).map(([name, v]) => "<li><b>" + esc(name) + "</b> — " + v.n + " events, " + D().round1(v.dt).toFixed(1) + " hrs</li>").join("") + "</ul>") +
      section("Sources", "database", cites(st.records));
  }

  /* 4 — Part usage (6205ZZ) */
  function partAnswer(name) {
    const p = D().partByName(name);
    if (!p) return fallbackAnswer();
    const byLine = {};
    p.records.forEach((r) => {
      const m = D().machineById(r.machineId);
      const line = m ? m.line : "?";
      byLine[line] = (byLine[line] || 0) + 1;
    });
    const topLine = Object.entries(byLine).sort((a, b) => b[1] - a[1])[0];
    const sharePct = Math.round((topLine[1] / p.count) * 100);
    return section("Summary", "info",
        "<p><b>" + esc(p.part) + "</b> appears in <b>" + p.machines.length + " machines</b> with <b>" + p.count +
        " recorded replacements</b>. Last used " + D().fmtDate(p.last) + ". " + LS.trustBadge("FACT") + "</p>") +
      section("Machines using this part", "box",
        "<ul>" + p.machines.map((mid) => {
          const m = D().machineById(mid);
          const n = p.records.filter((r) => r.machineId === mid).length;
          return '<li><a href="#/machine/' + m.id + '"><b>' + esc(m.name) + "</b></a> (" + m.line + ") — " + n + " replacements</li>";
        }).join("") + "</ul>") +
      section("Concentration", "activity",
        '<div class="ai-stats-grid">' +
        statTile(topLine[0] + " share", sharePct + "<small>%</small>", topLine[1] + " of " + p.count + " recorded uses") +
        statTile("Machines", p.machines.length) +
        "</div><div class='mt-8'>" + calcNote() + "</div>") +
      section("Sources", "database", cites(p.records.slice(0, 6)));
  }

  /* 5 — Highest downtime */
  function highestDowntimeAnswer() {
    const top = D().topMachinesByDowntime(5);
    const first = top[0];
    return section("Answer", "info",
        '<p><a href="#/machine/' + first.machine.id + '"><b>' + esc(first.machine.name) + "</b></a> has the highest recorded downtime: <b>" +
        first.downtime.toFixed(1) + " hrs</b> across " + first.breakdowns + " breakdowns. " + LS.trustBadge("CALCULATED") + "</p>") +
      section("Top 5 by downtime", "barchart",
        "<ul>" + top.map((t, i) =>
          "<li>" + (i + 1) + ". <b>" + esc(t.machine.name) + "</b> — " + t.downtime.toFixed(1) + " hrs (" + t.breakdowns + " breakdowns)</li>"
        ).join("") + "</ul><div class='mt-8'>" + calcNote() + "</div>");
  }

  /* 6 — Hybrid search demo with "why this matched" */
  function searchAnswer(q) {
    const results = D().searchRecords(q, 6);
    if (!results.length) return fallbackAnswer();
    return section("Matching records", "search",
        "<p>Found <b>" + results.length + " records</b> related to “" + esc(q) +
        "” — including shorthand and Hinglish variants (<span class='mono'>brng</span>, <span class='mono'>BRG</span>, <span class='mono'>bearing gaya</span>). " + LS.trustBadge("FACT") + "</p>" +
        '<div style="display:grid;gap:8px;margin-top:8px">' + results.map((x) => {
          const m = D().machineById(x.record.machineId);
          return '<div class="match-card">' +
            '<div class="flex spread"><b style="font-size:13px">' + esc(x.record.normalized) + '</b><span class="muted small">' + D().fmtDate(x.record.date) + "</span></div>" +
            '<div class="tl-raw" style="margin:0">' + esc(x.record.raw) + "</div>" +
            '<div class="flex spread"><span class="small">' + (m ? '<a href="#/machine/' + m.id + '">' + esc(m.name) + "</a>" : "") + "</span>" +
            '<button class="cite-chip" onclick="LS.openRecord(' + x.record.id + ')">' + icon("file") + "#" + x.record.id + "</button></div>" +
            (x.why.length ? '<div class="match-why"><span class="muted small">Why this matched:</span>' +
              x.why.map((w) => '<span class="why-chip">' + w + "</span>").join("") + "</div>" : "") +
            "</div>";
        }).join("") + "</div>");
  }

  /* 7 — Similar failures */
  function similarFailuresAnswer() {
    const results = D().searchRecords("bearing noise motor", 5);
    return section("Similar failures across the plant", "repeat",
      "<p>Records most similar to the latest failure on <b>Line 3 Conveyor Motor</b> (bearing noise):</p>" +
      '<div style="display:grid;gap:8px;margin-top:8px">' + results.map((x) => {
        const m = D().machineById(x.record.machineId);
        return '<div class="match-card">' +
          '<div class="flex spread"><b style="font-size:13px">' + esc(m ? m.name : "") + "</b><span class='muted small'>" + D().fmtDate(x.record.date) + "</span></div>" +
          '<div class="tl-raw" style="margin:0">' + esc(x.record.raw) + "</div>" +
          '<div class="flex spread">' +
          (x.why.length ? '<div class="match-why">' + x.why.map((w) => '<span class="why-chip">' + w + "</span>").join("") + "</div>" : "<span></span>") +
          '<button class="cite-chip" onclick="LS.openRecord(' + x.record.id + ')">' + icon("file") + "#" + x.record.id + "</button></div></div>";
      }).join("") + "</div>");
  }

  function plantKpiAnswer() {
    const k = D().plant.kpis;
    return section("Plant KPIs — " + D().plant.period, "barchart",
      '<div class="ai-stats-grid">' +
      statTile("MTTR", k.mttr.value + " <small>hrs</small>", "↓ " + Math.abs(k.mttr.delta) + "% vs previous period") +
      statTile("MTBF", k.mtbf.value + " <small>hrs</small>", "↑ " + k.mtbf.delta + "%") +
      statTile("Downtime", k.downtime.value + " <small>hrs</small>", "↓ " + Math.abs(k.downtime.delta) + "%") +
      "</div><div class='mt-8'>" + calcNote("Demo Plant dataset") + "</div>");
  }

  function fallbackAnswer() {
    return section("I can help with your maintenance history", "info",
      "<p>In this demo I can answer questions about <b>machines, failures, downtime, spare parts and maintenance history</b>. Try one of these:</p>" +
      '<ul style="margin-top:6px">' + SUGGESTIONS.slice(0, 4).map((s) => "<li>“" + esc(s) + "”</li>").join("") + "</ul>");
  }

  /* ---------------- Intent routing (deterministic) ---------------- */

  function answer(qRaw) {
    const q = D().normalizeQuery(qRaw);

    if (/6205/.test(q)) return partAnswer("6205ZZ");
    if (/v-?belt|a42/.test(q)) return partAnswer("V-Belt");
    if (/proximity|sensor.*(kahan|where|which machine)/.test(q)) return partAnswer("Proximity Sensor");

    if (/downtime/.test(q) && /bearing/.test(q)) {
      return bearingDowntimeAnswer(/line ?3|l3/.test(q));
    }
    if (/repeated|repeat|which machines/.test(q) && /bearing/.test(q)) return repeatedBearingAnswer();
    if (/highest downtime|sabse (zyada|jyada) downtime|worst machine/.test(q)) return highestDowntimeAnswer();
    if (/similar/.test(q)) return similarFailuresAnswer();
    if (/mttr|mtbf|kpi/.test(q)) return plantKpiAnswer();

    // Machine-specific history (explicit history intent, or a machine named in full)
    if (/kya kya hua|what happened|history|pichle/.test(q)) {
      if (/conveyor/.test(q) && /line ?3|l3/.test(q)) return machineHistoryAnswer(D().machineById("CONV-L3-MTR-01"));
      const m = D().machines.find((mm) => q.includes(mm.name.toLowerCase()));
      if (m) return machineHistoryAnswer(m);
      if (/conveyor/.test(q)) return machineHistoryAnswer(D().machineById("CONV-L3-MTR-01"));
    }
    const named = D().machines.find((mm) => q.includes(mm.name.toLowerCase()));
    if (named) return machineHistoryAnswer(named);

    if (/bearing|belt|vfd|coupling|leak|overheat|sensor|trip|noise/.test(q)) return searchAnswer(qRaw);

    return fallbackAnswer();
  }

  /* ---------------- Rendering ---------------- */

  function bubbleUser(text) {
    return '<div class="msg msg-user"><div class="msg-avatar">' + D().plant.user.initials + '</div><div class="msg-bubble">' + esc(text) + "</div></div>";
  }
  function bubbleAI(html) {
    return '<div class="msg msg-ai"><div class="msg-avatar">' + icon("layers", "ic-sm") + '</div><div class="msg-bubble">' + html + "</div></div>";
  }

  function renderLog() {
    const log = document.getElementById("chat-log");
    if (!log) return;
    log.innerHTML =
      '<div class="chat-hero">' +
      '<div class="brand-mark">' + icon("chat") + "</div>" +
      "<h1>Maintenance Intelligence</h1>" +
      "<p>Ask questions about your plant’s maintenance history — in English, Hindi or Hinglish. Every answer links back to the source records.</p>" +
      (chat.messages.length === 0
        ? '<div class="chat-suggestions" style="justify-content:center;margin-top:6px">' +
          SUGGESTIONS.map((s) => '<button class="sugg-chip" data-q="' + esc(s) + '">' + esc(s) + "</button>").join("") + "</div>"
        : "") +
      "</div>" +
      chat.messages.map((m) => (m.role === "user" ? bubbleUser(m.text) : bubbleAI(m.html))).join("") +
      (chat.busy ? bubbleAI('<div class="typing" aria-label="Assistant is typing"><i></i><i></i><i></i></div>') : "") +
      (chat.messages.length > 0 && !chat.busy
        ? '<div class="chat-suggestions">' + SUGGESTIONS.filter((s) => !chat.messages.some((m) => m.text === s))
            .slice(0, 3).map((s) => '<button class="sugg-chip" data-q="' + esc(s) + '">' + esc(s) + "</button>").join("") + "</div>"
        : "");
    window.scrollTo(0, document.body.scrollHeight);
  }

  function submit(q) {
    if (!q || chat.busy) return;
    chat.messages.push({ role: "user", text: q });
    chat.busy = true;
    renderLog();
    setTimeout(() => {
      chat.busy = false;
      chat.messages.push({ role: "ai", html: answer(q) });
      if (document.getElementById("chat-log")) renderLog();
    }, 650);
  }

  LS.askAssistant = function (q) {
    LS.go("assistant");
    setTimeout(() => submit(q), 80);
  };

  LS.views.assistant = {
    render(host) {
      host.innerHTML =
        '<div class="chat-layout">' +
        '<div></div>' +
        '<div class="chat-log" id="chat-log" aria-live="polite"></div>' +
        '<form class="chat-input-row" id="chat-form">' +
        '<input id="chat-input" type="text" placeholder="Ask about machines, failures, downtime, spare parts…" aria-label="Ask the maintenance assistant" autocomplete="off">' +
        '<button class="btn btn-primary" type="submit" aria-label="Send">' + icon("send") + "Ask</button>" +
        "</form></div>";

      renderLog();

      document.getElementById("chat-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const input = document.getElementById("chat-input");
        const q = input.value.trim();
        if (!q) return;
        input.value = "";
        submit(q);
      });
      host.addEventListener("click", (e) => {
        const chip = e.target.closest(".sugg-chip");
        if (chip) submit(chip.dataset.q);
      });
    },
  };
})();
