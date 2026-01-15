/**
 * 主题配置 - 深色模式
 * 狼主题风格：深色系，主色调 #0a3e1e
 */
const theme = {
  // 主色调（深森林绿）
  primaryColor: '#0a3e1e',
  
  // 辅色（与主色相同）
  secondaryColor: '#0a3e1e',
  
  // 次要色
  minorColor: '#189649',
  
  // 警告色
  warningColor: '#3e0a0b',
  
  // 渐变色（基于主题色）
  gradientStart: '#0a3e1e',
  gradientEnd: '#1a5f3e',
  
  // 深色模式 - 文字颜色
  textPrimary: '#ffffff',
  textSecondary: 'rgba(255, 255, 255, 0.85)',
  textTertiary: 'rgba(255, 255, 255, 0.65)',
  textDisabled: 'rgba(255, 255, 255, 0.4)',
  
  // 深色模式 - 背景色
  bgPrimary: '#0d0d0d',        // 主背景（深黑）
  bgSecondary: '#1a1a1a',       // 次背景（深灰）
  bgTertiary: '#262626',        // 三级背景
  bgCard: '#1f1f1f',            // 卡片背景
  bgHover: 'rgba(10, 62, 30, 0.2)',  // 悬停背景
  
  // 深色模式 - 边框色
  borderColor: 'rgba(255, 255, 255, 0.1)',
  borderColorHover: 'rgba(10, 62, 30, 0.5)',
  
  // 圆角（统一为 3-5px，避免过于圆润）
  borderRadiusSmall: '6rpx',   // 3px - 小圆角（按钮、标签等）
  borderRadius: '8rpx',         // 4px - 标准圆角（卡片、输入框等）
  borderRadiusLarge: '10rpx',   // 5px - 大圆角（特殊场景）
  
  // 阴影（深色模式）
  shadowSmall: '0 2rpx 8rpx rgba(0, 0, 0, 0.3)',
  shadowMedium: '0 4rpx 16rpx rgba(0, 0, 0, 0.4)',
  shadowLarge: '0 8rpx 24rpx rgba(0, 0, 0, 0.5)'
}

module.exports = theme

