/**
 * 统一 UI 提示
 */
function toastError(error, fallback = '操作失败') {
  let message = fallback
  if (error) {
    if (typeof error === 'string') {
      message = error
    } else if (error.message) {
      message = error.message
    }
  }
  wx.showToast({
    title: message,
    icon: 'none'
  })
}

function toastSuccess(message = '操作成功') {
  wx.showToast({
    title: message,
    icon: 'success'
  })
}

module.exports = {
  toastError,
  toastSuccess
}
