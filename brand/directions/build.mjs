// Generates the three MSE Transport brand directions: SVG marks/logos + board.html.
// node build.mjs  (then screenshot board.html at 1600px)
import { writeFileSync, mkdirSync } from 'node:fs';

const D = [
  {
    id: 'a', name: 'Route', line: 'Precise, calm and trusted with your cash.',
    why: 'Fintech-grade navy for trust, a green signal for "delivered and paid". The mark is a route that draws the M and drops into its destination.',
    fonts: { head: 'Inter', ui: 'Inter', ar: 'IBM Plex Sans Arabic', mono: 'JetBrains Mono' },
    pal: { primary: '#1D3B72', accent: '#00B37E', bg: '#F7F8FA', panel: '#FFFFFF', line: '#E3E7EE', fg: '#0E1726', muted: '#556074',
           dbg: '#0B1220', dpanel: '#111A2E', dline: '#22304D', dfg: '#E8EEF8', dmuted: '#9AA8C0', dprimary: '#86A8FF', daccent: '#2EE6A6' },
    mark: (t, i, d) => `<rect width="32" height="32" rx="7" fill="${t}"/><path d="M8 23.5V9.5l8 8.5 8-8.5v7.5" fill="none" stroke="${i}" stroke-width="3.3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="23.4" r="2.7" fill="${d}"/>`,
    word: (fg, mu, f) => `<span style="font:700 1em ${f.head};letter-spacing:-.02em;color:${fg}">MSE</span><span style="font:400 1em ${f.head};color:${mu};margin-left:.28em">Transport</span>`,
  },
  {
    id: 'b', name: 'Momentum', line: 'Fast, friendly, always moving.',
    why: 'Electric blue and lime read as modern software, not trucking. The mark is an S drawn as a delivery route with rounded switchbacks, ending at the customer’s door (the lime dot).',
    fonts: { head: 'Readex Pro', ui: 'Plus Jakarta Sans', ar: 'Readex Pro', mono: 'JetBrains Mono' },
    pal: { primary: '#2B59FF', accent: '#B8F03A', bg: '#F6F7FB', panel: '#FFFFFF', line: '#E2E5F0', fg: '#0D1030', muted: '#535A7A',
           dbg: '#090C1F', dpanel: '#10142E', dline: '#232A52', dfg: '#ECEFFF', dmuted: '#9CA3C8', dprimary: '#7D96FF', daccent: '#C8F55A' },
    mark: (t, i, d) => `<rect width="32" height="32" rx="9.5" fill="${t}"/><path d="M23.5 9.5h-10a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7h-5.5" fill="none" stroke="${i}" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="8.6" cy="23.5" r="3" fill="${d}"/>`,
    word: (fg, mu, f) => `<span style="font:700 1em ${f.head};letter-spacing:-.01em;color:${fg}">mse</span><span style="font:400 1em ${f.head};color:${mu};margin-left:.22em">transport</span>`,
  },
  {
    id: 'c', name: 'Seal', line: 'Every dirham accounted for.',
    why: 'Finance-first: deep green and brass, an octagonal seal like a stamp on a receipt. Restrained, premium, built for people who handle cash.',
    fonts: { head: 'Fraunces', ui: 'Inter', ar: 'Noto Kufi Arabic', mono: 'JetBrains Mono' },
    pal: { primary: '#0F3D33', accent: '#C49A4A', bg: '#FBF8F1', panel: '#FFFFFF', line: '#E9E2D3', fg: '#1A1A17', muted: '#5C5A52',
           dbg: '#0B1311', dpanel: '#121C19', dline: '#26332F', dfg: '#F1EEE6', dmuted: '#A8A598', dprimary: '#8FD3BC', daccent: '#DDB76C' },
    mark: (t, i, d) => `<path d="M10.2 1.5h11.6l8.7 8.7v11.6l-8.7 8.7H10.2l-8.7-8.7V10.2z" fill="${t}"/><path d="M10 20.5V11l6 6 6-6v9.5" fill="none" stroke="${i}" stroke-width="2.9" stroke-linecap="round" stroke-linejoin="round"/><rect x="10" y="23.2" width="12" height="2.3" rx="1.15" fill="${d}"/>`,
    word: (fg, mu, f) => `<span style="font:600 .9em ${f.head};letter-spacing:.05em;color:${fg}">MSE</span><span style="display:inline-block;width:1px;height:.8em;background:${mu};opacity:.5;margin:0 .45em;vertical-align:-.05em"></span><span style="font:500 .48em ${f.ui};letter-spacing:.26em;color:${mu};vertical-align:.3em">TRANSPORT</span>`,
  },
];

const svg = (inner, size) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="${size}" height="${size}">${inner}</svg>`;
const ff = (n) => `'${n}', system-ui, sans-serif`;

for (const d of D) {
  const p = d.pal;
  mkdirSync(d.id, { recursive: true });
  writeFileSync(`${d.id}/mark.svg`, svg(d.mark(p.primary, '#FFFFFF', p.accent), 32).replace(' width="32" height="32"', ''));
  writeFileSync(`${d.id}/mark-dark.svg`, svg(d.mark(p.dprimary, p.dbg, p.dbg), 32).replace(' width="32" height="32"', ''));
  writeFileSync(`${d.id}/mark-mono.svg`, svg(d.mark('currentColor', '#FFFFFF', '#FFFFFF'), 32).replace(' width="32" height="32"', ''));
}

const fontsUrl = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=IBM+Plex+Sans+Arabic:wght@400;600&family=JetBrains+Mono:wght@500&family=Readex+Pro:wght@400;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,500;9..144,600&family=Noto+Kufi+Arabic:wght@400;600&display=swap';

const sw = (hex, label) => `<div class="sw"><i style="background:${hex}"></i><b>${label}</b><code>${hex}</code></div>`;

const section = (d) => {
  const p = d.pal, f = Object.fromEntries(Object.entries(d.fonts).map(([k, v]) => [k, ff(v)]));
  const m = (s) => svg(d.mark(p.primary, '#FFFFFF', p.accent), s);
  const md = (s) => svg(d.mark(p.dprimary, p.dbg, p.dbg), s);
  const mono = (s, c) => `<span style="color:${c}">${svg(d.mark('currentColor', c === '#111' ? '#fff' : '#111', c === '#111' ? '#fff' : '#111'), s)}</span>`;
  const kpi = (dark) => {
    const bg = dark ? p.dpanel : p.panel, fg = dark ? p.dfg : p.fg, mu = dark ? p.dmuted : p.muted, ln = dark ? p.dline : p.line, pr = dark ? p.dprimary : p.primary, ac = dark ? p.daccent : p.accent;
    return `<div class="kpi" style="background:${bg};border-color:${ln};font-family:${f.ui}">
      <div style="color:${mu}">Courier still owes</div>
      <div class="big" style="color:${fg};font-family:${f.head}">4,315.00 <small style="color:${mu}">MAD</small></div>
      <div class="bar" style="background:${ln}"><i style="background:${pr};width:62%"></i></div>
      <div style="color:${mu}"><b style="color:${dark ? ac : pr}">62%</b> remitted · Speedex Colis</div></div>`;
  };
  return `
<section style="--p:${p.primary};--a:${p.accent}">
  <header class="dh">
    <span class="tag" style="background:${p.primary}">${d.id.toUpperCase()}</span>
    <h2 style="font-family:${f.head}">${d.name}</h2>
    <p class="line" style="font-family:${f.head}">“${d.line}”</p>
    <p class="why">${d.why}</p>
  </header>
  <div class="grid">
    <div class="card" style="background:${p.bg}">
      <div class="lbl">Logo · light</div>
      <div class="logo" >${m(56)}<span>${d.word(p.fg, p.muted, f)}</span></div>
      <div class="lbl" style="margin-top:22px">Favicon 16 · 32 · mono</div>
      <div class="fav">${m(16)}${m(32)}${mono(32, '#111')}<span style="background:#111;padding:6px;border-radius:8px;display:inline-flex">${mono(24, '#fff')}</span></div>
    </div>
    <div class="card" style="background:${p.dbg}">
      <div class="lbl" style="color:${p.dmuted}">Logo · dark</div>
      <div class="logo" >${md(56)}<span>${d.word(p.dfg, p.dmuted, f)}</span></div>
      <div class="lbl" style="margin-top:22px;color:${p.dmuted}">Favicon on dark</div>
      <div class="fav">${md(16)}${md(32)}${m(32)}</div>
    </div>
    <div class="card" style="background:#fff">
      <div class="lbl">Palette</div>
      <div class="sws">${sw(p.primary, 'Primary')}${sw(p.accent, 'Accent')}${sw(p.fg, 'Ink')}${sw(p.muted, 'Muted')}${sw(p.line, 'Line')}${sw(p.bg, 'Surface')}${sw(p.dbg, 'Dark bg')}${sw(p.dpanel, 'Dark panel')}${sw(p.dprimary, 'Dark primary')}${sw(p.daccent, 'Dark accent')}</div>
    </div>
    <div class="card" style="background:#fff">
      <div class="lbl">Type · ${d.fonts.head}${d.fonts.ui !== d.fonts.head ? ' / ' + d.fonts.ui : ''} / ${d.fonts.ar}</div>
      <div style="font:600 30px/1.1 ${f.head};color:${p.fg};letter-spacing:-.01em">Cash collected, settled.</div>
      <div style="font:400 14px/1.5 ${f.ui};color:${p.muted};margin-top:6px">Reconcile every courier remittance against your orders in minutes.</div>
      <div style="font:600 22px ${f.ar};color:${p.fg};margin-top:8px" dir="rtl">تسوية نقدية دقيقة لكل طرد</div>
      <div style="font:500 13px ${f.mono};color:${p.muted};margin-top:6px">SPX-100007 · 1,250.00 MAD</div>
    </div>
    <div class="card wide" style="background:${p.bg};padding:0;overflow:hidden">
      <div class="hdr" style="background:${p.panel};border-color:${p.line};font-family:${f.ui}">
        ${m(24)}<span style="font:600 15px ${f.head};color:${p.fg}">MSE <span style="color:${p.muted};font-weight:500">Settle</span></span>
        <nav><a style="color:${p.primary};border-color:${p.primary}">Overview</a><a>Statements</a><a>Disputes</a><a>Couriers</a></nav>
        <span class="btn" style="background:${p.primary}">Upload CSV</span>
        <span class="av" style="background:${p.accent}33;color:${p.fg}">AT</span>
      </div>
      <div style="display:flex;gap:12px;padding:14px">${kpi(false)}${kpi(true)}</div>
    </div>
    <div class="card" style="background:#ECEDEF;display:flex;gap:18px;align-items:center;justify-content:center">
      <div class="icon" style="background:${p.primary}">${svg(d.mark(p.primary, '#FFFFFF', p.accent), 78)}</div>
      <div class="bc" style="background:${p.primary};font-family:${f.ui}">
        <div style="display:flex;align-items:center;gap:8px;font-size:18px">${svg(d.mark('rgba(255,255,255,.12)', '#fff', p.accent), 26)}<span>${d.word('#fff', 'rgba(255,255,255,.7)', f)}</span></div>
        <div style="margin-top:auto"><b style="font:600 13px ${f.head}">Founder</b><br><span style="opacity:.75;font-size:11px">msetransport.com</span></div>
      </div>
    </div>
  </div>
</section>`;
};

writeFileSync('board.html', `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>MSE Transport · brand directions</title>
<link rel="stylesheet" href="${fontsUrl}">
<style>
*{box-sizing:border-box}body{margin:0;background:#F2F2EF;font:14px Inter,system-ui,sans-serif;color:#15171A;width:1600px}
.top{padding:36px 48px 8px;display:flex;align-items:baseline;gap:18px}.top h1{margin:0;font:700 28px Inter;letter-spacing:-.02em}.top p{margin:0;color:#5c6170}
section{margin:20px 48px 34px;background:#fff;border-radius:20px;padding:26px;box-shadow:0 1px 2px rgba(0,0,0,.05)}
.dh{display:grid;grid-template-columns:auto auto 1fr;grid-template-rows:auto auto;column-gap:14px;align-items:center;margin-bottom:18px}
.tag{grid-row:span 2;color:#fff;font:700 18px Inter;width:42px;height:42px;border-radius:12px;display:grid;place-items:center}
.dh h2{margin:0;font-size:26px;font-weight:600}.line{margin:0;font-size:18px;color:#3b3f48}.why{grid-column:2/4;margin:2px 0 0;color:#5c6170;max-width:1100px}
.grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr) minmax(0,1.25fr) minmax(0,1fr);gap:14px}
.card{min-width:0;border-radius:14px;padding:18px;border:1px solid rgba(0,0,0,.06)}.wide{grid-column:span 3}
.lbl{font:600 10.5px Inter;letter-spacing:.08em;text-transform:uppercase;color:#7a7f8c;margin-bottom:12px}
.logo{display:flex;align-items:center;gap:12px;white-space:nowrap;font-size:34px!important}.logo>span{line-height:1}
.fav{display:flex;align-items:center;gap:14px}svg{flex-shrink:0}.bc>div:first-child{white-space:nowrap}.logo>span{min-width:0}
.sws{display:grid;grid-template-columns:repeat(5,1fr);gap:8px}.sw{display:flex;flex-direction:column;font-size:11px}.sw i{height:34px;border-radius:8px;border:1px solid rgba(0,0,0,.08);margin-bottom:4px}.sw code{font:10.5px 'JetBrains Mono';color:#7a7f8c}
.hdr{display:flex;align-items:center;gap:10px;padding:10px 16px;border-bottom:1px solid}.hdr nav{display:flex;gap:16px;margin-left:22px;font-size:13px;color:#667}.hdr nav a{padding:4px 0;border-bottom:2px solid transparent;font-weight:500}
.btn{margin-left:auto;color:#fff;font-size:12.5px;font-weight:600;padding:7px 12px;border-radius:8px}.av{width:28px;height:28px;border-radius:99px;display:grid;place-items:center;font-size:11px;font-weight:700}
.kpi{flex:1;border:1px solid;border-radius:12px;padding:14px 16px;font-size:12.5px;display:flex;flex-direction:column;gap:7px}.kpi .big{font-size:28px;font-weight:600;letter-spacing:-.01em}.kpi small{font-size:14px}
.bar{height:6px;border-radius:9px;overflow:hidden}.bar i{display:block;height:100%;border-radius:9px}
.icon{width:96px;height:96px;border-radius:24px;display:grid;place-items:center;box-shadow:0 10px 24px -8px rgba(0,0,0,.35)}
.bc{width:250px;height:150px;border-radius:12px;padding:16px;color:#fff;display:flex;flex-direction:column;box-shadow:0 10px 24px -8px rgba(0,0,0,.35)}
</style></head><body>
<div class="top"><h1>MSE Transport</h1><p>Three brand directions for the suite: MSE Dispatch · MSE Settle · MSE Copilot</p></div>
${D.map(section).join('\n')}
</body></html>`);
console.log('ok');
