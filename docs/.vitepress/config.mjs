import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineDocsConfig } from '@tavric-labs/docs-theme/config'

const docsDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoUrl = 'https://github.com/tavric-labs/mcp-mobile'

// Serve every page as a directory (/android/) so the static host needs no
// extension-less URL rewriting. ARCHITECTURE.md becomes /architecture/ and
// RELEASE_NOTES.md becomes /release-notes/; the source files keep their names.
const rewrites = Object.fromEntries(
  fs
    .readdirSync(docsDir)
    .filter((f) => f.endsWith('.md') && f !== 'index.md')
    .map((f) => [f, `${f.slice(0, -3).toLowerCase().replace(/_/g, '-')}/index.md`])
)

const nav = [
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
]

const sidebar = [
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
]

export default defineDocsConfig({
  name: 'MCP Mobile',
  repo: 'tavric-labs/mcp-mobile',
  siteUrl: 'https://mcp-mobile.tavric-labs.net',
  description:
    'Small native adapters around the official MCP Kotlin and Swift SDKs, for Android and iOS apps that need an MCP client.',
  nav,
  sidebar,
  extra: { rewrites }
})
