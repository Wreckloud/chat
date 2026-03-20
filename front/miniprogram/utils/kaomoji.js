const COMMON_KAOMOJI_LIST = [
  '(^_^)',
  '(＾▽＾)',
  '(￣▽￣)',
  '(≧▽≦)',
  '(*^_^*)',
  '(^-^*)/',
  '(=ﾟωﾟ)ﾉ',
  '(｡･∀･)ﾉﾞ',
  'ヽ(✿ﾟ▽ﾟ)ノ',
  'o(*￣▽￣*)o',
  '(～￣▽￣)～',
  '(´▽`ʃ♡ƪ)',
  '(๑•̀ㅂ•́)و✧',
  '(ง •̀_•́)ง',
  '( •̀ ω •́ )✧',
  '(｀・ω・´)',
  '٩(ˊᗜˋ*)و',
  '٩(๑❛ᴗ❛๑)۶',
  '(￣^￣)ゞ',
  '(｡•̀ᴗ-)✧',
  '(^人^)',
  'm(_ _)m',
  '(_ _)',
  'orz',
  '(¬_¬)',
  '(￢_￢)',
  '(눈_눈)',
  '(￣ー￣)',
  '(⊙_⊙)',
  'Σ(ﾟдﾟ;)',
  '(°ー°〃)',
  '(；′⌒`)',
  '(´･_･`)',
  '(｡•́︿•̀｡)',
  '(╥﹏╥)',
  '(つД`)ノ',
  '(T_T)',
  '(ಥ_ಥ)',
  '(ノへ￣、)',
  '(；д；)',
  '(｀へ´)',
  '(╬ಠ益ಠ)',
  '(￣o￣) . z Z',
  '(￣﹃￣)',
  '(๑´ڡ`๑)',
  '(●ˇ∀ˇ●)',
  '(￣︶￣)',
  '(=^･ω･^=)',
  '(ฅ´ω`ฅ)',
  'ฅ(•ㅅ•❀)ฅ',
  'ʕ•ᴥ•ʔ',
  '(づ￣ 3￣)づ',
  '(づ｡◕‿‿◕｡)づ',
  '(っ´Ι`)っ',
  '(๑•́ ₃ •̀๑)',
  'QAQ',
  'XD',
  '>_<'
]

function appendKaomojiWithSpace(source, kaomoji) {
  const token = String(kaomoji || '').trim()
  if (!token) {
    return String(source || '')
  }
  const origin = String(source || '')
  const separator = origin && !/\s$/.test(origin) ? ' ' : ''
  return `${origin}${separator}${token} `
}

module.exports = {
  COMMON_KAOMOJI_LIST,
  appendKaomojiWithSpace
}
