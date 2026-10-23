#!/usr/bin/env node
// docs/api/index.html is the single source for the API guide: pom.xml packages
// it as the Maven documentation jar. The docs site renders it too, so this
// script derives docs/api.md from it before every site build. It understands
// only the tags the guide uses and fails on anything else, so an unsupported
// edit breaks the build instead of silently dropping content.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const source = path.join(root, 'docs/api/index.html')
const target = path.join(root, 'docs/api.md')

const html = fs.readFileSync(source, 'utf8')
const body = html
  .replace(/<!doctype[^>]*>/i, '')
  .replace(/<\/?html[^>]*>/gi, '')
  .replace(/<meta[^>]*>/gi, '')
  .replace(/<title>[\s\S]*?<\/title>/i, '')
  .replace(/<style>[\s\S]*?<\/style>/i, '')

const entities = { '&lt;': '<', '&gt;': '>', '&amp;': '&', '&quot;': '"', '&#39;': "'" }
const decode = (text) => text.replace(/&(lt|gt|amp|quot|#39);/g, (m) => entities[m])

const allowed = new Set(['h1', 'h2', 'p', 'pre', 'code', 'a'])
for (const [, tag] of body.matchAll(/<\/?([a-zA-Z][a-zA-Z0-9]*)/g)) {
  if (!allowed.has(tag.toLowerCase())) {
    throw new Error(`docs/api/index.html uses <${tag}>, which generate-api-page.mjs does not convert`)
  }
}

const inline = (text) =>
  decode(
    text
      .replace(/<code>([\s\S]*?)<\/code>/g, (_, c) => `\u0000${c}\u0000`)
      .replace(/<a href="([^"]+)">([\s\S]*?)<\/a>/g, (_, href, label) => `[${label}](${href})`)
  )
    .replace(/\u0000([\s\S]*?)\u0000/g, (_, c) => `\`${c}\``)

const blocks = []
const pattern = /<(h1|h2|p|pre)>([\s\S]*?)<\/\1>/g
let consumed = 0
for (const match of body.matchAll(pattern)) {
  if (body.slice(consumed, match.index).trim()) {
    throw new Error('docs/api/index.html has content outside h1, h2, p and pre blocks')
  }
  consumed = match.index + match[0].length
  const [, tag, content] = match
  if (tag === 'h1') blocks.push(`# ${inline(content)}`)
  else if (tag === 'h2') blocks.push(`## ${inline(content)}`)
  else if (tag === 'p') blocks.push(inline(content))
  else blocks.push('```\n' + decode(content) + '\n```')
}
if (body.slice(consumed).trim()) {
  throw new Error('docs/api/index.html has trailing content outside known blocks')
}

// Highlight the code blocks by language: the guide's three <pre> blocks follow
// its Kotlin, Java and Swift headings.
const langs = { Kotlin: 'kotlin', Java: 'java', Swift: 'swift' }
let lang = ''
const out = blocks.map((block) => {
  const heading = block.match(/^## (.*)$/)
  if (heading) lang = langs[heading[1]] || ''
  return block.startsWith('```\n') && lang ? block.replace('```\n', '```' + lang + '\n') : block
})

fs.writeFileSync(
  target,
  `<!-- Generated from docs/api/index.html by scripts/generate-api-page.mjs. Do not edit. -->\n\n${out.join('\n\n')}\n`
)
console.log(`generated docs/api.md from docs/api/index.html (${out.length} blocks)`)
