#!/usr/bin/env node
/*
 * Generates the icon resources of the Eclipse plug-in from a checkout of the
 * Atom Material Icons icon generator (https://github.com/AtomMaterialUI/iconGenerator).
 *
 * For every file and folder association it
 *   - renders the referenced SVG to <name>.png (16x16) and <name>@2x.png (32x32),
 *   - writes a compact association XML that the plug-in reads at runtime.
 *
 * With --java it also renders the Java type icons (class, interface, enum, ...) from a
 * checkout of the Atom Material Icons plug-in (https://github.com/mallowigi/a-file-icon-idea).
 *
 * Usage: node generate.mjs <path-to-iconGenerator-checkout> [--java <path-to-a-file-icon-idea-checkout>] [<plugin-dir>]
 */
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { Resvg } from '@resvg/resvg-js';
import { XMLParser } from 'fast-xml-parser';

const USAGE = 'Usage: node generate.mjs <path-to-iconGenerator-checkout> [--java <path-to-a-file-icon-idea-checkout>] [<plugin-dir>]';

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
let javaSource;
const javaIndex = args.indexOf('--java');
if (javaIndex >= 0) {
  javaSource = args[javaIndex + 1];
  args.splice(javaIndex, 2);
}
const source = args[0];
const pluginDir = resolve(args[1] ?? join(here, '../../bundles/io.github.socra167.atommaterialicons'));

if (!source || !existsSync(join(source, 'icon_associations.xml'))) {
  console.error(USAGE);
  process.exit(1);
}

/** Java type icons, by the name the plug-in uses, taken from the glyphs of the IntelliJ plug-in. */
const JAVA_GLYPHS = 'common/src/main/resources/glyphs/nodes';
const JAVA_ICONS = [
  'class',
  'abstractClass',
  'interface',
  'enum',
  'record',
  'annotationtype',
  'exceptionClass',
  'abstractException',
  'test',
];

/** Upstream revision recorded in the generated XML, when the source is a git checkout. */
let revision = 'unknown';
try {
  revision = execFileSync('git', ['-C', source, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim();
} catch {
  // not a git checkout
}

const SIZES = [
  { suffix: '', size: 16 },
  { suffix: '@2x', size: 32 },
];

/** Folder icon used for folders that match no association. */
const DEFAULT_FOLDER_ICON = 'folder';

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '',
  processEntities: true,
  isArray: (name) => name === 'regex',
});

function readAssociations(file) {
  const xml = parser.parse(readFileSync(join(source, file), 'utf8'));
  return xml.associations.associations.regex ?? [];
}

/** Indexes the SVGs of a directory by lower-case base name, so that slightly mismatching references still resolve. */
function indexSvgs(dir) {
  const index = new Map();
  for (const file of readdirSync(dir)) {
    if (file.endsWith('.svg')) {
      index.set(file.slice(0, -4).toLowerCase(), file.slice(0, -4));
    }
  }
  return index;
}

function escapeXml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function render(svgFile, outDir, name) {
  let svg = readFileSync(svgFile, 'utf8');
  // Some upstream SVGs omit the namespace, which strict parsers reject.
  if (!/<svg[^>]*\sxmlns=/.test(svg)) {
    svg = svg.replace(/<svg\b/, '<svg xmlns="http://www.w3.org/2000/svg"');
  }
  for (const { suffix, size } of SIZES) {
    const png = new Resvg(svg, { fitTo: { mode: 'width', value: size } }).render().asPng();
    writeFileSync(join(outDir, `${name}${suffix}.png`), png);
  }
}

function generate(kind, xmlFile, svgDir) {
  const svgIndex = indexSvgs(svgDir);
  const outDir = join(pluginDir, 'icons', kind);
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });

  const rendered = new Set();
  const entries = [];
  const missing = [];

  const failed = new Set();
  const ensureIcon = (iconName) => {
    if (rendered.has(iconName)) {
      return true;
    }
    if (failed.has(iconName)) {
      return false;
    }
    try {
      render(join(svgDir, `${iconName}.svg`), outDir, iconName);
      rendered.add(iconName);
      return true;
    } catch (e) {
      failed.add(iconName);
      console.warn(`  could not render ${kind}/${iconName}.svg: ${e.message}`);
      return false;
    }
  };

  for (const assoc of readAssociations(xmlFile)) {
    if (!assoc.pattern || !assoc.icon) {
      continue;
    }
    const iconName = svgIndex.get(basename(assoc.icon, '.svg').toLowerCase());
    if (!iconName) {
      missing.push(`${assoc.name} (${assoc.icon})`);
      continue;
    }
    if (!ensureIcon(iconName)) {
      continue;
    }

    const attrs = [
      `name="${escapeXml(assoc.name)}"`,
      `priority="${Number.parseInt(assoc.priority ?? '0', 10) || 0}"`,
      `pattern="${escapeXml(assoc.pattern)}"`,
      `icon="${escapeXml(iconName)}"`,
    ];
    if (assoc.iconPack && assoc.iconPack !== 'DEFAULT') {
      attrs.push(`pack="${escapeXml(assoc.iconPack)}"`);
    }
    entries.push(`  <association ${attrs.join(' ')}/>`);
  }

  let header = `<associations kind="${kind}"`;
  if (kind === 'folders') {
    if (!ensureIcon(DEFAULT_FOLDER_ICON)) {
      throw new Error('The default folder icon could not be rendered');
    }
    header += ` default="${DEFAULT_FOLDER_ICON}"`;
  }

  const xml = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<!-- Generated by tools/icon-generator from the Atom Material Icons icon generator. Do not edit. -->',
    `<!-- Source: https://github.com/AtomMaterialUI/iconGenerator revision ${revision} -->`,
    '<!-- Icons and associations: MIT License, Copyright (c) 2015-2025 Elior "Mallowigi" Boukhobza -->',
    `${header}>`,
    ...entries,
    '</associations>',
    '',
  ].join('\n');
  mkdirSync(join(pluginDir, 'associations'), { recursive: true });
  writeFileSync(join(pluginDir, 'associations', `${kind}.xml`), xml);

  console.log(`${kind}: ${entries.length} associations, ${rendered.size} icons`);
  for (const m of missing) {
    console.warn(`  skipped, icon not found: ${m}`);
  }
}

function generateJavaIcons() {
  const svgDir = join(javaSource, JAVA_GLYPHS);
  if (!existsSync(svgDir)) {
    console.error(`No Java glyphs in ${svgDir}`);
    console.error(USAGE);
    process.exit(1);
  }
  const outDir = join(pluginDir, 'icons', 'java');
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });
  for (const name of JAVA_ICONS) {
    render(join(svgDir, `${name}.svg`), outDir, name);
  }
  console.log(`java: ${JAVA_ICONS.length} icons`);
}

generate('files', 'icon_associations.xml', join(source, 'assets/icons/files'));
generate('folders', 'folder_associations.xml', join(source, 'assets/icons/folders'));
if (javaSource) {
  generateJavaIcons();
}
