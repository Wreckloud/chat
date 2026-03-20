/**
 * 媒体上传封装（本地存储）
 */
const request = require('./request')

const IMAGE_MIME_MAPPING = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  gif: 'image/gif'
}
const VIDEO_MIME_MAPPING = {
  mp4: 'video/mp4',
  mov: 'video/quicktime',
  m4v: 'video/x-m4v',
  webm: 'video/webm'
}
const FILE_MIME_MAPPING = {
  pdf: 'application/pdf',
  txt: 'text/plain',
  md: 'text/markdown',
  csv: 'text/csv',
  json: 'application/json',
  zip: 'application/zip',
  rar: 'application/vnd.rar',
  '7z': 'application/x-7z-compressed',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
}
const FILE_EXTENSION_PATTERN = /^[a-z0-9]{1,20}$/

function toExposedUploadError(error, fallbackMessage) {
  if (error && (error.kind === 'business' || error.kind === 'network' || error.kind === 'http' || error.code)) {
    return error
  }
  const message = error && typeof error.message === 'string' && error.message.trim()
    ? error.message.trim()
    : fallbackMessage
  const exposedError = new Error(message)
  exposedError.expose = true
  return exposedError
}

function getPathExtension(filePath) {
  if (!filePath || filePath.lastIndexOf('.') < 0) {
    return null
  }
  return filePath.slice(filePath.lastIndexOf('.') + 1).toLowerCase()
}

function getNameExtension(fileName) {
  if (!fileName || fileName.lastIndexOf('.') < 0) {
    return null
  }
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase()
}

function resolveMappedMimeType(extension, mapping, unsupportedMessage) {
  const mimeType = mapping[extension]
  if (!mimeType) {
    throw new Error(unsupportedMessage)
  }
  return mimeType
}

function resolvePositiveInt(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return null
  }
  return Math.floor(numberValue)
}

function getImageInfo(filePath) {
  return new Promise((resolve, reject) => {
    wx.getImageInfo({
      src: filePath,
      success: resolve,
      fail: reject
    })
  })
}

function getVideoInfo(filePath) {
  return new Promise((resolve, reject) => {
    wx.getVideoInfo({
      src: filePath,
      success: resolve,
      fail: reject
    })
  })
}

function getFileInfo(filePath) {
  return new Promise((resolve, reject) => {
    wx.getFileInfo({
      filePath,
      success: resolve,
      fail: reject
    })
  })
}

function resolveLocalFilePath(file) {
  if (!file) {
    return ''
  }
  return file.tempFilePath || file.path || ''
}

function resolveVideoPosterTempFilePath(tempFile) {
  if (!tempFile) {
    return ''
  }
  const thumbPath = String(tempFile.thumbTempFilePath || '').trim()
  if (thumbPath) {
    return thumbPath
  }
  const coverPath = String(tempFile.coverTempFilePath || '').trim()
  if (coverPath) {
    return coverPath
  }
  return ''
}

async function resolveImageUploadMeta(tempFile) {
  const filePath = resolveLocalFilePath(tempFile)
  if (!filePath) {
    throw new Error('未获取到图片文件')
  }

  let extension = getPathExtension(filePath)
  let width = resolvePositiveInt(tempFile.width)
  let height = resolvePositiveInt(tempFile.height)

  if (!extension || !width || !height) {
    try {
      const info = await getImageInfo(filePath)
      if (!extension && info && info.type) {
        extension = String(info.type).toLowerCase()
      }
      width = width || resolvePositiveInt(info.width)
      height = height || resolvePositiveInt(info.height)
    } catch (error) {
      if (!extension) {
        throw new Error('无法识别图片格式')
      }
    }
  }

  if (!extension) {
    throw new Error('无法识别图片格式')
  }

  const mimeType = resolveMappedMimeType(extension, IMAGE_MIME_MAPPING, '暂不支持该图片格式')

  let size = resolvePositiveInt(tempFile.size)
  if (!size) {
    try {
      const fileInfo = await getFileInfo(filePath)
      size = resolvePositiveInt(fileInfo.size)
    } catch (error) {
      // ignore
    }
  }
  if (!size) {
    throw new Error('无法读取图片大小')
  }

  return {
    filePath,
    extension,
    mimeType,
    size,
    width,
    height
  }
}

async function resolveVideoUploadMeta(tempFile) {
  const filePath = resolveLocalFilePath(tempFile)
  if (!filePath) {
    throw new Error('未获取到视频文件')
  }

  const extension = getPathExtension(filePath)
  if (!extension) {
    throw new Error('无法识别视频格式')
  }
  const mappedMimeType = resolveMappedMimeType(extension, VIDEO_MIME_MAPPING, '暂不支持该视频格式')

  const mimeTypeFromFile = tempFile && tempFile.mimeType
    ? String(tempFile.mimeType).trim().toLowerCase()
    : ''
  const mimeType = mimeTypeFromFile.startsWith('video/')
    ? mimeTypeFromFile
    : mappedMimeType

  let width = resolvePositiveInt(tempFile.width)
  let height = resolvePositiveInt(tempFile.height)
  if (!width || !height) {
    try {
      const videoInfo = await getVideoInfo(filePath)
      width = width || resolvePositiveInt(videoInfo.width)
      height = height || resolvePositiveInt(videoInfo.height)
    } catch (error) {
      // ignore
    }
  }

  let size = resolvePositiveInt(tempFile.size)
  if (!size) {
    try {
      const fileInfo = await getFileInfo(filePath)
      size = resolvePositiveInt(fileInfo.size)
    } catch (error) {
      // ignore
    }
  }
  if (!size) {
    throw new Error('无法读取视频大小')
  }

  return {
    filePath,
    extension,
    mimeType,
    size,
    width,
    height
  }
}

function resolveFileMimeType(extension, fileType) {
  const normalized = fileType ? String(fileType).trim().toLowerCase() : ''
  if (normalized.includes('/')) {
    return normalized
  }
  return FILE_MIME_MAPPING[extension] || 'application/octet-stream'
}

async function resolveFileUploadMeta(tempFile) {
  const filePath = tempFile && tempFile.tempFilePath ? tempFile.tempFilePath : ''
  if (!filePath) {
    throw new Error('未获取到文件')
  }

  const fileName = String(tempFile.name || filePath.split('/').pop() || '').trim()
  let extension = getNameExtension(fileName) || getPathExtension(filePath)
  if (!extension) {
    throw new Error('文件后缀不能为空')
  }
  extension = extension.toLowerCase()
  if (!FILE_EXTENSION_PATTERN.test(extension)) {
    throw new Error('文件后缀不合法')
  }

  const mimeType = resolveFileMimeType(extension, tempFile.type)

  let size = resolvePositiveInt(tempFile.size)
  if (!size) {
    try {
      const fileInfo = await getFileInfo(filePath)
      size = resolvePositiveInt(fileInfo.size)
    } catch (error) {
      // ignore
    }
  }
  if (!size) {
    throw new Error('无法读取文件大小')
  }

  return {
    filePath,
    fileName: fileName || `文件.${extension}`,
    extension,
    mimeType,
    size
  }
}

function parseUploadResponseBody(responseBody) {
  if (responseBody == null) {
    return null
  }
  if (typeof responseBody === 'object') {
    return responseBody
  }
  if (typeof responseBody !== 'string') {
    return null
  }
  const trimmed = responseBody.trim()
  if (!trimmed) {
    return null
  }
  try {
    return JSON.parse(trimmed)
  } catch (error) {
    return null
  }
}

function uploadFileByPolicy(policy, filePath, mediaLabel) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: policy.host,
      filePath,
      name: 'file',
      formData: {
        key: policy.objectKey,
        policy: policy.policy,
        accessKeyId: policy.accessKeyId,
        signature: policy.signature,
        success_action_status: String(policy.successActionStatus)
      },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const responseBody = parseUploadResponseBody(res.data)
          if (responseBody && typeof responseBody.code === 'number' && responseBody.code !== 0) {
            reject(new Error(responseBody.message || `${mediaLabel}上传失败`))
            return
          }
          resolve()
          return
        }
        reject(new Error(`${mediaLabel}上传失败(${res.statusCode})`))
      },
      fail(err) {
        reject(new Error(err && err.errMsg ? err.errMsg : `${mediaLabel}上传失败`))
      }
    })
  })
}

async function applyUploadPolicy(path, meta) {
  const policyRes = await request.post(path, {
    extension: meta.extension,
    mimeType: meta.mimeType,
    size: meta.size
  })
  return policyRes.data
}

async function uploadChatImage(tempFile) {
  try {
    const meta = await resolveImageUploadMeta(tempFile)
    const policy = await applyUploadPolicy('/media/chat/image/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '图片')

    return {
      mediaKey: policy.objectKey,
      mediaWidth: meta.width,
      mediaHeight: meta.height,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType
    }
  } catch (error) {
    throw toExposedUploadError(error, '图片上传失败')
  }
}

async function uploadChatVideo(tempFile) {
  try {
    const meta = await resolveVideoUploadMeta(tempFile)
    const policy = await applyUploadPolicy('/media/chat/video/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '视频')

    let mediaPosterKey = ''
    const posterTempFilePath = resolveVideoPosterTempFilePath(tempFile)
    if (posterTempFilePath) {
      try {
        const posterMeta = await resolveImageUploadMeta({ tempFilePath: posterTempFilePath })
        const posterPolicy = await applyUploadPolicy('/media/chat/image/upload-policy', posterMeta)
        await uploadFileByPolicy(posterPolicy, posterMeta.filePath, '视频封面')
        mediaPosterKey = posterPolicy.objectKey
      } catch (error) {
        // 封面上传失败不阻断视频消息发送，客户端会退回无封面样式。
        mediaPosterKey = ''
      }
    }

    return {
      mediaKey: policy.objectKey,
      mediaPosterKey,
      mediaWidth: meta.width || null,
      mediaHeight: meta.height || null,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType
    }
  } catch (error) {
    throw toExposedUploadError(error, '视频上传失败')
  }
}

async function uploadChatFile(tempFile) {
  try {
    const filePath = resolveLocalFilePath(tempFile)
    const meta = await resolveFileUploadMeta({
      ...tempFile,
      tempFilePath: filePath
    })
    const policy = await applyUploadPolicy('/media/chat/file/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '文件')

    return {
      mediaKey: policy.objectKey,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType,
      fileName: meta.fileName
    }
  } catch (error) {
    throw toExposedUploadError(error, '文件上传失败')
  }
}

async function uploadForumThreadImage(tempFile) {
  try {
    const meta = await resolveImageUploadMeta(tempFile)
    const policy = await applyUploadPolicy('/media/forum/thread/image/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '图片')
    return {
      mediaKey: policy.objectKey,
      mediaWidth: meta.width,
      mediaHeight: meta.height,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType
    }
  } catch (error) {
    throw toExposedUploadError(error, '图片上传失败')
  }
}

async function uploadForumThreadVideo(tempFile) {
  try {
    const meta = await resolveVideoUploadMeta(tempFile)
    const policy = await applyUploadPolicy('/media/forum/thread/video/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '视频')

    let mediaPosterKey = ''
    const posterTempFilePath = resolveVideoPosterTempFilePath(tempFile)
    if (posterTempFilePath) {
      try {
        const posterMeta = await resolveImageUploadMeta({ tempFilePath: posterTempFilePath })
        const posterPolicy = await applyUploadPolicy('/media/forum/thread/image/upload-policy', posterMeta)
        await uploadFileByPolicy(posterPolicy, posterMeta.filePath, '视频封面')
        mediaPosterKey = posterPolicy.objectKey
      } catch (error) {
        // 封面上传失败不阻断视频发帖，详情页会退回无封面样式。
        mediaPosterKey = ''
      }
    }

    return {
      mediaKey: policy.objectKey,
      mediaPosterKey,
      mediaWidth: meta.width || null,
      mediaHeight: meta.height || null,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType
    }
  } catch (error) {
    throw toExposedUploadError(error, '视频上传失败')
  }
}

async function uploadForumReplyImage(tempFile) {
  try {
    const meta = await resolveImageUploadMeta(tempFile)
    const policy = await applyUploadPolicy('/media/forum/reply/image/upload-policy', meta)
    await uploadFileByPolicy(policy, meta.filePath, '图片')
    return {
      mediaKey: policy.objectKey,
      mediaWidth: meta.width,
      mediaHeight: meta.height,
      mediaSize: meta.size,
      mediaMimeType: meta.mimeType
    }
  } catch (error) {
    throw toExposedUploadError(error, '图片上传失败')
  }
}

module.exports = {
  uploadChatImage,
  uploadChatVideo,
  uploadChatFile,
  uploadForumThreadImage,
  uploadForumThreadVideo,
  uploadForumReplyImage
}
