/**
 * 兼容别名：保留 media-upload 引用，实际实现落在 oss.js，避免开发者工具缓存导致模块丢失。
 */
module.exports = require('./oss')
