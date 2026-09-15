import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const config = readFileSync(resolve('.vitepress/config.mts'), 'utf8')
const routePattern = /\blink:\s*['"]([^'"]+)['"]/g
const routes = new Set()

for (const match of config.matchAll(routePattern)) {
  if (match[1].startsWith('/')) routes.add(match[1])
}

const missing = []

for (const route of routes) {
  const pathname = route.split(/[?#]/, 1)[0]
  const relativePath = pathname === '/'
    ? 'index.md'
    : pathname.endsWith('/')
      ? `${pathname.slice(1)}index.md`
      : `${pathname.slice(1)}.md`

  if (!existsSync(resolve(relativePath))) {
    missing.push({ route, relativePath })
  }
}

if (missing.length > 0) {
  console.error('Navigation contains routes without Markdown pages:')
  for (const item of missing) {
    console.error(`- ${item.route} -> ${item.relativePath}`)
  }
  process.exitCode = 1
} else {
  console.log(`Validated ${routes.size} internal navigation routes.`)
}

