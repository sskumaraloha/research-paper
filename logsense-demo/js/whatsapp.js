/* ==========================================================================
   LogSense demo — WhatsApp Entry Agent simulation.
   A scripted, phone-style conversation. No real WhatsApp connection —
   the flow mirrors the product's conversational capture behaviour.
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  const TECH_MSG = "Line 3 ka conveyor motor band tha, bearing change kiya, alignment check kiya, ab chal raha hai.";
  const DOWNTIME_MSG = "2 ghante";

  /* step: 0 = waiting for report, 1 = waiting for downtime,
     2 = waiting for confirm, 3 = saved */
  const wa = { step: 0, log: [] };
  LS.wa = wa;

  function now() {
    const d = new Date();
    return String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0");
  }

  function push(kind, text, extraHtml) {
    wa.log.push({ kind, text, extraHtml: extraHtml || "", time: now() });
    renderLog();
  }

  function renderLog() {
    const el = document.getElementById("wa-log");
    if (!el) return;
    el.innerHTML =
      '<div class="wa-sys">Messages are simulated — no real WhatsApp connection. Today: ' + D().fmtDate(D().todayIso) + "</div>" +
      wa.log.map((m) => {
        if (m.kind === "sys") return '<div class="wa-msg wa-sys">' + m.text + "</div>";
        const cls = m.kind === "out" ? "wa-out" : "wa-in";
        return '<div class="wa-msg ' + cls + '">' + m.text + m.extraHtml + '<span class="wa-time">' + m.time + (m.kind === "out" ? " ✓✓" : "") + "</span></div>";
      }).join("");
    el.scrollTop = el.scrollHeight;
    renderChips();
    renderSteps();
  }

  function renderChips() {
    const el = document.getElementById("wa-chips");
    if (!el) return;
    let chips = "";
    if (wa.step === 0) chips = '<button class="wa-chip" data-send="' + esc(TECH_MSG) + '">' + icon("mic", "ic-sm") + " " + esc(TECH_MSG.slice(0, 46)) + "…</button>";
    if (wa.step === 1) chips = '<button class="wa-chip" data-send="' + DOWNTIME_MSG + '">' + DOWNTIME_MSG + "</button>";
    if (wa.step === 3) chips = '<button class="wa-chip" onclick="LS.go(\'machine/CONV-L3-MTR-01\')">' + icon("box", "ic-sm") + " Open machine history</button>";
    el.innerHTML = chips;
  }

  function renderSteps() {
    const el = document.getElementById("wa-steps");
    if (!el) return;
    const steps = [
      ["Technician reports the breakdown in plain Hinglish", 0],
      ["Agent extracts machine, issue, action — and asks for what is missing", 1],
      ["Technician answers; agent asks for one confirmation", 2],
      ["Record saved into machine history with full traceability", 3],
    ];
    const saved = wa.step === 3;
    el.innerHTML = steps.map(([t, n]) => {
      const done = wa.step > n || (saved && n === 3);
      return '<div class="wa-step ' + (done ? "done" : wa.step === n ? "now" : "") + '">' +
        '<span class="step-n">' + (done ? icon("check", "ic-sm") : n + 1) + "</span><span>" + t + "</span></div>";
    }).join("");
    const banner = document.getElementById("wa-banner");
    if (banner && saved) {
      banner.innerHTML = '<div class="banner banner-good">' + icon("check") +
        "<div><strong>Record #2048 saved</strong>Open the machine to see the new timeline entry and the recurrence alert." +
        '<div class="banner-actions"><a class="btn btn-sm" href="#/machine/CONV-L3-MTR-01">' + icon("box") + "Line 3 Conveyor Motor</a></div></div></div>";
    }
  }

  function botReply(html, extraHtml, delay) {
    setTimeout(() => push("in", html, extraHtml), delay || 700);
  }

  function handleSend(text) {
    const t = (text || "").trim();
    if (!t) return;

    if (wa.step === 0) {
      push("out", esc(t));
      wa.step = 1;
      botReply(
        "I understood:\n\n" +
        "<b>Machine:</b> Line 3 Conveyor Motor\n" +
        "<b>Issue:</b> Bearing failure\n" +
        "<b>Action:</b> Bearing replaced\n" +
        "<b>Alignment:</b> Checked\n" +
        "<b>Status:</b> Running\n" +
        "<b>Downtime:</b> Not provided\n\n" +
        "How long was the machine down?"
      );
      return;
    }

    if (wa.step === 1) {
      push("out", esc(t));
      wa.step = 2;
      botReply(
        "Please confirm:\n\n" +
        "<b>Line 3 Conveyor Motor</b>\n" +
        "Bearing failure\n" +
        "Bearing replaced\n" +
        "Alignment checked\n" +
        "Downtime: <b>2 hours</b>",
        '<div class="wa-confirm-row">' +
        '<button class="btn btn-success btn-sm" onclick="LS.waConfirm()">' + icon("check") + "Confirm</button>" +
        '<button class="btn btn-sm" onclick="LS.waEdit()">' + icon("edit") + "Edit</button></div>"
      );
      return;
    }

    if (wa.step === 2) {
      push("out", esc(t));
      botReply("Please use the <b>Confirm</b> button above, or type what should change.");
      return;
    }

    // step 3 — record already saved
    push("out", esc(t));
    botReply("Record #2048 is already saved. Send a new breakdown report anytime — I'm listening. 👍");
  }

  LS.waConfirm = function () {
    if (wa.step !== 2) return;
    wa.step = 3;
    LS.state.set("whatsappAdded", true);
    botReply(
      "✅ Maintenance record <b>#2048</b> created successfully.\n\n" +
      "Line 3 Conveyor Motor · Bearing replaced · 2.0 hrs downtime\n" +
      "Source: WhatsApp · Technician: Ramesh",
      '<div class="wa-confirm-row"><button class="btn btn-primary btn-sm" onclick="LS.go(\'machine/CONV-L3-MTR-01\')">' + icon("box") + "View in machine history</button></div>",
      600
    );
    setTimeout(() => {
      botReply(
        "⚠️ Note for the maintenance team: this is the <b>5th bearing replacement</b> on this motor since Nov 2025. " +
        "LogSense has flagged the recurrence for <b>engineering review</b>.", "", 200
      );
      LS.toast("<b>New record #2048 added</b> to Line 3 Conveyor Motor. Recurrence insight updated.", "info");
    }, 1600);
  };

  LS.waEdit = function () {
    if (wa.step !== 2) return;
    push("in", "No problem — reply with the field to change, e.g. <b>“downtime 3 ghante”</b>. (In this demo, press Confirm to continue the story.)");
  };

  LS.views.whatsapp = {
    render(host) {
      const saved = LS.state.get("whatsappAdded");
      if (saved && wa.step !== 3) wa.step = 3;

      host.innerHTML =
        '<div class="page-head"><div><h1>WhatsApp Entry Agent</h1>' +
        '<p class="page-sub">Technicians report breakdowns the way they already talk — the agent structures it, asks for what’s missing, and saves a traceable record. Simulated conversation, no real WhatsApp API.</p></div></div>' +

        '<div class="wa-layout">' +
        '<div class="phone"><div class="phone-screen">' +
        '<div class="wa-head"><div class="avatar">LS</div><div><div class="wa-head-name">LogSense Agent</div><div class="wa-head-sub">Demo Pune Plant · online</div></div></div>' +
        '<div class="wa-log" id="wa-log" aria-live="polite"></div>' +
        '<div class="wa-chips" id="wa-chips"></div>' +
        '<form class="wa-footer" id="wa-form">' +
        '<input id="wa-input" type="text" placeholder="Type a message" aria-label="WhatsApp message" autocomplete="off">' +
        '<button class="wa-send" type="submit" aria-label="Send">' + icon("send", "ic-sm") + "</button></form>" +
        "</div></div>" +

        '<div class="wa-aside">' +
        '<div class="card card-pad"><h2 style="font-size:15px;margin-bottom:10px">How this flow works</h2><div id="wa-steps" style="display:grid;gap:8px"></div></div>' +
        '<div class="banner banner-info">' + icon("info") +
        "<div><strong>Why it matters</strong>No new forms, no app to learn. Technician knowledge — “Yadav sir bolte hai shaft me runout hai” — stops living only in people’s heads.</div></div>" +
        '<div id="wa-banner">' +
        (wa.step === 3
          ? '<div class="banner banner-good">' + icon("check") + "<div><strong>Record #2048 saved</strong>Open the machine to see the new timeline entry and the recurrence alert." +
            '<div class="banner-actions"><a class="btn btn-sm" href="#/machine/CONV-L3-MTR-01">' + icon("box") + "Line 3 Conveyor Motor</a></div></div></div>"
          : '<div class="banner banner-warn">' + icon("play") + "<div><strong>Try it</strong>Tap the suggested message under the chat (or type your own) to start the conversation.</div></div>") +
        "</div>" +
        "</div></div>";

      if (wa.log.length === 0) {
        wa.log.push({
          kind: "in",
          text: "Namaste Ramesh 🙏 Report a breakdown in Hindi or English — machine, kya hua, kya kiya.",
          extraHtml: "",
          time: now(),
        });
      }
      renderLog();

      document.getElementById("wa-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const input = document.getElementById("wa-input");
        handleSend(input.value);
        input.value = "";
      });
      document.getElementById("wa-chips").addEventListener("click", (e) => {
        const chip = e.target.closest("[data-send]");
        if (chip) handleSend(chip.dataset.send);
      });
    },
  };
})();
