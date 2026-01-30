#!/usr/bin/env node
import { readFileSync, readdirSync, existsSync } from 'fs';
import { join } from 'path';

const ASSETS = 'app/src/main/assets';
const EN = 'en';
const STRINGS_FILE = 'strings.json';

function getKeys(filePath) {
    const raw = readFileSync(filePath, 'utf8');
    const obj = JSON.parse(raw);
    return new Set(Object.keys(obj));
}

const enPath = join(ASSETS, EN, STRINGS_FILE);
const enKeys = getKeys(enPath);
const locales = readdirSync(ASSETS, { withFileTypes: true }).filter(
  (d) => d.isDirectory() && d.name !== EN && existsSync(join(ASSETS, d.name, STRINGS_FILE))
);

let failed = false;
for (const { name } of locales) {
    const localePath = join(ASSETS, name, STRINGS_FILE);
    const keys = getKeys(localePath);
    const missing = [...enKeys].filter((k) =>!keys.has(k));
    //extras keys are not a problem for now, but this is how i would check for them:
    //const extra = [...keys].filter((k) => !enKeys.has(k));
    if(missing.length) {
        failed = true;
        if(missing.length) {
            console.error(`[${name}] Missing keys: ${missing.join(', ')}`);
        }
    }
}

process.exit(failed? 1 : 0);