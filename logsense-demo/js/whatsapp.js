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
      '<div class="wa-sys">' + LS.t("wa.simNote") + " " + D().fmtDate(D().todayIso) + "</div>" +
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
    if (wa.step === 3) chips = '<button class="wa-chip" onclick="LS.go(\'machine/CONV-L3-MTR-01\')">' + icon("box", "ic-sm") + " " + LS.t("wa.openHistory") + "</button>";
    el.innerHTML = chips;
  }

  function renderSteps() {
    const el = document.getElementById("wa-steps");
    if (!el) return;
    const t = LS.t;
    const steps = [
      [t("wa.step1"), 0],
      [t("wa.step2"), 1],
      [t("wa.step3"), 2],
      [t("wa.step4"), 3],
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
        "<div><strong>" + t("wa.savedHead") + "</strong>" + t("wa.savedBody") +
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
      botReply(LS.t("wa.understood"));
      return;
    }

    if (wa.step === 1) {
      push("out", esc(t));
      wa.step = 2;
      botReply(
        LS.t("wa.confirmMsg"),
        '<div class="wa-confirm-row">' +
        '<button class="btn btn-success btn-sm" onclick="LS.waConfirm()">' + icon("check") + LS.t("btn.confirm") + "</button>" +
        '<button class="btn btn-sm" onclick="LS.waEdit()">' + icon("edit") + LS.t("btn.edit") + "</button></div>"
      );
      return;
    }

    if (wa.step === 2) {
      push("out", esc(t));
      botReply(LS.t("wa.useConfirm"));
      return;
    }

    // step 3 — record already saved
    push("out", esc(t));
    botReply(LS.t("wa.alreadySaved"));
  }

  LS.waConfirm = function () {
    if (wa.step !== 2) return;
    wa.step = 3;
    LS.state.set("whatsappAdded", true);
    botReply(
      LS.t("wa.savedMsg"),
      '<div class="wa-confirm-row"><button class="btn btn-primary btn-sm" onclick="LS.go(\'machine/CONV-L3-MTR-01\')">' + icon("box") + LS.t("wa.viewInHistory") + "</button></div>",
      600
    );
    setTimeout(() => {
      botReply(LS.t("wa.fifthNote"), "", 200);
      LS.toast("<b>#2048</b> → Line 3 Conveyor Motor ✓", "info");
    }, 1600);
  };

  LS.waEdit = function () {
    if (wa.step !== 2) return;
    push("in", LS.t("wa.editHint"));
  };

  LS.views.whatsapp = {
    render(host) {
      const t = LS.t;
      const saved = LS.state.get("whatsappAdded");
      if (saved && wa.step !== 3) wa.step = 3;

      host.innerHTML =
        '<div class="page-head"><div><h1>' + t("wa.title") + "</h1>" +
        '<p class="page-sub">' + t("wa.sub") + "</p></div></div>" +

        '<div class="wa-layout">' +
        '<div class="phone"><div class="phone-screen">' +
        '<div class="wa-head"><div class="avatar">LS</div><div><div class="wa-head-name">LogSense Agent</div><div class="wa-head-sub">Demo Pune Plant · ' + t("wa.online") + "</div></div></div>" +
        '<div class="wa-log" id="wa-log" aria-live="polite"></div>' +
        '<div class="wa-chips" id="wa-chips"></div>' +
        '<form class="wa-footer" id="wa-form">' +
        '<input id="wa-input" type="text" placeholder="' + t("wa.typePh") + '" aria-label="WhatsApp message" autocomplete="off">' +
        '<button class="wa-send" type="submit" aria-label="Send">' + icon("send", "ic-sm") + "</button></form>" +
        "</div></div>" +

        '<div class="wa-aside">' +
        '<div class="card card-pad"><h2 style="font-size:15px;margin-bottom:10px">' + t("wa.how") + '</h2><div id="wa-steps" style="display:grid;gap:8px"></div></div>' +
        '<div class="banner banner-info">' + icon("info") +
        "<div><strong>" + t("wa.why") + "</strong>" + t("wa.whyBody") + "</div></div>" +
        '<div id="wa-banner">' +
        (wa.step === 3
          ? '<div class="banner banner-good">' + icon("check") + "<div><strong>" + t("wa.savedHead") + "</strong>" + t("wa.savedBody") +
            '<div class="banner-actions"><a class="btn btn-sm" href="#/machine/CONV-L3-MTR-01">' + icon("box") + "Line 3 Conveyor Motor</a></div></div></div>"
          : '<div class="banner banner-warn">' + icon("play") + "<div><strong>" + t("wa.tryIt") + "</strong>" + t("wa.tryBody") + "</div></div>") +
        "</div>" +
        "</div></div>";

      if (wa.log.length === 0) {
        wa.log.push({
          kind: "in",
          text: t("wa.greeting"),
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
