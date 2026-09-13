/* ==========================================================================
   LogSense demo — app shell: icons, state, router, drawer/modal/toasts,
   global search, topbar menus, record drawer, chart helper.
   ========================================================================== */

window.LS = window.LS || {};

(function () {
  "use strict";

  /* ---------------- Inline SVG icons (feather-style, no CDN needed) ---------------- */

  const ICONS = {
    layers: '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>',
    grid: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>',
    box: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
    clock: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
    activity: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>',
    trending: '<polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/>',
    wrench: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>',
    chat: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
    phone: '<rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>',
    upload: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>',
    checksq: '<polyline points="9 11 12 14 22 4"/><path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
    users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    sliders: '<line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/>',
    search: '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',
    bell: '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>',
    help: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
    x: '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
    check: '<polyline points="20 6 9 17 4 12"/>',
    chevdown: '<polyline points="6 9 12 15 18 9"/>',
    chevright: '<polyline points="9 18 15 12 9 6"/>',
    arrowright: '<line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>',
    arrowdown: '<line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/>',
    arrowup: '<line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>',
    alert: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
    alertc: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>',
    info: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/>',
    send: '<line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>',
    database: '<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>',
    zap: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
    eye: '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-4-8-11-8z" transform="translate(0 0)"/><circle cx="12" cy="12" r="3"/>',
    edit: '<path d="M17 3a2.83 2.83 0 0 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>',
    refresh: '<polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
    mappin: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>',
    menu: '<line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/>',
    mic: '<path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>',
    play: '<polygon points="5 3 19 12 5 21 5 3"/>',
    external: '<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>',
    cpu: '<rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><line x1="9" y1="1" x2="9" y2="4"/><line x1="15" y1="1" x2="15" y2="4"/><line x1="9" y1="20" x2="9" y2="23"/><line x1="15" y1="20" x2="15" y2="23"/><line x1="20" y1="9" x2="23" y2="9"/><line x1="20" y1="14" x2="23" y2="14"/><line x1="1" y1="9" x2="4" y2="9"/><line x1="1" y1="14" x2="4" y2="14"/>',
    filter: '<polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>',
    barchart: '<line x1="12" y1="20" x2="12" y2="10"/><line x1="18" y1="20" x2="18" y2="4"/><line x1="6" y1="20" x2="6" y2="16"/>',
    repeat: '<polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>',
  };

  LS.icon = function (name, cls) {
    const body = ICONS[name] || ICONS.info;
    return '<svg class="ic ' + (cls || "") + '" viewBox="0 0 24 24" aria-hidden="true" focusable="false">' + body + "</svg>";
  };
  const icon = LS.icon;

  /* ---------------- Demo state (sessionStorage-backed) ---------------- */

  const STATE_KEY = "logsense-demo-state";
  let stateObj = { loggedIn: false, uploadDone: false, aliasMapped: false, whatsappAdded: false, approved: [], rejected: [] };
  try {
    const saved = sessionStorage.getItem(STATE_KEY);
    if (saved) stateObj = Object.assign(stateObj, JSON.parse(saved));
  } catch (e) { /* private mode etc. — in-memory state still works */ }

  LS.state = {
    get: (k) => stateObj[k],
    set: (k, v) => {
      stateObj[k] = v;
      try { sessionStorage.setItem(STATE_KEY, JSON.stringify(stateObj)); } catch (e) { /* noop */ }
      LS.refreshBadges();
    },
    reset: () => {
      try { sessionStorage.removeItem(STATE_KEY); } catch (e) { /* noop */ }
      location.hash = "#/dashboard";
      location.reload();
    },
  };

  /* ---------------- Escaping & tiny helpers ---------------- */

  LS.esc = function (s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  };

  LS.trustBadge = function (kind) {
    const map = {
      FACT: ["badge-fact", "database", "Directly supported by maintenance records."],
      CALCULATED: ["badge-calc", "barchart", "Computed deterministically from structured maintenance records — not generated by the AI."],
      HYPOTHESIS: ["badge-hypo", "alert", "Possible explanation. Requires human engineering validation — never shown as a confirmed root cause."],
      NEW: ["badge-new", "zap", "Added in this session."],
      REVIEW: ["badge-review", "eye", "Requires engineering review."],
    };
    const [cls, ic, tipText] = map[kind] || map.FACT;
    return '<span class="tip badge ' + cls + '" tabindex="0">' + icon(ic) + kind +
      '<span class="tip-body">' + tipText + "</span></span>";
  };

  LS.statusBadge = function (status) {
    const map = {
      Healthy: ["status-healthy", "check"],
      Attention: ["status-attention", "alertc"],
      Recurring: ["status-recurring", "repeat"],
    };
    const [cls, ic] = map[status] || map.Healthy;
    return '<span class="status-badge ' + cls + '">' + icon(ic) + status + "</span>";
  };

  LS.srcTag = function (rec) {
    if (rec.src.f === "WhatsApp Entry Agent") {
      return '<span class="src-tag src-wa">' + icon("chat") + "WhatsApp</span>";
    }
    return '<span class="src-tag">' + icon("file") + LS.esc(rec.src.f) + "</span>";
  };

  /* ---------------- Toasts ---------------- */

  LS.toast = function (msg, type) {
    const host = document.getElementById("toasts");
    const el = document.createElement("div");
    el.className = "toast" + (type ? " toast-" + type : "");
    el.setAttribute("role", "status");
    const ic = type === "info" ? "info" : type === "warn" ? "alert" : "check";
    el.innerHTML = icon(ic) + "<div>" + msg + "</div>";
    host.appendChild(el);
    setTimeout(() => {
      el.classList.add("leaving");
      setTimeout(() => el.remove(), 300);
    }, 3600);
  };

  /* ---------------- Drawer & modal ---------------- */

  const drawer = () => document.getElementById("drawer");
  const drawerScrim = () => document.getElementById("drawer-scrim");

  LS.openDrawer = function (html) {
    drawer().innerHTML = html;
    drawer().hidden = false;
    drawerScrim().hidden = false;
    const closeBtn = drawer().querySelector("[data-close-drawer]");
    if (closeBtn) closeBtn.focus();
  };
  LS.closeDrawer = function () {
    drawer().hidden = true;
    drawerScrim().hidden = true;
  };

  LS.openModal = function (html) {
    document.getElementById("modal").innerHTML = html;
    document.getElementById("modal-scrim").hidden = false;
  };
  LS.closeModal = function () {
    document.getElementById("modal-scrim").hidden = true;
  };

  /* ---------------- Record source drawer (traceability) ---------------- */

  LS.openRecord = function (recordId) {
    const rec = LS.data.recordById(recordId);
    if (!rec) { LS.toast("Record not found in demo data.", "warn"); return; }
    const m = LS.data.machineById(rec.machineId);
    const isWa = rec.src.f === "WhatsApp Entry Agent";
    LS.openDrawer(
      '<div class="drawer-head"><h2>Source Record #' + rec.id + "</h2>" +
      '<button class="icon-btn" data-close-drawer aria-label="Close">' + icon("x") + "</button></div>" +
      '<div class="drawer-body">' +
      '<div class="flex">' + LS.trustBadge("FACT") + (rec.isNew ? LS.trustBadge("NEW") : "") + LS.srcTag(rec) + "</div>" +
      '<div><div class="raw-label">' + icon("file", "ic-sm") + "Original entry (as recorded)</div>" +
      '<div class="raw-block">' + LS.esc(rec.raw) + "</div></div>" +
      '<div><div class="raw-label">' + icon("zap", "ic-sm") + "Normalized interpretation</div>" +
      '<dl class="kv">' +
      "<dt>Date</dt><dd>" + LS.data.fmtDate(rec.date) + "</dd>" +
      "<dt>Machine</dt><dd>" + (m ? LS.esc(m.name) : rec.machineId) + "</dd>" +
      "<dt>Failure mode</dt><dd>" + LS.esc(rec.mode) + "</dd>" +
      "<dt>Action</dt><dd>" + LS.esc(rec.normalized) + "</dd>" +
      "<dt>Part</dt><dd>" + (rec.part ? LS.esc(rec.part) : "—") + "</dd>" +
      "<dt>Downtime</dt><dd>" + (rec.downtime > 0 ? LS.data.fmtHrs(rec.downtime) : "—") + "</dd>" +
      "<dt>Technician</dt><dd>" + LS.esc(rec.tech) + "</dd>" +
      "<dt>Source</dt><dd>" + LS.esc(rec.src.f) + "</dd>" +
      (isWa ? "" : "<dt>Sheet / section</dt><dd>" + LS.esc(rec.src.s) + "</dd><dt>Row / page</dt><dd>" + LS.esc(rec.src.r) + "</dd>") +
      "<dt>Record ID</dt><dd class=\"mono\">#" + rec.id + "</dd>" +
      "</dl></div>" +
      (m ? '<a class="btn" href="#/machine/' + m.id + '" onclick="LS.closeDrawer()">' + icon("box") + "Open " + LS.esc(m.name) + "</a>" : "") +
      "</div>"
    );
  };

  /* ---------------- Chart helper (Chart.js via CDN, graceful fallback) ---------------- */

  LS._charts = {};
  LS.chart = function (canvasId, config) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    if (typeof Chart === "undefined") {
      const box = canvas.parentElement;
      box.innerHTML = '<div class="chart-fallback">Chart library not loaded (offline?). The underlying data is still available in the tables and record views.</div>';
      return;
    }
    if (LS._charts[canvasId]) { LS._charts[canvasId].destroy(); }
    LS._charts[canvasId] = new Chart(canvas.getContext("2d"), config);
  };

  LS.chartDefaults = function () {
    if (typeof Chart === "undefined") return;
    Chart.defaults.font.family = '"IBM Plex Sans", "Segoe UI", system-ui, sans-serif';
    Chart.defaults.font.size = 11.5;
    Chart.defaults.color = "#898781";
    Chart.defaults.plugins.legend.display = false;
    Chart.defaults.plugins.tooltip.backgroundColor = "#22303e";
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.plugins.tooltip.cornerRadius = 8;
    Chart.defaults.plugins.tooltip.titleFont = { weight: "600" };
    Chart.defaults.animation.duration = 500;
  };

  /* ---------------- Router ---------------- */

  LS.views = LS.views || {};

  const NAV = [
    { group: null, items: [{ route: "dashboard", label: "Dashboard", icon: "grid" }] },
    {
      group: "Operations",
      items: [
        { route: "machines", label: "Machines", icon: "box" },
        { route: "history", label: "Maintenance History", icon: "clock" },
        { route: "patterns", label: "Patterns & Insights", icon: "activity", badge: "insights" },
        { route: "parts", label: "Spare Parts", icon: "wrench" },
      ],
    },
    {
      group: "AI",
      items: [
        { route: "assistant", label: "Maintenance Assistant", icon: "chat" },
        { route: "whatsapp", label: "WhatsApp Agent", icon: "phone" },
      ],
    },
    {
      group: "Data",
      items: [
        { route: "upload", label: "Upload & Import", icon: "upload" },
        { route: "validation", label: "Validation Queue", icon: "checksq", badge: "validation" },
      ],
    },
    {
      group: "Administration",
      items: [
        { route: "users", label: "Users & Roles", icon: "users" },
        { route: "settings", label: "Settings", icon: "sliders" },
      ],
    },
    {
      group: "Demo",
      items: [
        { route: "how-it-works", label: "How It Works", icon: "cpu" },
        { route: "closing", label: "Closing Slide", icon: "play" },
      ],
    },
  ];

  function renderNav() {
    const host = document.getElementById("nav");
    host.innerHTML = NAV.map((g) =>
      (g.group ? '<div class="nav-group">' + g.group + "</div>" : "") +
      g.items.map((it) =>
        '<a href="#/' + it.route + '" data-route="' + it.route + '">' + icon(it.icon) +
        "<span>" + it.label + "</span>" +
        (it.badge ? '<span class="nav-badge" data-badge="' + it.badge + '"></span>' : "") +
        "</a>"
      ).join("")
    ).join("");
  }

  LS.pendingValidationCount = function () {
    const handled = (stateObj.approved || []).length + (stateObj.rejected || []).length;
    const aliasPart = stateObj.aliasMapped ? 0 : LS.data.aliasGroup.count;
    return Math.max(0, aliasPart + LS.data.validationItems.length - handled);
  };

  LS.insightCount = function () {
    return 4 + (stateObj.whatsappAdded ? 1 : 0);
  };

  LS.refreshBadges = function () {
    document.querySelectorAll('[data-badge="validation"]').forEach((el) => {
      const n = LS.pendingValidationCount();
      el.textContent = n;
      el.hidden = n === 0;
    });
    document.querySelectorAll('[data-badge="insights"]').forEach((el) => {
      el.textContent = LS.insightCount();
      el.classList.add("badge-quiet");
    });
  };

  function parseHash() {
    const h = (location.hash || "#/dashboard").replace(/^#\/?/, "");
    const parts = h.split("/");
    return { route: parts[0] || "dashboard", param: parts.slice(1).join("/") || null };
  }

  LS.go = function (route) { location.hash = "#/" + route; };

  function renderRoute() {
    const { route, param } = parseHash();
    const view = LS.views[route] || LS.views.dashboard;
    const host = document.getElementById("view");
    // destroy stale charts
    Object.keys(LS._charts).forEach((k) => { LS._charts[k].destroy(); delete LS._charts[k]; });
    LS.closeDrawer();
    LS.closeModal();
    closeMenus();
    document.getElementById("sidebar").classList.remove("open");
    const scrim = document.getElementById("sidebar-scrim");
    if (scrim) scrim.remove();
    host.innerHTML = "";
    view.render(host, param);
    document.querySelectorAll("#nav a").forEach((a) => {
      a.classList.toggle("active", a.dataset.route === route ||
        (route === "machine" && a.dataset.route === "machines"));
    });
    LS.refreshBadges();
    host.scrollTop = 0;
    window.scrollTo(0, 0);
  }

  /* ---------------- Topbar: menus, notifications, search ---------------- */

  function closeMenus() {
    document.querySelectorAll(".menu-pop").forEach((m) => m.remove());
    const res = document.getElementById("gsearch-results");
    if (res) res.remove();
  }

  function toggleMenu(id, html) {
    const existing = document.getElementById(id);
    closeMenus();
    if (existing) return;
    const el = document.createElement("div");
    el.className = "menu-pop";
    el.id = id;
    el.innerHTML = html;
    document.getElementById("topbar").appendChild(el);
  }

  function userMenu() {
    toggleMenu("menu-user",
      '<div class="menu-head"><strong>' + LS.data.plant.user.name + "</strong><span>" + LS.data.plant.user.role + " · " + LS.data.plant.shortName + "</span></div>" +
      '<button class="menu-item" onclick="LS.go(\'closing\')">' + icon("play") + "Closing slide</button>" +
      '<button class="menu-item" onclick="LS.showDemoScript()">' + icon("help") + "Demo script (presenter aid)</button>" +
      '<button class="menu-item" onclick="LS.state.reset()">' + icon("refresh") + "Reset demo</button>" +
      '<button class="menu-item danger" onclick="LS.logout()">' + icon("logout") + "Sign out</button>"
    );
  }

  function notifMenu() {
    const n = LS.pendingValidationCount();
    toggleMenu("menu-notif",
      '<div class="menu-head"><strong>Notifications</strong><span>Demo Plant</span></div>' +
      '<button class="notif-item" style="width:100%;border:none;background:none;text-align:left" onclick="LS.go(\'validation\')">' + icon("checksq") +
      "<div><p><b>" + n + " records</b> awaiting validation review.</p><time>Today</time></div></button>" +
      '<button class="notif-item" style="width:100%;border:none;background:none;text-align:left" onclick="LS.go(\'machine/CONV-L3-MTR-01\')">' + icon("repeat") +
      "<div><p>Possible bearing recurrence on <b>Line 3 Conveyor Motor</b> (~92-day interval).</p><time>Yesterday</time></div></button>" +
      '<button class="notif-item" style="width:100%;border:none;background:none;text-align:left" onclick="LS.go(\'machine/BLST-L1-02\')">' + icon("alertc") +
      "<div><p>Temporary sensor fix repeated on <b>Blister Machine 2</b>.</p><time>2 days ago</time></div></button>"
    );
  }

  function plantMenu() {
    toggleMenu("menu-plant",
      '<div class="menu-head"><strong>Plants</strong><span>Your organization</span></div>' +
      '<button class="menu-item">' + icon("mappin") + "<span><b>" + LS.data.plant.shortName + "</b><br><span class='muted small'>Pune Manufacturing Facility · active</span></span></button>" +
      '<div class="menu-item muted">' + icon("info") + "Single plant in this demo environment</div>"
    );
  }

  LS.showDemoScript = function () {
    LS.openModal(
      '<div class="modal-head"><h2>9-minute demo script</h2><button class="icon-btn" onclick="LS.closeModal()" aria-label="Close">' + icon("x") + "</button></div>" +
      '<div class="modal-body"><ol style="margin:0;padding-left:20px;display:grid;gap:9px;font-size:13.5px">' +
      "<li><b>0:00 Dashboard</b> — MTTR, MTBF, downtime, breakdowns, AI-detected insights.</li>" +
      "<li><b>1:00 Upload</b> — import <code>maintenance_log_2023.xlsx</code>; watch the pipeline process 1,842 records.</li>" +
      "<li><b>2:00 Validation</b> — bulk-map the “Conv Motor-3” alias (37 records) and approve a low-confidence record.</li>" +
      "<li><b>3:00 Machine history</b> — open Line 3 Conveyor Motor: 23 records, failure Pareto, timeline, View Source.</li>" +
      "<li><b>4:00 AI chat</b> — ask “Line 3 ke conveyor motor pe pichle 2 saal mein kya kya hua?”; click a citation.</li>" +
      "<li><b>5:00 Pattern</b> — ~92-day bearing recurrence, labelled HYPOTHESIS (never a confirmed root cause).</li>" +
      "<li><b>6:00 Statistics</b> — “Total downtime bearing failures Line 3 last 2 years” → 18.7 hrs, 7/7 records, CALCULATED.</li>" +
      "<li><b>6:45 Spare parts</b> — “6205 bearing kahan kahan lagi hai?” → 3 machines, Line 3 concentration.</li>" +
      "<li><b>7:30 WhatsApp</b> — technician reports the breakdown in Hinglish; agent clarifies downtime; confirm.</li>" +
      "<li><b>9:00 Updated history</b> — record #2048 appears; 5th bearing replacement triggers the recurrence alert.</li>" +
      "<li><b>9:30 Closing slide</b> — “Your Plant’s Memory, Searchable.”</li>" +
      "</ol></div>"
    );
  };

  /* Global search */

  function runGlobalSearch(q) {
    const old = document.getElementById("gsearch-results");
    if (old) old.remove();
    if (!q || q.trim().length < 2) return;
    const wrap = document.createElement("div");
    wrap.className = "gsearch-results";
    wrap.id = "gsearch-results";

    const machines = LS.data.searchMachines(q).slice(0, 4);
    const records = LS.data.searchRecords(q, 5);
    const parts = LS.data.partStats().filter((p) => p.part.toLowerCase().includes(q.toLowerCase())).slice(0, 3);

    let html = "";
    if (machines.length) {
      html += '<div class="gsr-group">Machines</div>' + machines.map((m) =>
        '<button class="gsr-item" onclick="LS.go(\'machine/' + m.id + '\')">' + icon("box") +
        '<span><span class="gsr-title">' + LS.esc(m.name) + '</span><br><span class="gsr-sub">' + m.line + " · " + LS.esc(m.type) + "</span></span></button>"
      ).join("");
    }
    if (parts.length) {
      html += '<div class="gsr-group">Spare parts</div>' + parts.map((p) =>
        '<button class="gsr-item" onclick="LS.go(\'parts\')">' + icon("wrench") +
        '<span><span class="gsr-title">' + LS.esc(p.part) + '</span><br><span class="gsr-sub">' + p.machines.length + " machines · " + p.count + " uses</span></span></button>"
      ).join("");
    }
    if (records.length) {
      html += '<div class="gsr-group">Maintenance records</div>' + records.map((r) => {
        const m = LS.data.machineById(r.record.machineId);
        return '<button class="gsr-item" onclick="LS.openRecord(' + r.record.id + ')">' + icon("file") +
          '<span><span class="gsr-title">' + LS.esc(r.record.normalized) + '</span><br><span class="gsr-sub">#' + r.record.id + " · " + (m ? LS.esc(m.name) : "") + " · " + LS.data.fmtDate(r.record.date) + "</span></span></button>";
      }).join("");
    }
    if (!html) html = '<div class="gsr-empty">No matches in Demo Plant data for “' + LS.esc(q) + "”.</div>";
    wrap.innerHTML = html;
    document.querySelector(".gsearch").appendChild(wrap);
  }

  /* ---------------- Login / logout ---------------- */

  LS.login = function () {
    LS.state.set("loggedIn", true);
    document.getElementById("login-screen").hidden = true;
    document.getElementById("app").hidden = false;
    if (!location.hash || location.hash === "#/") location.hash = "#/dashboard";
    renderRoute();
    LS.toast("Signed in to <b>" + LS.data.plant.shortName + "</b> (demo environment).", "info");
  };

  LS.logout = function () {
    LS.state.set("loggedIn", false);
    document.getElementById("app").hidden = true;
    document.getElementById("login-screen").hidden = false;
  };

  /* ---------------- Boot ---------------- */

  document.addEventListener("DOMContentLoaded", () => {
    LS.chartDefaults();
    renderNav();
    LS.refreshBadges();

    // login form
    document.getElementById("login-form").addEventListener("submit", (e) => {
      e.preventDefault();
      LS.login();
    });
    document.getElementById("demo-login").addEventListener("click", LS.login);

    // topbar
    document.getElementById("btn-user").addEventListener("click", (e) => { e.stopPropagation(); userMenu(); });
    document.getElementById("btn-notif").addEventListener("click", (e) => { e.stopPropagation(); notifMenu(); });
    document.getElementById("btn-plant").addEventListener("click", (e) => { e.stopPropagation(); plantMenu(); });
    document.getElementById("btn-help").addEventListener("click", (e) => { e.stopPropagation(); LS.showDemoScript(); });

    const gs = document.getElementById("gsearch-input");
    gs.addEventListener("input", () => runGlobalSearch(gs.value));
    gs.addEventListener("focus", () => runGlobalSearch(gs.value));
    gs.addEventListener("click", (e) => e.stopPropagation());

    // mobile nav
    document.getElementById("nav-toggle").addEventListener("click", (e) => {
      e.stopPropagation();
      const sb = document.getElementById("sidebar");
      const open = sb.classList.toggle("open");
      let scrim = document.getElementById("sidebar-scrim");
      if (open && !scrim) {
        scrim = document.createElement("div");
        scrim.id = "sidebar-scrim";
        scrim.addEventListener("click", () => { sb.classList.remove("open"); scrim.remove(); });
        document.body.appendChild(scrim);
      } else if (!open && scrim) scrim.remove();
    });

    // scrims & escape
    document.getElementById("drawer-scrim").addEventListener("click", LS.closeDrawer);
    document.getElementById("modal-scrim").addEventListener("click", (e) => {
      if (e.target.id === "modal-scrim") LS.closeModal();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") { LS.closeDrawer(); LS.closeModal(); closeMenus(); }
    });
    document.addEventListener("click", (e) => {
      if (!e.target.closest(".menu-pop") && !e.target.closest(".gsearch")) closeMenus();
    });
    document.getElementById("drawer").addEventListener("click", (e) => {
      if (e.target.closest("[data-close-drawer]")) LS.closeDrawer();
    });

    window.addEventListener("hashchange", () => {
      if (stateObj.loggedIn) renderRoute();
    });

    if (stateObj.loggedIn) {
      document.getElementById("login-screen").hidden = true;
      document.getElementById("app").hidden = false;
      renderRoute();
    }
  });
})();
