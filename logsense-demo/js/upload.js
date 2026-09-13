/* ==========================================================================
   LogSense demo — Upload & Import with simulated ingestion pipeline
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;

  const STEPS = [
    { key: "Upload", detail: "maintenance_log_2023.xlsx received (2.4 MB)" },
    { key: "Parse", detail: "12 sheets read · 1,842 rows detected" },
    { key: "Clean", detail: "52 empty/duplicate rows skipped" },
    { key: "Normalize", detail: "Shorthand & Hinglish expanded (brng → bearing…)" },
    { key: "Resolve machines", detail: "1,753 rows matched to Machine Master · 37 unresolved alias “Conv Motor-3”" },
    { key: "Extract failure modes", detail: "Bearing · Belt · VFD · Sensor · Coupling · more" },
    { key: "Validation", detail: "43 records below the auto-approval confidence threshold" },
    { key: "Index", detail: "1,790 records indexed and searchable" },
  ];

  let running = false;

  function wizardHtml() {
    return '<div class="wizard" id="wizard" aria-live="polite">' + STEPS.map((s, i) =>
      '<div class="wz-step" data-step="' + i + '">' +
      '<div class="wz-rail"><div class="wz-dot"><span class="wz-ic">' + icon("chevright", "ic-sm") + '</span></div><div class="wz-line"></div></div>' +
      '<div class="wz-body"><div class="wz-title">' + LS.t("step." + s.key) + '</div><div class="wz-detail"></div></div>' +
      "</div>").join("") + "</div>";
  }

  function resultHtml() {
    const s = LS.data.importSummary;
    const t = LS.t;
    return '<div id="import-result" style="display:grid;gap:14px">' +
      '<div class="banner banner-good">' + icon("check") + "<div><strong>" + t("up.complete") + "</strong>" +
      "maintenance_log_2023.xlsx ✓</div></div>" +
      '<div class="import-result">' +
      '<div class="ir-stat"><b>' + s.detected.toLocaleString("en-IN") + "</b><span>" + t("up.detected") + "</span></div>" +
      '<div class="ir-stat"><b>' + s.usable.toLocaleString("en-IN") + "</b><span>" + t("up.usable") + "</span></div>" +
      '<div class="ir-stat"><b>' + s.skipped + "</b><span>" + t("up.skipped") + "</span></div>" +
      '<div class="ir-stat"><b>' + s.review + "</b><span>" + t("up.review") + "</span></div>" +
      "</div>" +
      '<div class="flex"><a class="btn btn-primary" href="#/validation">' + icon("checksq") + t("btn.reviewRecords") + "</a>" +
      '<a class="btn" href="#/history">' + icon("search") + t("nav.history") + "</a></div></div>";
  }

  function runWizard() {
    if (running) return;
    running = true;
    const startBtn = document.getElementById("btn-process");
    if (startBtn) startBtn.disabled = true;
    const steps = document.querySelectorAll("#wizard .wz-step");
    let i = 0;

    function tick() {
      if (i > 0) {
        const prev = steps[i - 1];
        prev.classList.remove("active");
        prev.classList.add("done");
        prev.querySelector(".wz-dot").innerHTML = icon("check", "ic-sm");
        prev.querySelector(".wz-detail").textContent = STEPS[i - 1].detail;
      }
      if (i >= steps.length) {
        running = false;
        LS.state.set("uploadDone", true);
        const res = document.getElementById("wizard-result");
        if (res) res.innerHTML = resultHtml();
        LS.toast("<b>1,842 records</b> processed from maintenance_log_2023.xlsx — <b>43</b> queued for review.");
        return;
      }
      const cur = steps[i];
      cur.classList.add("active");
      cur.querySelector(".wz-dot").innerHTML = '<span class="spinner" aria-hidden="true"></span>';
      cur.querySelector(".wz-detail").textContent = "Processing…";
      i++;
      setTimeout(tick, 460);
    }
    tick();
  }

  LS.views.upload = {
    render(host) {
      const t = LS.t;
      const done = LS.state.get("uploadDone");
      host.innerHTML =
        '<div class="page-head"><div><h1>' + t("up.title") + "</h1>" +
        '<p class="page-sub">' + t("up.sub") + "</p></div></div>" +

        '<div class="grid grid-main-side">' +

        "<div style='display:grid;gap:14px'>" +
        '<div class="dropzone" id="dropzone" role="button" tabindex="0" aria-label="Upload files">' +
        icon("upload", "ic-lg") +
        "<h2>" + t("up.dropTitle") + "</h2>" +
        "<p>" + t("up.dropSub") + "</p>" +
        '<div class="file-types"><span class="file-type">XLSX</span><span class="file-type">CSV</span><span class="file-type">PDF</span><span class="file-type">JPG / scans</span></div>' +
        "</div>" +
        '<div class="file-row">' + icon("file") +
        '<div style="flex:1"><div class="file-name">maintenance_log_2023.xlsx</div>' +
        '<div class="file-meta">2.4 MB · 12 sheets · Demo Plant</div></div>' +
        '<button class="btn btn-primary" id="btn-process">' + icon("zap") + (done ? t("up.reRun") : t("up.process")) + "</button>" +
        "</div>" +
        '<div class="banner banner-info">' + icon("info") +
        "<div><strong>" + t("up.reads") + "</strong>" + t("up.readsBody") + "</div></div>" +
        "</div>" +

        '<div class="card"><div class="card-head"><h2>' + t("up.pipeline") + '</h2><span class="card-note">' + t("up.simulated") + "</span></div>" +
        '<div class="card-body">' + wizardHtml() + '<div id="wizard-result" class="mt-14">' + (done ? resultHtml() : "") + "</div></div></div>" +
        "</div>";

      const dz = document.getElementById("dropzone");
      ["dragover", "dragenter"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("dragover"); }));
      ["dragleave", "drop"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.remove("dragover"); }));
      dz.addEventListener("drop", runWizard);
      dz.addEventListener("click", runWizard);
      dz.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); runWizard(); } });
      document.getElementById("btn-process").addEventListener("click", runWizard);

      if (done) {
        document.querySelectorAll("#wizard .wz-step").forEach((el, i) => {
          el.classList.add("done");
          el.querySelector(".wz-dot").innerHTML = icon("check", "ic-sm");
          el.querySelector(".wz-detail").textContent = STEPS[i].detail;
        });
      }
    },
  };
})();
