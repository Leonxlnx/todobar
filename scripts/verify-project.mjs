import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const failures = []

function fail(message) {
  failures.push(message)
}

function read(relativePath) {
  return readFileSync(join(root, relativePath), 'utf8')
}

function readJson(relativePath) {
  return JSON.parse(read(relativePath))
}

function matchVersion(name, text, pattern) {
  const match = text.match(pattern)

  if (!match) {
    fail(`${name}: version not found`)
    return undefined
  }

  return match[1]
}

function verifyVersions() {
  const packageJson = readJson('package.json')
  const packageLock = readJson('package-lock.json')
  const cargoToml = read('src-tauri/Cargo.toml')
  const cargoLock = read('src-tauri/Cargo.lock')
  const tauriConfig = readJson('src-tauri/tauri.conf.json')
  const readme = read('README.md')
  const changelog = read('CHANGELOG.md')
  const expected = packageJson.version
  const versions = {
    'package-lock.json': packageLock.version,
    'package-lock root package': packageLock.packages?.['']?.version,
    'src-tauri/Cargo.toml': matchVersion(
      'src-tauri/Cargo.toml',
      cargoToml,
      /^version\s*=\s*"([^"]+)"/m,
    ),
    'src-tauri/Cargo.lock': matchVersion(
      'src-tauri/Cargo.lock',
      cargoLock,
      /\[\[package\]\]\s+name = "todobar"\s+version = "([^"]+)"/m,
    ),
    'src-tauri/tauri.conf.json': tauriConfig.version,
    'README current stable release': matchVersion(
      'README.md',
      readme,
      /Current stable release: `v([^`]+)`\./,
    ),
  }

  for (const [name, version] of Object.entries(versions)) {
    if (version !== expected) {
      fail(`${name}: expected ${expected}, found ${version ?? 'missing'}`)
    }
  }

  if (!changelog.includes(`## ${expected}`)) {
    fail(`CHANGELOG.md: missing section for ${expected}`)
  }

  const releaseVerification = `docs/release-verification-v${expected}.md`

  if (!existsSync(join(root, releaseVerification))) {
    fail(`${releaseVerification}: missing release verification document`)
  }
}

function listMarkdownFiles(relativeDir) {
  const directory = join(root, relativeDir)
  const entries = readdirSync(directory, { withFileTypes: true })
  const files = []

  for (const entry of entries) {
    const relativePath = join(relativeDir, entry.name)

    if (entry.isDirectory()) {
      files.push(...listMarkdownFiles(relativePath))
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      files.push(relativePath)
    }
  }

  return files
}

function verifyMarkdownLinks() {
  const markdownFiles = [
    'README.md',
    'CHANGELOG.md',
    'CONTRIBUTING.md',
    'SECURITY.md',
  ]

  if (existsSync(join(root, 'docs'))) {
    markdownFiles.push(...listMarkdownFiles('docs'))
  }

  for (const relativePath of markdownFiles) {
    const text = read(relativePath)
    const linkPattern = /\[[^\]]+\]\(([^)]+)\)/g
    let match

    while ((match = linkPattern.exec(text))) {
      const rawTarget = match[1].trim()

      if (
        !rawTarget ||
        rawTarget.startsWith('#') ||
        rawTarget.startsWith('http://') ||
        rawTarget.startsWith('https://') ||
        rawTarget.startsWith('mailto:')
      ) {
        continue
      }

      const targetWithoutTitle = rawTarget.split(/\s+["']/)[0]
      const targetPath = decodeURIComponent(targetWithoutTitle.split('#')[0])

      if (!targetPath) {
        continue
      }

      const absoluteTarget = normalize(join(root, dirname(relativePath), targetPath))

      if (!absoluteTarget.startsWith(root) || !existsSync(absoluteTarget)) {
        fail(`${relativePath}: broken local link ${rawTarget}`)
        continue
      }

      if (targetPath.endsWith('/') && !statSync(absoluteTarget).isDirectory()) {
        fail(`${relativePath}: expected directory link ${rawTarget}`)
      }
    }
  }
}

function verifyScripts() {
  const packageJson = readJson('package.json')

  for (const script of [
    'verify',
    'build',
    'lint',
    'test:smoke',
    'test:native',
    'tauri:build',
  ]) {
    if (!packageJson.scripts?.[script]) {
      fail(`package.json: missing ${script} script`)
    }
  }
}

function verifyWorkflows() {
  const ci = read('.github/workflows/ci.yml')
  const release = read('.github/workflows/release.yml')

  for (const [name, text] of [
    ['ci.yml', ci],
    ['release.yml', release],
  ]) {
    if (!text.includes('actions/checkout@v6')) {
      fail(`${name}: expected actions/checkout@v6`)
    }

    if (!text.includes('actions/setup-node@v6')) {
      fail(`${name}: expected actions/setup-node@v6`)
    }
  }

  if (!ci.includes('npx playwright install chromium')) {
    fail('ci.yml: missing Playwright browser install')
  }

  if (!ci.includes('npm run test:smoke')) {
    fail('ci.yml: missing sidebar smoke test')
  }

  if (!ci.includes('npm run test:native')) {
    fail('ci.yml: missing native Tauri build smoke test')
  }

  for (const platform of ['Windows', 'macOS Apple Silicon', 'macOS Intel']) {
    if (!release.includes(`name: ${platform}`)) {
      fail(`release.yml: missing ${platform} build`)
    }
  }
}

function verifyNativeDesktopSurface() {
  const cargoToml = read('src-tauri/Cargo.toml')
  const nativeShell = read('src-tauri/src/lib.rs')
  const app = read('src/App.tsx')
  const readme = read('README.md')

  if (!cargoToml.includes('tray-icon')) {
    fail('src-tauri/Cargo.toml: missing tray-icon feature')
  }

  for (const token of [
    'TrayIconBuilder',
    'setup_global_shortcuts',
    '.global_shortcut()',
    '.on_shortcut(shortcut',
    'CommandOrControl+Shift+T',
    'CommandOrControl+Alt+T',
    'todobar-tray-toggle',
    'todobar-tray-settings',
    'show_menu_on_left_click(false)',
  ]) {
    if (!nativeShell.includes(token) && !app.includes(token)) {
      fail(`desktop tray control: missing ${token}`)
    }
  }

  if (!readme.includes('Native tray/menu-bar control')) {
    fail('README.md: missing tray/menu-bar feature documentation')
  }
}

function verifyProductVision() {
  const readme = read('README.md')
  const visionPath = 'docs/product-vision.md'
  const roadmap = read('docs/roadmap.md')

  if (!existsSync(join(root, visionPath))) {
    fail(`${visionPath}: missing product vision document`)
    return
  }

  const vision = read(visionPath)

  for (const phrase of [
    'Version 0 - Edge Planner',
    'Version 1 - Better Planner',
    'Version 2 - Assistant Layer',
    'Version 3 - MCP and Context Connectors',
    'Gmail inbox summaries',
    'Companion Manager App',
  ]) {
    if (!vision.includes(phrase) && !roadmap.includes(phrase)) {
      fail(`product roadmap: missing ${phrase}`)
    }
  }

  if (!readme.includes('Product vision')) {
    fail('README.md: missing Product vision link')
  }
}

verifyVersions()
verifyMarkdownLinks()
verifyScripts()
verifyWorkflows()
verifyNativeDesktopSurface()
verifyProductVision()

if (failures.length > 0) {
  console.error('Project verification failed:')
  for (const failure of failures) {
    console.error(`- ${failure}`)
  }
  process.exit(1)
}

console.log('Project verification passed.')
