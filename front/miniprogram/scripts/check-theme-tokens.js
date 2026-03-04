/**
 * 主题 token 一致性校验：
 * 1. blue/olive 主题变量集合必须一致
 * 2. 页面中使用到的变量必须在 app.wxss 中定义
 */
const fs = require('fs')
const path = require('path')

const APP_WXSS_PATH = path.join(__dirname, '..', 'app.wxss')
const PAGES_DIR = path.join(__dirname, '..', 'pages')
const THEMES = ['theme-retro-blue', 'theme-retro-olive']

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function extractClassBlock(source, className) {
  const marker = `.${className}`
  const start = source.indexOf(marker)
  if (start < 0) {
    throw new Error(`Cannot find class block: ${className}`)
  }

  const braceStart = source.indexOf('{', start)
  if (braceStart < 0) {
    throw new Error(`Cannot find opening brace for: ${className}`)
  }

  let depth = 0
  // 使用括号深度切出主题 class 的完整内容，避免正则跨块误匹配
  for (let i = braceStart; i < source.length; i++) {
    const ch = source[i]
    if (ch === '{') depth += 1
    if (ch === '}') depth -= 1
    if (depth === 0) {
      return source.slice(braceStart + 1, i)
    }
  }

  throw new Error(`Cannot find closing brace for: ${className}`)
}

function extractTokenNames(block) {
  const tokens = new Set()
  for (const match of block.matchAll(/--([a-z0-9-]+)\s*:/gi)) {
    tokens.add(match[1])
  }
  return tokens
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
      used.add(match[1])
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
  const blueBlock = extractClassBlock(appWxss, THEMES[0])
  const oliveBlock = extractClassBlock(appWxss, THEMES[1])

  const blueTokens = extractTokenNames(blueBlock)
  const oliveTokens = extractTokenNames(oliveBlock)
  const usedTokens = extractUsedTokens(listPageWxssFiles())

  const missingInOlive = difference(blueTokens, oliveTokens)
  const missingInBlue = difference(oliveTokens, blueTokens)
  const missingInThemes = difference(usedTokens, new Set([...blueTokens, ...oliveTokens]))

  const errors = []
  if (missingInOlive.length) {
    errors.push(`[theme-retro-olive] missing tokens:\n${formatNames(missingInOlive)}`)
  }
  if (missingInBlue.length) {
    errors.push(`[theme-retro-blue] missing tokens:\n${formatNames(missingInBlue)}`)
  }
  if (missingInThemes.length) {
    errors.push(`[app.wxss] missing used tokens:\n${formatNames(missingInThemes)}`)
  }

  if (errors.length) {
    console.error('Theme token check failed:\n')
    console.error(errors.join('\n\n'))
    process.exit(1)
  }

  console.log(
    `Theme token check passed. blue=${blueTokens.size}, olive=${oliveTokens.size}, used=${usedTokens.size}`
  )
}

main()
