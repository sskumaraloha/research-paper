/* ==========================================================================
   LogSense demo — AI Validation Queue (split-screen review + alias mapping)
   ========================================================================== */

(function () {
  "use strict";
  const icon = LS.icon;
  const esc = LS.esc;
  const D = () => LS.data;

  function itemState(id) {
    if ((LS.state.get("approved") || []).includes(id)) return "approved";
    if ((LS.state.get("rejected") || []).includes(id)) return "rejected";
    return "pending";
  }

  function setItemState(id, state) {
    const approved = (LS.state.get("approved") || []).filter((x) => x !== id);
    const rejected = (LS.state.get("rejected") || []).filter((x) => x !== id);
    if (state === "approved") approved.push(id);
    if (state === "rejected") rejected.push(id);
    LS.state.set("approved", approved);
    LS.state.set("rejected", rejected);
  }

  LS.approveVq = function (id) {
    setItemState(id, "approved");
    LS.toast("Record approved and indexed. <b>" + LS.pendingValidationCount() + "</b> remaining in queue.");
    rerenderItem(id);
  };
  LS.rejectVq = function (id) {
    setItemState(id, "rejected");
    LS.toast("Record rejected — excluded from search and statistics.", "warn");
    rerenderItem(id);
  };
  LS.editVq = function (id) {
    const item = D().validationItems.find((v) => v.id === id);
    LS.openModal(
      '<div class="modal-head"><h2>Edit extracted record</h2><button class="icon-btn" onclick="LS.closeModal()" aria-label="Close">' + icon("x") + "</button></div>" +
      '<div class="modal-body" style="display:grid;gap:12px">' +
      Object.entries(item.fields).map(([k, v]) =>
        '<div class="field" style="margin:0"><label>' + esc(k) + '</label><input value="' + esc(v) + '"></div>').join("") +
      '<div class="flex"><button class="btn btn-primary" onclick="LS.closeModal();LS.approveVq(\'' + id + '\')">' + icon("check") + "Save &amp; approve</button>" +
      '<button class="btn" onclick="LS.closeModal()">Cancel</button></div>' +
      '<p class="small muted">Demo note: edits are illustrative and are not persisted.</p></div>'
    );
  };

  function itemHtml(item) {
    const state = itemState(item.id);
    const confCls = item.conf < 75 ? "conf low" : "conf";
    return '<div class="card" id="card-' + item.id + '">' +
      '<div class="vq-item">' +
      '<div class="vq-left">' +
      '<div class="raw-label">' + icon("file", "ic-sm") + "Original record</div>" +
      '<div class="raw-block">' + esc(item.raw) + "</div>" +
      '<span class="src-tag">' + icon("file") + esc(item.src) + "</span>" +
      "</div>" +
      '<div class="vq-right">' +
      '<div class="flex spread"><div class="raw-label" style="margin:0">' + icon("zap", "ic-sm") + "AI-extracted record</div>" +
      '<span class="' + confCls + '"><span class="conf-bar"><i style="width:' + item.conf + '%"></i></span>' + item.conf + "%</span></div>" +
      '<dl class="kv">' + Object.entries(item.fields).map(([k, v]) => "<dt>" + esc(k) + "</dt><dd>" + esc(v) + "</dd>").join("") + "</dl>" +
      '<p class="small muted">' + esc(item.note) + "</p>" +
      (state === "pending"
        ? '<div class="vq-actions">' +
          '<button class="btn btn-success btn-sm" onclick="LS.approveVq(\'' + item.id + '\')">' + icon("check") + "Approve</button>" +
          '<button class="btn btn-sm" onclick="LS.editVq(\'' + item.id + '\')">' + icon("edit") + "Edit</button>" +
          '<button class="btn btn-danger-ghost btn-sm" onclick="LS.rejectVq(\'' + item.id + '\')">' + icon("x") + "Reject</button></div>"
        : state === "approved"
          ? '<div class="vq-done approved">' + icon("check") + "Approved — indexed into machine history</div>"
          : '<div class="vq-done rejected">' + icon("x") + "Rejected — excluded from the index</div>") +
      "</div></div></div>";
  }

  function rerenderItem(id) {
    const item = D().validationItems.find((v) => v.id === id);
    const el = document.getElementById("card-" + id);
    if (item && el) el.outerHTML = itemHtml(item);
    updateCounts();
  }

  function updateCounts() {
    const el = document.getElementById("vq-count");
    if (el) el.textContent = LS.pendingValidationCount() + " records pending review";
  }

  LS.mapAlias = function () {
    LS.state.set("aliasMapped", true);
    LS.toast("<b>Machine alias updated.</b> " + D().aliasGroup.count + " records mapped to " + esc(D().aliasGroup.targetName) + ".");
    const card = document.getElementById("alias-card");
    if (card) card.outerHTML = aliasHtml();
    updateCounts();
  };

  function aliasHtml() {
    const g = D().aliasGroup;
    if (LS.state.get("aliasMapped")) {
      return '<div class="banner banner-good mb-20" id="alias-card">' + icon("check") +
        "<div><strong>Machine alias updated</strong>" + g.count + " records using <span class='alias-name'>" + esc(g.alias) +
        "</span> are now mapped to <b>" + esc(g.targetName) + "</b> and included in its history, search and statistics. " +
        '<div class="banner-actions"><a class="btn btn-sm" href="#/machine/' + g.target + '">' + icon("box") + "Open " + esc(g.targetName) + "</a></div></div></div>";
    }
    return '<div class="alias-card mb-20" id="alias-card">' +
      '<div class="alias-text"><strong>' + icon("box") + " Grouped machine alias detected</strong>" +
      "<span><b>" + g.count + " records</b> use the machine name <span class='alias-name'>" + esc(g.alias) +
      "</span> — LogSense suggests they refer to <b>" + esc(g.targetName) + "</b>.</span>" +
      '<span class="small muted">Samples: ' + g.samples.map((s) => "“" + esc(s) + "”").join(" · ") + "</span></div>" +
      '<button class="btn btn-primary" onclick="LS.mapAlias()">' + icon("check") + "Map to " + esc(g.targetName) + "</button></div>";
  }

  LS.views.validation = {
    render(host) {
      host.innerHTML =
        '<div class="page-head"><div><h1>AI Validation Queue</h1>' +
        '<p class="page-sub">Review records where AI confidence is below the automatic approval threshold. Approved records enter machine history, search and statistics.</p></div>' +
        '<div class="page-actions"><span class="tbl-count" id="vq-count" style="align-self:center"></span></div></div>' +
        aliasHtml() +
        '<div style="display:grid;gap:14px">' +
        D().validationItems.map(itemHtml).join("") +
        "</div>" +
        '<div class="banner banner-info mt-20">' + icon("shield") +
        "<div><strong>Human validation is part of the product</strong>Low-confidence extractions are never silently trusted — a reviewer approves, edits or rejects them before they join the searchable history.</div></div>";
      updateCounts();
    },
  };
})();
