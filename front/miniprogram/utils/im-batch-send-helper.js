function showBatchFailureToast(failCount, totalCount) {
  if (!Number.isFinite(failCount) || failCount <= 0) {
    return
  }
  if (!Number.isFinite(totalCount) || totalCount <= 1) {
    wx.showToast({
      title: '发送失败',
      icon: 'none'
    })
    return
  }
  wx.showToast({
    title: `发送失败 ${failCount}/${totalCount}`,
    icon: 'none'
  })
}

async function sendUploadedMediaBatch(page, files, options) {
  const upload = options && typeof options.upload === 'function' ? options.upload : null
  const buildPayload = options && typeof options.buildPayload === 'function' ? options.buildPayload : null
  if (!upload || !buildPayload) {
    return { successCount: 0, failCount: 0, totalCount: 0 }
  }

  const safeFiles = Array.isArray(files) ? files.filter(Boolean) : []
  if (safeFiles.length === 0) {
    return { successCount: 0, failCount: 0, totalCount: 0 }
  }

  let successCount = 0
  let failCount = 0
  for (let index = 0; index < safeFiles.length; index++) {
    const file = safeFiles[index]
    try {
      const media = await upload(file)
      await page.sendWsMessageWithAck(buildPayload(media))
      successCount += 1
    } catch (error) {
      if (page.pageUnloaded) {
        break
      }
      failCount += 1
    }
  }
  return {
    successCount,
    failCount,
    totalCount: safeFiles.length
  }
}

module.exports = {
  showBatchFailureToast,
  sendUploadedMediaBatch
}

