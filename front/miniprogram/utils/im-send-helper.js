const imHelper = require('./im-helper')

const DEFAULT_MORE_ACTIONS = [
  { key: 'image', label: '多图', icon: '图', subLabel: '相册' },
  { key: 'video', label: '视频', icon: '视', subLabel: '相册' },
  { key: 'file', label: '文件', icon: '档', subLabel: '多选' },
  { key: 'link', label: '链接', icon: '链', subLabel: '发送' }
]

function buildBaseSendPayload(page, msgType, extra = {}) {
  const sendType = String(page && page.IM_SEND_TYPE ? page.IM_SEND_TYPE : 'SEND').toUpperCase()
  const payload = {
    type: sendType,
    msgType,
    ...extra
  }
  if (sendType === 'SEND') {
    payload.conversationId = Number(page.data.conversationId)
  }
  return payload
}

async function sendTextMessage(page, content, options = {}) {
  return page.sendWsMessageWithAck(
    buildBaseSendPayload(page, 'TEXT', { content }),
    options
  )
}

async function chooseImageFromAlbum(page, deps) {
  if (page.data.sending) {
    return
  }

  try {
    const chooseRes = await imHelper.chooseMedia({
      count: 9,
      mediaType: ['image'],
      sourceType: ['album']
    })
    const tempFiles = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles : []
    if (tempFiles.length === 0) {
      return
    }

    page.setData({ sending: true })
    for (let index = 0; index < tempFiles.length; index++) {
      try {
        const media = await deps.uploadImage(tempFiles[index])
        await page.sendWsMessageWithAck(
          buildBaseSendPayload(page, 'IMAGE', {
            mediaKey: media.mediaKey,
            mediaWidth: media.mediaWidth,
            mediaHeight: media.mediaHeight,
            mediaSize: media.mediaSize,
            mediaMimeType: media.mediaMimeType
          })
        )
      } catch (error) {
        if (page.pageUnloaded) {
          return
        }
      }
    }
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
  } finally {
    page.setData({ sending: false })
  }
}

async function chooseVideoFromAlbum(page, deps) {
  if (page.data.sending) {
    return
  }

  try {
    const chooseRes = await imHelper.chooseMedia({
      count: 1,
      mediaType: ['video'],
      sourceType: ['album']
    })
    const tempFile = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
    if (!tempFile || !tempFile.tempFilePath) {
      return
    }
    page.setData({ sending: true })
    const media = await deps.uploadVideo(tempFile)
    await page.sendWsMessageWithAck(
      buildBaseSendPayload(page, 'VIDEO', {
        mediaKey: media.mediaKey,
        mediaWidth: media.mediaWidth,
        mediaHeight: media.mediaHeight,
        mediaSize: media.mediaSize,
        mediaMimeType: media.mediaMimeType
      })
    )
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
  } finally {
    page.setData({ sending: false })
  }
}

async function chooseFileForShare(page, deps) {
  if (page.data.sending) {
    return
  }

  try {
    const chooseRes = await imHelper.chooseMessageFile({
      count: 9,
      type: 'file'
    })
    const selectedFiles = Array.isArray(chooseRes.tempFiles)
      ? chooseRes.tempFiles.filter(Boolean)
      : []
    if (selectedFiles.length === 0) {
      return
    }

    page.setData({ sending: true })
    for (let index = 0; index < selectedFiles.length; index++) {
      const file = selectedFiles[index]
      try {
        const media = await deps.uploadFile(file)
        await page.sendWsMessageWithAck(
          buildBaseSendPayload(page, 'FILE', {
            content: imHelper.normalizeFileNameForMessage(media.fileName),
            mediaKey: media.mediaKey,
            mediaSize: media.mediaSize,
            mediaMimeType: media.mediaMimeType
          })
        )
      } catch (error) {
        if (page.pageUnloaded) {
          return
        }
      }
    }
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
  } finally {
    page.setData({ sending: false })
  }
}

async function shareLinkAsText(page, deps) {
  const modalRes = await imHelper.showEditableModal({
    title: '分享链接',
    placeholderText: '请输入链接地址'
  })
  if (!modalRes.confirm) {
    return
  }

  const link = imHelper.normalizeSharedLink(modalRes.content)
  if (!link) {
    return
  }

  if (page.data.sending) {
    return
  }

  page.setData({ sending: true })
  try {
    await sendTextMessage(page, link)
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
  } finally {
    page.setData({ sending: false })
  }
}

function onTapLink(e) {
  const url = e && e.currentTarget && e.currentTarget.dataset
    ? String(e.currentTarget.dataset.url || '')
    : ''
  if (!url) {
    return
  }
  wx.setClipboardData({
    data: url
  })
}

function onTapVideo(page, e, toastError) {
  if (page.previewingVideo) {
    return
  }
  const dataset = e && e.currentTarget ? e.currentTarget.dataset || {} : {}
  const url = String(dataset.url || '')
  const posterUrl = String(dataset.posterUrl || '')
  if (!url) {
    toastError('视频地址无效')
    return
  }

  page.previewingVideo = true
  wx.previewMedia({
    current: 0,
    sources: [
      {
        url,
        type: 'video',
        poster: posterUrl || undefined
      }
    ],
    fail: (error) => {
      if (page.pageUnloaded) {
        return
      }
      toastError(error, '视频打开失败')
    },
    complete: () => {
      page.previewingVideo = false
    }
  })
}

function onTapFile(page, e, toastError) {
  const dataset = e && e.currentTarget ? e.currentTarget.dataset || {} : {}
  const url = String(dataset.url || '')
  if (!url) {
    toastError('文件地址无效')
    return
  }
  const fileName = String(dataset.name || '').trim() || 'file'

  wx.showLoading({ title: '打开中...', mask: true })
  imHelper.downloadTempFile(url)
    .then(downloadRes => {
      if (downloadRes.statusCode < 200 || downloadRes.statusCode >= 300 || !downloadRes.tempFilePath) {
        throw new Error(`文件下载失败(${downloadRes.statusCode || 0})`)
      }
      return imHelper.openDocument(downloadRes.tempFilePath, fileName)
    })
    .catch(error => {
      if (page.pageUnloaded) {
        return
      }
      toastError(error, '文件打开失败')
    })
    .finally(() => {
      wx.hideLoading()
    })
}

async function onMoreActionTap(page, e, handlers) {
  const key = e && e.currentTarget && e.currentTarget.dataset
    ? String(e.currentTarget.dataset.key || '')
    : ''
  if (!key) {
    return
  }
  page.setMorePanelVisible(false)

  if (key === 'image') {
    await handlers.image()
    return
  }
  if (key === 'video') {
    await handlers.video()
    return
  }
  if (key === 'file') {
    await handlers.file()
    return
  }
  if (key === 'link') {
    await handlers.link()
  }
}

module.exports = {
  DEFAULT_MORE_ACTIONS,
  sendTextMessage,
  chooseImageFromAlbum,
  chooseVideoFromAlbum,
  chooseFileForShare,
  shareLinkAsText,
  onTapLink,
  onTapVideo,
  onTapFile,
  onMoreActionTap
}
