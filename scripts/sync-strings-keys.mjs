#!/usr/bin/env node
import { readFileSync, readdirSync, writeFileSync, existsSync } from 'fs';
import { join } from 'path';

const ASSETS = 'app/src/main/assets';
const EN = 'en';
const STRINGS_FILE = 'strings.json';

// Google Cloud Translation API v2. Set GOOGLE_TRANSLATE_API_KEY (e.g. in GitHub Secrets).
// Free tier: 500,000 chars/month. No per-request size limit like MyMemory.
const REQUEST_DELAY_MS = 100;

// Map our locale folder names to Google Translate language codes (source is always 'en').
const LOCALE_TO_LANG = {
  es: 'es',
  fr: 'fr',
  hi: 'hi',
  id: 'id',
  nep: 'ne',
  por: 'pt',
  sw: 'sw',
  tpi: 'tpi',
};

function loadJson(filePath) {
  return JSON.parse(readFileSync(filePath, 'utf8'));
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

/**
 * Translate text from English to targetLang using Google Cloud Translation API.
 * Returns { translated, error } — error is set when API key missing or request failed (for logging).
 */
async function translate(text, targetLang) {
  const apiKey = process.env.GOOGLE_TRANSLATE_API_KEY;
  if (!apiKey) return { translated: null, error: 'GOOGLE_TRANSLATE_API_KEY not set' };

  if (!text || typeof text !== 'string') return { translated: text, error: null };
  const trimmed = text.trim();
  if (!trimmed) return { translated: text, error: null };

  const params = new URLSearchParams({
    key: apiKey,
    q: trimmed,
    target: targetLang,
    source: 'en',
    format: 'text',
  });
  const url = `https://translation.googleapis.com/language/translate/v2?${params.toString()}`;

  try {
    const res = await fetch(url, { method: 'POST' });
    const data = await res.json().catch(() => ({}));
    const translated = data?.data?.translations?.[0]?.translatedText;
    if (translated) return { translated, error: null };
    const msg = data?.error?.message || (res.ok ? 'No translatedText in response' : `HTTP ${res.status}`);
    return { translated: null, error: msg };
  } catch (err) {
    return { translated: null, error: err?.message || String(err) };
  }
}

async function main() {
  const enPath = join(ASSETS, EN, STRINGS_FILE);
  const enObj = loadJson(enPath);
  const enKeys = new Set(Object.keys(enObj));

  const locales = readdirSync(ASSETS, { withFileTypes: true }).filter(
    (d) => d.isDirectory() && d.name !== EN && existsSync(join(ASSETS, d.name, STRINGS_FILE))
  );

  const hasKey = Boolean(process.env.GOOGLE_TRANSLATE_API_KEY);
  console.error('[sync-strings] GOOGLE_TRANSLATE_API_KEY:', hasKey ? 'set' : 'NOT SET (using English fallback)');

  let changed = false;
  for (const { name } of locales) {
    const localePath = join(ASSETS, name, STRINGS_FILE);
    const obj = loadJson(localePath);
    const langCode = LOCALE_TO_LANG[name] || name;
    let updated = false;
    let translatedCount = 0;
    let fallbackCount = 0;
    let firstError = null;

    for (const key of enKeys) {
      if (obj[key] !== undefined) continue;

      const enValue = enObj[key];
      let value = enValue;

      const result = await translate(enValue, langCode);
      if (result.translated != null) {
        value = result.translated;
        translatedCount++;
      } else {
        fallbackCount++;
        if (result.error && !firstError) firstError = result.error;
      }

      obj[key] = value;
      updated = true;
      await sleep(REQUEST_DELAY_MS);
    }

    if (updated) {
      writeFileSync(localePath, JSON.stringify(obj, null, 2) + '\n', 'utf8');
      changed = true;
      console.error(`[sync-strings] ${name}: ${translatedCount} translated, ${fallbackCount} fallback to English${firstError ? `; first error: ${firstError}` : ''}`);
    }
  }

  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
