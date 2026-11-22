// Renders the social preview (1200x630) and the apple-touch-icon (180x180) for each product.
// Usage (needs @playwright/test, e.g. from web/): node ../brand/og/generate.mjs
import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const dev = resolve(here, '../../..');
const PRODUCTS = [
  { name: 'Dispatch', line: 'Every parcel, every courier, every dirham. Live.', out: `${dev}/spring-parcel-platform/dashboard/public` },
  { name: 'Settle', line: 'Every dirham your couriers collect, accounted for.', out: `${dev}/spring-cod-settlement/web/public` },
  { name: 'Copilot', line: 'Ask about your cash. Get the exact figure, and its source.', out: `${dev}/spring-ai-settlement-copilot/web/public` },
];
const mark = (tile, ink, dot, size) => `<svg width="${size}" height="${size}" viewBox="0 0 32 32"><rect width="32" height="32" rx="7" fill="${tile}"/><path d="M8 23.5V9.5l8 8.5 8-8.5v7.5" fill="none" stroke="${ink}" stroke-width="3.3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="23.4" r="2.7" fill="${dot}"/></svg>`;
const font = '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">';

const og = (p) => `<!doctype html><html><head>${font}<style>
body{margin:0;width:1200px;height:630px;font-family:Inter,sans-serif;color:#fff;background:#1D3B72;position:relative;overflow:hidden}
.dots{position:absolute;inset:0;opacity:.12;background-image:radial-gradient(#fff 1.4px,transparent 1.4px);background-size:28px 28px}
.glow{position:absolute;right:-180px;bottom:-220px;width:620px;height:620px;border-radius:50%;background:#00B37E;opacity:.28;filter:blur(90px)}
.route{position:absolute;right:70px;top:120px;opacity:.16}
.wrap{position:absolute;left:84px;top:78px;right:84px;bottom:70px;display:flex;flex-direction:column}
.brand{display:flex;align-items:center;gap:16px;font-size:30px;font-weight:600;letter-spacing:-.01em}
.brand span{opacity:.75;font-weight:400}
h1{margin:auto 0 0;font-size:92px;font-weight:700;letter-spacing:-.03em;line-height:1}
h1 b{color:#7FE3C1;font-weight:700}
p{margin:22px 0 0;font-size:34px;line-height:1.25;max-width:820px;opacity:.9}
.chips{display:flex;gap:10px;margin-top:40px}
.chip{border:1px solid rgba(255,255,255,.3);border-radius:99px;padding:8px 18px;font-size:20px;opacity:.85}
.chip.on{background:#fff;color:#1D3B72;opacity:1;font-weight:600;border-color:#fff}
</style></head><body><div class="dots"></div><div class="glow"></div>
<svg class="route" width="420" height="420" viewBox="0 0 32 32"><path d="M8 23.5V9.5l8 8.5 8-8.5v7.5" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="23.4" r="2.2" fill="#00B37E"/></svg>
<div class="wrap"><div class="brand">${mark('rgba(255,255,255,.12)', '#fff', '#00B37E', 56)}MSE <span>Transport</span></div>
<h1>MSE <b>${p.name}</b></h1><p>${p.line}</p>
<div class="chips">${['Dispatch', 'Settle', 'Copilot'].map((n) => `<span class="chip ${n === p.name ? 'on' : ''}">MSE ${n}</span>`).join('')}</div></div></body></html>`;

const b = await chromium.launch();
for (const p of PRODUCTS) {
  mkdirSync(p.out, { recursive: true });
  const page = await b.newPage({ viewport: { width: 1200, height: 630 } });
  await page.setContent(og(p), { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: `${p.out}/og.png` });
  const icon = await b.newPage({ viewport: { width: 180, height: 180 } });
  await icon.setContent(`<body style="margin:0;background:#1D3B72;display:grid;place-items:center;height:180px">${mark('#1D3B72', '#fff', '#00B37E', 150)}</body>`);
  await icon.screenshot({ path: `${p.out}/apple-touch-icon.png` });
  console.log('wrote', p.out);
}
await b.close();
