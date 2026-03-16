/**
 * 主题 token 一致性校验：
 * 1. blue/olive 主题变量集合必须一致
 * 2. 页面中使用到的变量必须在 app.wxss 中定义
 */
const fs = require('fs')
const path = require('path')

const APP_WXSS_PATH = path.join(__dirname, '..', 'app.wxss')
const PAGES_DIR = path.join(__dirname, '..', 'pages')
const THEMES = ['theme-retro-blue', 'theme-retro-olive', 'theme-retro-amber', 'theme-retro-steel', 'theme-retro-wine']

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function extractTokenNames(block) {
  const tokens = new Set()
  for (const match of block.matchAll(/--([a-z0-9-]+)\s*:/gi)) {
    tokens.add(match[1])
  }
  return tokens
}

function extractThemeTokenMap(source, themeClasses) {
  const tokenMap = {}
  themeClasses.forEach(themeClass => {
    tokenMap[themeClass] = new Set()
  })

  // 扁平匹配每个 css 块，再按 selector 是否命中主题 class 聚合 token。
  const blockRegex = /([^{}]+)\{([^{}]*)\}/gms
  let match = blockRegex.exec(source)
  while (match) {
    const selectors = match[1] || ''
    const block = match[2] || ''
    const tokens = extractTokenNames(block)
    if (tokens.size > 0) {
      themeClasses.forEach(themeClass => {
        if (selectors.includes(`.${themeClass}`)) {
          tokens.forEach(token => tokenMap[themeClass].add(token))
        }
      })
    }
    match = blockRegex.exec(source)
  }

  return tokenMap
}

function listPageWxssFiles() {
  const result = []
  const entries = fs.readdirSync(PAGES_DIR, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const filePath = path.join(PAGES_DIR, entry.name, `${entry.name}.wxss`)
    if (fs.existsSync(filePath)) {
      result.push(filePath)
    }
  }
  return result
}

function extractUsedTokens(files) {
  const used = new Set()
  for (const filePath of files) {
    const source = readText(filePath)
    for (const match of source.matchAll(/var\(--([a-z0-9-]+)/gi)) {
      const token = match[1]
      if (token.startsWith('retro-')) {
        used.add(token)
      }
    }
  }
  return used
}

function difference(sourceSet, targetSet) {
  return [...sourceSet].filter(name => !targetSet.has(name)).sort()
}

function formatNames(names) {
  return names.map(name => `  - ${name}`).join('\n')
}

function main() {
  const appWxss = readText(APP_WXSS_PATH)
  const themeTokenMap = extractThemeTokenMap(appWxss, THEMES)
  const baselineTokens = themeTokenMap[THEMES[0]]
  const usedTokens = extractUsedTokens(listPageWxssFiles())

  const errors = []
  THEMES.forEach(themeClass => {
    const tokens = themeTokenMap[themeClass]
    const missing = difference(baselineTokens, tokens)
    const extra = difference(tokens, baselineTokens)
    if (missing.length) {
      errors.push(`[${themeClass}] missing tokens:\n${formatNames(missing)}`)
    }
    if (extra.length) {
      errors.push(`[${themeClass}] extra tokens:\n${formatNames(extra)}`)
    }
  })
  const missingInThemes = difference(usedTokens, baselineTokens)

  if (missingInThemes.length) {
    errors.push(`[app.wxss] missing used tokens:\n${formatNames(missingInThemes)}`)
  }

  if (errors.length) {
    console.error('Theme token check failed:\n')
    console.error(errors.join('\n\n'))
    process.exit(1)
  }

  console.log(
    `Theme token check passed. themeCount=${THEMES.length}, tokenCount=${baselineTokens.size}, used=${usedTokens.size}`
  )
}

main()
