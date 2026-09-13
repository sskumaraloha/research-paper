/* ==========================================================================
   LogSense demo — Maintenance Intelligence assistant.
   Rule-based, deterministic simulation: same question -> same answer, and
   every number is computed from the mock dataset. No external API.
   Understands English, Hinglish, Hindi (Devanagari) and Marathi questions;
   answers render in the currently selected UI language.
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;
  const t = (k) => LS.t(k);
  const tf = (k, v) => LS.tf(k, v);

  const chat = { messages: [], busy: false };
  LS.assistant = chat;

  const suggestions = () => LS.i18n.suggestions();

  /* ---------------- Response builders (all numbers computed) ---------------- */

  function section(title, ic, body) {
    return '<div class="ai-section"><h3>' + icon(ic, "ic-sm") + title + "</h3>" + body + "</div>";
  }

  function calcNote(text) {
    return '<span class="tip calc-note" tabindex="0">' + icon("barchart") + (text || t("ai.calcNote")) +
      '<span class="tip-body">' + t("ai.calcTip") + "</span></span>";
  }

  function statTile(label, value, coverage) {
    return '<div class="stat-callout"><span class="kpi-label">' + label + "</span>" +
      '<span class="stat-main">' + value + "</span>" +
      (coverage ? '<span class="stat-coverage">' + coverage + "</span>" : "") + "</div>";
  }

  function cites(records) {
    return '<div class="cite-row">' + records.map((r) =>
      '<button class="cite-chip" onclick="LS.openRecord(' + r.id + ')">' + icon("file") + "#" + r.id + "</button>"
    ).join("") + "</div>";
  }

  function hypothesis(text) {
    return '<div class="ai-hypo"><div class="flex">' + LS.trustBadge("HYPOTHESIS") +
      '<span class="badge badge-hypo">' + t("ai.possible") + "</span></div><p>" + text +
      "</p><p class='small'>" + t("ai.hypoDisclaimer") + "</p></div>";
  }

  const whyLabel = (w) => ({
    "Same failure mode": t("why.sameMode"),
    "Same machine type": t("why.sameType"),
    "Shared part": t("why.sharedPart"),
    "Semantic similarity (shorthand)": t("why.semantic"),
  }[w] || w);

  const hrsSmall = (v) => v + " <small>" + t("unit.hrs") + "</small>";

  /* 1 — Machine history (works for any machine; richest for the L3 conveyor motor) */
  function machineHistoryAnswer(m) {
    const st = D().machineStats(m.id);
    const recs = D().recordsFor(m.id);
    const paretoRows = D().pareto(m.id);
    const isConv = m.id === "CONV-L3-MTR-01";
    const rec = isConv ? D().bearingRecurrence() : null;
    const oldest = recs[recs.length - 1];

    let html = section(t("ai.summary"), "info",
      "<p>" + tf("ai.summaryMachine", {
        name: esc(m.name), n: st.records, from: D().fmtDate(oldest.date), to: D().fmtDate(recs[0].date), b: st.breakdowns,
      }) + " " + LS.trustBadge("FACT") + "</p>");

    html += section(t("ai.keyFailures"), "alertc",
      "<ul>" + paretoRows.map(([mode, n]) => "<li><b>" + LS.tMode(mode) + "</b> — " + tf("ai.incidents", { n }) + "</li>").join("") + "</ul>");

    html += section(t("ai.stats"), "barchart",
      '<div class="ai-stats-grid">' +
      statTile(t("ai.totalDt"), hrsSmall(st.downtime.toFixed(1))) +
      statTile(t("ai.breakdownEvents"), st.breakdowns) +
      statTile(t("ai.avgRepair"), hrsSmall(st.mttr)) +
      "</div><div class='mt-8'>" + calcNote() + " · <span class='stat-coverage'>" + tf("ai.breakdownsIncluded", { n: st.breakdowns }) + "</span></div>");

    if (isConv && rec) {
      html += section(t("ai.patterns"), "activity",
        "<p>" + tf("ai.pattern92", { d: rec.avgDays, n: rec.count, from: D().fmtDate(rec.first), to: D().fmtDate(rec.last) }) +
        " " + LS.trustBadge("CALCULATED") + "</p>");
      html += hypothesis(t("ai.hypoBearing"));
      html += section(t("ai.sources"), "database", cites(rec.records.slice().reverse()));
    } else {
      const breakdowns = recs.filter((r) => r.kind === "breakdown").slice(0, 4);
      if (breakdowns.length) html += section(t("ai.sources"), "database", cites(breakdowns));
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

    return section(t("ai.summary"), "info",
        "<p>" + tf("ai.repeatSummary", { n: repeat.length }) + " " + LS.trustBadge("FACT") + "</p>") +
      section(t("ai.repeatHeader"), "repeat",
        "<ul>" + repeat.map((x) =>
          '<li><a href="#/machine/' + x.m.id + '"><b>' + esc(x.m.name) + "</b></a> — " +
          tf("ai.repeatItem", { n: x.rs.length, h: D().round1(x.rs.reduce((s, r) => s + r.downtime, 0)).toFixed(1) }) + "</li>"
        ).join("") +
        (single.length ? "<li class='muted'>" + tf("ai.moreSingle", { n: single.length }) + "</li>" : "") + "</ul>") +
      section(t("ai.stats"), "barchart", "<div class='mt-8'>" + calcNote() + "</div>") +
      section(t("ai.sources"), "database", cites(repeat.flatMap((x) => x.rs.slice(0, 2))));
  }

  /* 3 — Deterministic downtime statistic */
  function bearingDowntimeAnswer(scopeLine3) {
    const st = D().bearingStats(scopeLine3 ? "line3" : "all");
    const scopeLabel = scopeLine3 ? t("ai.scopeL3") : t("ai.scopePlant");
    return section(t("ai.calcStat"), "barchart",
        '<div class="ai-stats-grid">' +
        statTile(t("ai.bearingDt"), hrsSmall(st.downtime.toFixed(1)),
          tf("ai.recordsIncluded", { a: st.count, b: st.withDowntime })) +
        statTile(t("ai.bearingEvents"), st.count, scopeLabel) +
        "</div><div class='mt-8'>" + calcNote() + " " + LS.trustBadge("CALCULATED") + "</div>") +
      section(t("ai.byMachine"), "box",
        "<ul>" + Object.entries(st.records.reduce((acc, r) => {
          const m = D().machineById(r.machineId);
          const k = m ? m.name : r.machineId;
          acc[k] = acc[k] || { n: 0, dt: 0 };
          acc[k].n += 1; acc[k].dt += r.downtime;
          return acc;
        }, {})).map(([name, v]) => "<li><b>" + esc(name) + "</b> — " + tf("ai.eventsHrs", { n: v.n, h: D().round1(v.dt).toFixed(1) }) + "</li>").join("") + "</ul>") +
      section(t("ai.sources"), "database", cites(st.records));
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
    return section(t("ai.summary"), "info",
        "<p>" + tf("ai.partSummary", { p: esc(p.part), m: p.machines.length, n: p.count, d: D().fmtDate(p.last) }) +
        " " + LS.trustBadge("FACT") + "</p>") +
      section(t("ai.machinesUsing"), "box",
        "<ul>" + p.machines.map((mid) => {
          const m = D().machineById(mid);
          const n = p.records.filter((r) => r.machineId === mid).length;
          return '<li><a href="#/machine/' + m.id + '"><b>' + esc(m.name) + "</b></a> (" + m.line + ") — " + tf("ai.replacementsN", { n }) + "</li>";
        }).join("") + "</ul>") +
      section(t("ai.concentration"), "activity",
        '<div class="ai-stats-grid">' +
        statTile(tf("ai.share", { line: topLine[0] }), sharePct + "<small>%</small>", tf("ai.ofUses", { a: topLine[1], b: p.count })) +
        statTile(t("ai.machinesLbl"), p.machines.length) +
        "</div><div class='mt-8'>" + calcNote() + "</div>") +
      section(t("ai.sources"), "database", cites(p.records.slice(0, 6)));
  }

  /* 5 — Highest downtime */
  function highestDowntimeAnswer() {
    const top = D().topMachinesByDowntime(5);
    const first = top[0];
    return section(t("ai.answer"), "info",
        "<p>" + tf("ai.highest", {
          name: '<a href="#/machine/' + first.machine.id + '">' + esc(first.machine.name) + "</a>",
          h: first.downtime.toFixed(1), n: first.breakdowns,
        }) + " " + LS.trustBadge("CALCULATED") + "</p>") +
      section(t("ai.top5"), "barchart",
        "<ul>" + top.map((tm, i) =>
          "<li>" + (i + 1) + ". <b>" + esc(tm.machine.name) + "</b> — " + tf("ai.hrsBrk", { h: tm.downtime.toFixed(1), n: tm.breakdowns }) + "</li>"
        ).join("") + "</ul><div class='mt-8'>" + calcNote() + "</div>");
  }

  /* 6 — Hybrid search demo with "why this matched" */
  function searchAnswer(q) {
    const results = D().searchRecords(q, 6);
    if (!results.length) return fallbackAnswer();
    return section(t("ai.matching"), "search",
        "<p>" + tf("ai.searchSummary", { n: results.length, q: esc(q) }) + " " + LS.trustBadge("FACT") + "</p>" +
        '<div style="display:grid;gap:8px;margin-top:8px">' + results.map((x) => {
          const m = D().machineById(x.record.machineId);
          return '<div class="match-card">' +
            '<div class="flex spread"><b style="font-size:13px">' + esc(x.record.normalized) + '</b><span class="muted small">' + D().fmtDate(x.record.date) + "</span></div>" +
            '<div class="tl-raw" style="margin:0">' + esc(x.record.raw) + "</div>" +
            '<div class="flex spread"><span class="small">' + (m ? '<a href="#/machine/' + m.id + '">' + esc(m.name) + "</a>" : "") + "</span>" +
            '<button class="cite-chip" onclick="LS.openRecord(' + x.record.id + ')">' + icon("file") + "#" + x.record.id + "</button></div>" +
            (x.why.length ? '<div class="match-why"><span class="muted small">' + t("ai.whyMatched") + "</span>" +
              x.why.map((w) => '<span class="why-chip">' + whyLabel(w) + "</span>").join("") + "</div>" : "") +
            "</div>";
        }).join("") + "</div>");
  }

  /* 7 — Similar failures */
  function similarFailuresAnswer() {
    const results = D().searchRecords("bearing noise motor", 5);
    return section(t("ai.similar"), "repeat",
      "<p>" + t("ai.similarIntro") + "</p>" +
      '<div style="display:grid;gap:8px;margin-top:8px">' + results.map((x) => {
        const m = D().machineById(x.record.machineId);
        return '<div class="match-card">' +
          '<div class="flex spread"><b style="font-size:13px">' + esc(m ? m.name : "") + "</b><span class='muted small'>" + D().fmtDate(x.record.date) + "</span></div>" +
          '<div class="tl-raw" style="margin:0">' + esc(x.record.raw) + "</div>" +
          '<div class="flex spread">' +
          (x.why.length ? '<div class="match-why">' + x.why.map((w) => '<span class="why-chip">' + whyLabel(w) + "</span>").join("") + "</div>" : "<span></span>") +
          '<button class="cite-chip" onclick="LS.openRecord(' + x.record.id + ')">' + icon("file") + "#" + x.record.id + "</button></div></div>";
      }).join("") + "</div>");
  }

  function plantKpiAnswer() {
    const k = D().plant.kpis;
    return section(t("ai.stats") + " — " + t("dash.period"), "barchart",
      '<div class="ai-stats-grid">' +
      statTile("MTTR", hrsSmall(k.mttr.value), "↓ " + Math.abs(k.mttr.delta) + "% " + t("dash.vsPrev")) +
      statTile("MTBF", hrsSmall(k.mtbf.value), "↑ " + k.mtbf.delta + "%") +
      statTile(t("lbl.downtime"), hrsSmall(k.downtime.value), "↓ " + Math.abs(k.downtime.delta) + "%") +
      "</div><div class='mt-8'>" + calcNote() + "</div>");
  }

  function fallbackAnswer() {
    return section(t("ai.fallbackTitle"), "info",
      "<p>" + t("ai.fallbackBody") + "</p>" +
      '<ul style="margin-top:6px">' + suggestions().slice(0, 4).map((s) => "<li>“" + esc(s) + "”</li>").join("") + "</ul>");
  }

  /* ---------------- Intent routing (deterministic, multilingual) ---------------- */

  const RX = {
    bearing: /bearing|बेयरिंग|बेअरिंग/,
    downtime: /downtime|डाउनटाइम/,
    repeated: /repeated|repeat|which machines|बार-बार|बार बार|किन मशीनों|वारंवार|कोणत्या मशीन/,
    highest: /highest downtime|worst machine|सबसे (ज़्यादा|ज्यादा|जादा) डाउनटाइम|सर्वात जास्त डाउनटाइम|सर्वाधिक डाउनटाइम/,
    history: /kya kya hua|क्या-क्या हुआ|क्या क्या हुआ|काय काय झाले|what happened|history|pichle|पिछले|गेल्या/,
    conveyor: /conveyor|कन्वेयर|कन्व्हेयर/,
    line3: /line ?3|l3/,
    similar: /similar|मिलती-जुलती|सारखे बिघाड/,
    searchable: /bearing|belt|vfd|coupling|leak|overheat|sensor|trip|noise|बेयरिंग|बेअरिंग|सेंसर|सेन्सर|बेल्ट|कपलिंग|गळती|लीकेज/,
  };

  function answer(qRaw) {
    const q = D().normalizeQuery(qRaw);

    if (/6205/.test(q)) return partAnswer("6205ZZ");
    if (/v-?belt|a42/.test(q)) return partAnswer("V-Belt");
    if (/proximity|प्रॉक्सिमिटी/.test(q)) return partAnswer("Proximity Sensor");

    if (RX.downtime.test(q) && RX.bearing.test(q)) {
      return bearingDowntimeAnswer(RX.line3.test(q));
    }
    if (RX.repeated.test(q) && RX.bearing.test(q)) return repeatedBearingAnswer();
    if (RX.highest.test(q)) return highestDowntimeAnswer();
    if (RX.similar.test(q)) return similarFailuresAnswer();
    if (/mttr|mtbf|kpi/.test(q)) return plantKpiAnswer();

    // Machine-specific history (explicit history intent, or a machine named in full)
    if (RX.history.test(q)) {
      if (RX.conveyor.test(q) && RX.line3.test(q)) return machineHistoryAnswer(D().machineById("CONV-L3-MTR-01"));
      const m = D().machines.find((mm) => q.includes(mm.name.toLowerCase()));
      if (m) return machineHistoryAnswer(m);
      if (RX.conveyor.test(q)) return machineHistoryAnswer(D().machineById("CONV-L3-MTR-01"));
    }
    const named = D().machines.find((mm) => q.includes(mm.name.toLowerCase()));
    if (named) return machineHistoryAnswer(named);

    if (RX.searchable.test(q)) return searchAnswer(qRaw);

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
      "<h1>" + t("ai.hero") + "</h1>" +
      "<p>" + t("ai.heroSub") + "</p>" +
      (chat.messages.length === 0
        ? '<div class="chat-suggestions" style="justify-content:center;margin-top:6px">' +
          suggestions().map((s) => '<button class="sugg-chip" data-q="' + esc(s) + '">' + esc(s) + "</button>").join("") + "</div>"
        : "") +
      "</div>" +
      chat.messages.map((m) => (m.role === "user" ? bubbleUser(m.text) : bubbleAI(m.html))).join("") +
      (chat.busy ? bubbleAI('<div class="typing" aria-label="Assistant is typing"><i></i><i></i><i></i></div>') : "") +
      (chat.messages.length > 0 && !chat.busy
        ? '<div class="chat-suggestions">' + suggestions().filter((s) => !chat.messages.some((m) => m.text === s))
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
        "<div></div>" +
        '<div class="chat-log" id="chat-log" aria-live="polite"></div>' +
        '<form class="chat-input-row" id="chat-form">' +
        '<input id="chat-input" type="text" placeholder="' + t("ai.inputPh") + '" aria-label="Ask the maintenance assistant" autocomplete="off">' +
        '<button class="btn btn-primary" type="submit" aria-label="Send">' + icon("send") + t("ai.ask") + "</button>" +
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
