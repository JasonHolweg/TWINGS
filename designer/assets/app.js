"use strict";
/* =========================================================================
   TWINGS shared runtime: Minecraft particle textures + tinting, wing YAML
   parsing/serialization, i18n, API client. Used by index.html + library.html.
   ========================================================================= */

/* ---------------- particle definitions ---------------- */
// Special (non-dust) particles. tex = key in TEX, tint = null (texture is
// already colored) or a hex color multiplied onto the texture — the same
// way the game tints e.g. glint green for HAPPY_VILLAGER.
// `scale` is the particle's real in-game size relative to a dust pixel (1.0).
// HEART is genuinely huge in Minecraft and its size cannot be reduced via the
// API, so the preview shows it large — what you see is what you get.
const SPECIALS = {
  FL: { p: "FLAME",           tex: "flame",    tint: null,      c: "#ff9a2c", scale: 1.3 },
  SO: { p: "SOUL_FIRE_FLAME", tex: "soul",     tint: null,      c: "#4fd8ff", scale: 1.3 },
  ER: { p: "END_ROD",         tex: "glitter",  tint: null,      c: "#f4eeff", scale: 1.1 },
  HV: { p: "HAPPY_VILLAGER",  tex: "glint",    tint: "#4bd151", c: "#4bd151", scale: 1.9 },
  EN: { p: "ENCHANT",         tex: "sga",      tint: null,      c: "#d9d9ff", scale: 1.2 },
  HE: { p: "HEART",           tex: "heart",    tint: null,      c: "#ff5c8a", scale: 3.0 },
  WI: { p: "WITCH",           tex: "spell",    tint: "#c24bff", c: "#c24bff", scale: 1.6 },
  SN: { p: "SNOWFLAKE",       tex: "glitter2", tint: "#bfe6ff", c: "#bfe6ff", scale: 1.2 },
  GL: { p: "GLOW",            tex: "glow",     tint: null,      c: "#6ef0d0", scale: 1.4 },
};
function cellScale(cell) {
  return (cell && cell.t === "s") ? (SPECIALS[cell.key]?.scale ?? 1.3) : 1.0;
}
const PARTICLE_TO_SPECIAL = Object.fromEntries(Object.entries(SPECIALS).map(([k, v]) => [v.p, k]));

// pre-1.20.5 names found in old wing files (mirrors the plugin's ParticleAliases)
const LEGACY_PARTICLES = {
  VILLAGER_HAPPY: "HAPPY_VILLAGER", ENCHANTMENT_TABLE: "ENCHANT",
  SPELL_WITCH: "WITCH", REDSTONE: "DUST",
};
function normalizeParticle(name) { return LEGACY_PARTICLES[name] ?? name; }

/* ---------------- texture loading & tinting ---------------- */
const Textures = {
  images: {},
  ready: null,
  tintCache: new Map(),

  load() {
    if (this.ready) return this.ready;
    this.ready = Promise.all(Object.entries(TEX).map(([key, uri]) => new Promise(resolve => {
      const img = new Image();
      img.onload = () => { this.images[key] = img; resolve(); };
      img.onerror = () => resolve();
      img.src = uri;
    })));
    return this.ready;
  },

  /** Returns a canvas of the texture, optionally tinted (multiply), cached. */
  sprite(texKey, tint) {
    const cacheKey = texKey + "|" + (tint || "");
    let c = this.tintCache.get(cacheKey);
    if (c) return c;
    const img = this.images[texKey];
    if (!img) return null;
    c = document.createElement("canvas");
    c.width = img.width; c.height = img.height;
    const x = c.getContext("2d");
    x.imageSmoothingEnabled = false;
    x.drawImage(img, 0, 0);
    if (tint) {
      x.globalCompositeOperation = "multiply";
      x.fillStyle = tint;
      x.fillRect(0, 0, c.width, c.height);
      x.globalCompositeOperation = "destination-in";
      x.drawImage(img, 0, 0);
      x.globalCompositeOperation = "source-over";
    }
    this.tintCache.set(cacheKey, c);
    return c;
  },

  /** Sprite for a grid cell: dust cells use the game's generic texture tinted. */
  forCell(cell) {
    if (!cell) return null;
    if (cell.t === "c") return this.sprite("gen4", cell.hex);
    const sp = SPECIALS[cell.key];
    return sp ? this.sprite(sp.tex, sp.tint) : this.sprite("gen4", "#bbbbbb");
  },
};

function cellColor(cell) {
  if (!cell) return "#888888";
  if (cell.t === "c") return cell.hex;
  return SPECIALS[cell.key]?.c ?? "#bbbbbb";
}

/* ---------------- wing YAML: parse ---------------- */
function parseWingYaml(text) {
  const particles = {};
  const pattern = [];
  const top = {};
  let section = null, inPattern = false;
  for (const raw of String(text).split(/\r?\n/)) {
    const line = raw.replace(/\t/g, "  ");
    if (!line.trim() || line.trim().startsWith("#")) continue;
    if (inPattern) {
      const m = line.match(/^\s*-\s*(.+)$/);
      if (m) { pattern.push(m[1].trim().replace(/^['"]|['"]$/g, "")); continue; }
      inPattern = false;
    }
    if (/^\S/.test(line)) {
      const m = line.match(/^([A-Za-z_]+):\s*(.*)$/);
      if (!m) continue;
      const key = m[1], val = m[2].trim();
      if (key === "Particles") { section = "Particles"; continue; }
      if (key === "Item") { section = "Item"; continue; }
      if (key === "pattern") { inPattern = true; section = null; continue; }
      if (key === "exclude") { section = "exclude"; continue; }
      section = null;
      top[key] = val.replace(/^['"]|['"]$/g, "");
    } else if (section === "Particles") {
      const m = line.match(/^\s+([^:\s]+):\s*(.+)$/);
      if (m) particles[m[1]] = m[2].trim().replace(/^['"]|['"]$/g, "");
    } else if (section === "Item") {
      const m = line.match(/^\s+([^:\s]+):\s*(.+)$/);
      if (m) top["Item." + m[1]] = m[2].trim().replace(/^['"]|['"]$/g, "");
    }
  }
  if (pattern.length === 0) return null;
  return { particles, pattern, top };
}

/** Converts a parsed wing into grid data {w,h,cells,meta}. */
function gridFromParsed(parsed) {
  if (!parsed) return null;
  const rows = parsed.pattern.map(r => r.split(",").map(s => s.trim()));
  const h = rows.length;
  const w = Math.max(...rows.map(r => r.length));
  if (w < 1 || h < 1 || w > 64 || h > 64) return null;
  const codeCell = {};
  for (const [code, val] of Object.entries(parsed.particles)) {
    const parts = String(val).split(":");
    const pname = normalizeParticle(parts[0].toUpperCase());
    if (pname === "DUST") {
      const rgb = (parts[1] || "255,255,255").replace(/[()\s]/g, "").split(",").map(Number);
      const hex = "#" + rgb.slice(0, 3).map(n => Math.max(0, Math.min(255, n | 0)).toString(16).padStart(2, "0")).join("");
      codeCell[code] = { t: "c", hex };
    } else if (PARTICLE_TO_SPECIAL[pname]) {
      codeCell[code] = { t: "s", key: PARTICLE_TO_SPECIAL[pname] };
    } else {
      codeCell[code] = { t: "c", hex: "#bbbbbb" };
    }
  }
  const cells = new Array(w * h).fill(null);
  rows.forEach((row, r) => row.forEach((tok, c) => {
    if (tok.toLowerCase() !== "x" && codeCell[tok]) cells[r * w + c] = { ...codeCell[tok] };
  }));
  const t = parsed.top;
  const stripColors = s => (s || "").replace(/&#[0-9a-f]{6}/gi, "").replace(/&[0-9a-fk-or]/gi, "").trim();
  return {
    w, h, cells,
    // the plugin derives the wing's x-origin from row 0's token count (+1
    // phantom column), not the widest row; preserve that for faithful preview
    // of ragged hand-authored files. Equals w+1 for rectangular wings.
    geometryCols: rows[0].length + 1,
    meta: {
      mirror: String(t.mirrow ?? t.mirror) === "true",
      animated: String(t.Animated) === "true",
      deg: parseInt(t.DegreeAddition) || 0,
      sneakAdd: parseInt(t.SneakingDegreeAddition) || 0,
      rotation: parseInt(t.rotation) || 0,
      tilt: parseInt(t.tilt) || 0,
      spacing: parseFloat(t.spacing) || 0.07,
      moveup: parseFloat(t.moveup) || 0,
      moveback: parseFloat(t.moveback) || 0,
      animSpeed: parseFloat(t.animationspeed) || 1,
      respawnRate: Math.max(1, parseInt(t.updaterate) || 3),
      material: (t["Item.Material"] || "ELYTRA").toUpperCase(),
      name: stripColors(t["Item.Name"]),
      creator: t.creator || "",
    },
  };
}

/* ---------------- wing YAML: serialize ---------------- */
function yamlFromState(s) {
  const colorCodes = new Map();
  let ci = 1;
  for (const cell of s.cells) {
    if (cell && cell.t === "c" && !colorCodes.has(cell.hex)) colorCodes.set(cell.hex, "C" + (ci++));
  }
  const usedSpecials = new Set(s.cells.filter(c => c && c.t === "s").map(c => c.key));
  const lines = [];
  lines.push("# Created with the TWINGS Designer — jasonholweg.de/twings");
  lines.push("Particles:");
  for (const [hex, code] of colorCodes) {
    const n = parseInt(hex.slice(1), 16);
    lines.push(`  ${code}: 'DUST:${(n >> 16) & 255},${(n >> 8) & 255},${n & 255}:0.8'`);
  }
  for (const key of usedSpecials) {
    lines.push(`  ${key}: '${SPECIALS[key].p}'`);
  }
  lines.push("Item:");
  lines.push(`  Material: ${(s.material || "ELYTRA").replace(/[^A-Z0-9_]/gi, "").toUpperCase() || "ELYTRA"}`);
  lines.push(`  Name: '&d${(s.name || "Wings").replace(/'/g, "")}'`);
  lines.push("permission: ''");
  lines.push(`creator: '${(s.creator || "TWINGS Designer").replace(/'/g, "")}'`);
  lines.push(`DegreeAddition: ${s.deg}`);
  lines.push(`SneakingDegreeAddition: ${s.sneakAdd || 0}`);
  lines.push("ShowWhenRunning: false");
  lines.push(`tilt: ${s.tilt}`);
  lines.push("tiltbefore: false");
  lines.push("runtilt: 0");
  lines.push(`mirrow: ${s.mirror}`);
  lines.push(`moveup: ${Math.round(s.moveup * 100) / 100}`);
  lines.push(`moveback: ${Math.round(s.moveback * 100) / 100}`);
  lines.push(`rotation: ${s.rotation || 0}`);
  lines.push(`spacing: ${s.spacing}`);
  lines.push(`updaterate: ${Math.max(1, Math.round(s.respawnRate || 3))}`);
  lines.push(`animationspeed: ${Math.round((s.animSpeed || 1) * 100) / 100}`);
  lines.push("category: wings");
  lines.push(`Animated: ${s.animated}`);
  lines.push("exclude:");
  lines.push("- x");
  lines.push("pattern:");
  for (let r = 0; r < s.h; r++) {
    const toks = [];
    for (let c = 0; c < s.w; c++) {
      const cell = s.cells[r * s.w + c];
      if (!cell) toks.push("x");
      else if (cell.t === "c") toks.push(colorCodes.get(cell.hex));
      else toks.push(cell.key);
    }
    lines.push("- " + toks.join(","));
  }
  return lines.join("\n") + "\n";
}

/* ---------------- 2D pattern thumbnail (library cards, dialogs) ---------------- */
function renderPattern2D(canvas, grid, opts = {}) {
  const mirror = opts.mirror ?? grid.meta?.mirror ?? false;
  const cols = mirror ? grid.w * 2 : grid.w;
  const pad = opts.pad ?? 8;
  const cssW = canvas.clientWidth || canvas.width || 200;
  const cssH = canvas.clientHeight || canvas.height || 140;
  const dpr = window.devicePixelRatio || 1;
  canvas.width = cssW * dpr; canvas.height = cssH * dpr;
  const x = canvas.getContext("2d");
  x.setTransform(dpr, 0, 0, dpr, 0, 0);
  x.imageSmoothingEnabled = false;
  x.clearRect(0, 0, cssW, cssH);
  const cell = Math.max(1, Math.min((cssW - pad * 2) / cols, (cssH - pad * 2) / grid.h));
  const ox = (cssW - cell * cols) / 2, oy = (cssH - cell * grid.h) / 2;
  for (let r = 0; r < grid.h; r++) {
    for (let dc = 0; dc < cols; dc++) {
      const sc = !mirror ? dc : (dc < grid.w ? dc : (grid.w - 1) - (dc - grid.w));
      const v = grid.cells[r * grid.w + sc];
      if (!v) continue;
      const px = ox + dc * cell, py = oy + r * cell;
      if (v.t === "c") {
        x.fillStyle = v.hex;
        x.fillRect(px, py, Math.ceil(cell), Math.ceil(cell));
      } else {
        const sp = Textures.forCell(v);
        if (sp) x.drawImage(sp, px - cell * 0.15, py - cell * 0.15, cell * 1.3, cell * 1.3);
        else { x.fillStyle = cellColor(v); x.fillRect(px, py, Math.ceil(cell), Math.ceil(cell)); }
      }
    }
  }
}

/* ---------------- i18n ---------------- */
const I18N_SHARED = {
  de: {
    navDesigner: "Designer", navLibrary: "Bibliothek", help: "Hilfe",
    copy: "Kopieren", copied: "Kopiert", close: "Schließen",
    downloads: "Installationen", by: "von", openInDesigner: "Im Designer öffnen",
    copyCommand: "Befehl kopieren", loadError: "Konnte nicht geladen werden.",
  },
  en: {
    navDesigner: "Designer", navLibrary: "Library", help: "Help",
    copy: "Copy", copied: "Copied", close: "Close",
    downloads: "installs", by: "by", openInDesigner: "Open in designer",
    copyCommand: "Copy command", loadError: "Could not load.",
  },
};

const store = {
  get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch (e) {} },
};

let LANG = (store.get("twings.lang") || (navigator.language || "de").slice(0, 2)) === "en" ? "en" : "de";
let I18N_PAGE = { de: {}, en: {} };

function t(key) {
  return I18N_PAGE[LANG]?.[key] ?? I18N_SHARED[LANG]?.[key]
      ?? I18N_PAGE.de[key] ?? I18N_SHARED.de[key] ?? key;
}
function setPageStrings(dict) { I18N_PAGE = dict; }
function applyLang() {
  document.querySelectorAll("[data-i18n]").forEach(el => { el.textContent = t(el.dataset.i18n); });
  document.querySelectorAll("[data-i18n-ph]").forEach(el => { el.placeholder = t(el.dataset.i18nPh); });
  document.documentElement.lang = LANG;
  document.querySelectorAll(".lang-opt").forEach(b => b.classList.toggle("active", b.dataset.lang === LANG));
  store.set("twings.lang", LANG);
}
function setLang(l) { LANG = l === "en" ? "en" : "de"; applyLang(); document.dispatchEvent(new Event("langchange")); }

/* ---------------- API client ---------------- */
const api = {
  async publish(payload) {
    const res = await fetch("api.php?action=publish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    return res.json();
  },
  async list(params = {}) {
    const q = new URLSearchParams({ action: "list", ...params });
    const res = await fetch("api.php?" + q);
    return res.json();
  },
  async getYaml(id) {
    const res = await fetch("api.php?action=get&id=" + encodeURIComponent(id) + "&stat=0");
    if (!res.ok) return null;
    return res.text();
  },
};

function installCommand(id) { return "/twings install " + id; }

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (e) {
    const ta = document.createElement("textarea");
    ta.value = text; ta.style.position = "fixed"; ta.style.opacity = "0";
    document.body.appendChild(ta); ta.select();
    let ok = false;
    try { ok = document.execCommand("copy"); } catch (e2) {}
    ta.remove();
    return ok;
  }
}

/* ---------------- toast ---------------- */
function toast(msg, kind = "ok") {
  let el = document.getElementById("toast");
  if (!el) {
    el = document.createElement("div");
    el.id = "toast"; el.className = "toast";
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.dataset.kind = kind;
  el.classList.add("show");
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove("show"), 2400);
}

/* ---------------- shared nav wiring ---------------- */
function initNav() {
  document.querySelectorAll(".lang-opt").forEach(b => {
    b.addEventListener("click", () => setLang(b.dataset.lang));
  });
}
