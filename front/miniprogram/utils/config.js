/**
 * WolfChat 小程序配置文件
 * @author Wreckloud
 * @date 2024-12-18
 */

const config = {
  // 后端API基础地址
  // 开发环境
  baseUrl: 'http://localhost:8080/api',
  
  // 生产环境（上线时修改）
  // baseUrl: 'https://api.wolfchat.com/api',
  
  // 请求超时时间（毫秒）
  timeout: 10000,
  
  // Token存储key
  tokenKey: 'wolfchat_token',
  
  // 用户信息存储key
  userKey: 'wolfchat_user',
  
  // 刷新Token的时间阈值（秒）
  refreshTokenThreshold: 7 * 24 * 60 * 60, // 7天
  
  // 狼主题配色
  theme: {
    primary: '#1989fa',      // 主色调（蓝色）
    secondary: '#07c160',    // 次要色（绿色）
    danger: '#ee0a24',       // 危险色（红色）
    warning: '#ff976a',      // 警告色（橙色）
    wolf: '#8B4513',         // 狼色（棕色）
    wolfDark: '#5D3A1A',     // 深狼色
    background: '#f7f8fa',   // 背景色
  },
  
  // 群组角色
  groupRole: {
    OWNER: '群主',
    ADMIN: '管理员',
    MEMBER: '成员'
  },
  
  // 图片上传配置
  upload: {
    maxSize: 5 * 1024 * 1024, // 5MB
    allowTypes: ['image/jpeg', 'image/png', 'image/jpg']
  }
};

module.exports = config;

