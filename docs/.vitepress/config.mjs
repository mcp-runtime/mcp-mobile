import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { withMermaid } from 'vitepress-plugin-mermaid'

const docsDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoDir = path.resolve(docsDir, '..')
const repoUrl = 'https://github.com/tavric-labs/mcp-mobile'
const siteUrl = 'https://mcp-mobile.tavric-labs.net'

// Serve every page as a directory (/android/) so the static host needs no
// extension-less URL rewriting. ARCHITECTURE.md becomes /architecture/ and
// RELEASE_NOTES.md becomes /release-notes/; the source files keep their names.
const rewrites = Object.fromEntries(
  fs
    .readdirSync(docsDir)
    .filter((f) => f.endsWith('.md') && f !== 'index.md')
    .map((f) => [f, `${f.slice(0, -3).toLowerCase().replace(/_/g, '-')}/index.md`])
)

// Links that leave docs/ (README, PLAN, sources, ...) point at repo files;
// send them to GitHub instead of failing the dead-link check.
function repoLinks(md) {
  md.core.ruler.push('repo-links', (state) => {
    const rel = state.env.relativePath
    if (!rel) return
    const from = path.dirname(path.join(docsDir, rel))
    const walk = (tokens) => {
      for (const t of tokens) {
        if (t.type === 'link_open') {
          const href = t.attrGet('href')
          if (href && !/^([a-z]+:|#|\/)/i.test(href)) {
            const [file, hash = ''] = href.split('#')
            const abs = path.resolve(from, file)
            if (abs.startsWith(repoDir + path.sep) && !abs.startsWith(docsDir + path.sep)) {
              const isDir = fs.existsSync(abs) && fs.statSync(abs).isDirectory()
              const target = path.relative(repoDir, abs).split(path.sep).join('/')
              t.attrSet('href', `${repoUrl}/${isDir ? 'tree' : 'blob'}/main/${target}${hash ? '#' + hash : ''}`)
            }
          }
        }
        if (t.children) walk(t.children)
      }
    }
    walk(state.tokens)
  })
}

const description =
  'Small native adapters around the official MCP Kotlin and Swift SDKs, for Android and iOS apps that need an MCP client.'

export default withMermaid({
  title: 'MCP Mobile',
  titleTemplate: ':title | MCP Mobile',
  description,
  lang: 'en-US',
  cleanUrls: true,
  lastUpdated: true,
  rewrites,
  sitemap: { hostname: `${siteUrl}/` },
  markdown: { config: repoLinks },
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    ['meta', { name: 'theme-color', content: '#6d4aea' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: 'MCP Mobile' }],
    ['meta', { property: 'og:title', content: 'MCP Mobile documentation' }],
    ['meta', { property: 'og:description', content: description }],
    ['meta', { property: 'og:image', content: `${siteUrl}/logo.svg` }],
    ['meta', { name: 'twitter:card', content: 'summary' }]
  ],
  mermaid: {
    theme: 'neutral',
    fontFamily: 'ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    flowchart: { htmlLabels: true, curve: 'basis', padding: 14, wrappingWidth: 320, useMaxWidth: true },
    sequence: { mirrorActors: false, useMaxWidth: true }
  },
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'MCP Mobile',
    nav: [
      { text: 'Get started', items: [
        { text: 'Overview', link: '/overview/' },
        { text: 'Android', link: '/android/' },
        { text: 'iOS', link: '/ios/' } ] },
      { text: 'Reference', items: [
        { text: 'API guide', link: '/api/' },
        { text: 'Architecture', link: '/architecture/' } ] },
      { text: 'Project', items: [
        { text: 'Verification', link: '/verification/' },
        { text: 'Publishing', link: '/publishing/' },
        { text: 'Release notes', link: '/release-notes/' } ] },
      { text: 'Releases', link: `${repoUrl}/releases` }
    ],
    sidebar: [
      { text: 'Get started', items: [
        { text: 'Overview', link: '/overview/' },
        { text: 'Android', link: '/android/' },
        { text: 'iOS', link: '/ios/' } ] },
      { text: 'Reference', items: [
        { text: 'API guide', link: '/api/' },
        { text: 'Architecture', link: '/architecture/' } ] },
      { text: 'Project', items: [
        { text: 'Preview verification', link: '/verification/' },
        { text: 'Publishing SDKs', link: '/publishing/' },
        { text: 'Release notes', link: '/release-notes/' } ] }
    ],
    socialLinks: [{ icon: 'github', link: repoUrl }],
    editLink: { pattern: `${repoUrl}/edit/main/docs/:path`, text: 'Edit this page on GitHub' },
    lastUpdated: { text: 'Last updated', formatOptions: { dateStyle: 'medium' } },
    outline: { level: [2, 3], label: 'On this page' },
    docFooter: { prev: 'Previous', next: 'Next' },
    search: { provider: 'local', options: { detailedView: true } },
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © Tavric Labs'
    }
  }
})
