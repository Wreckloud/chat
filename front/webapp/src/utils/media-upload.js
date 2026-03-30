import http from '@/api/http'
import {
  applyChatFileUploadPolicy,
  applyChatImageUploadPolicy,
  applyChatVideoUploadPolicy,
  applyForumReplyImageUploadPolicy,
  applyForumThreadImageUploadPolicy,
  applyForumThreadVideoUploadPolicy
} from '@/api/modules'

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

function normalizeFileExtension(fileName) {
  const normalized = String(fileName || '').trim()
  const dotIndex = normalized.lastIndexOf('.')
  if (dotIndex < 0 || dotIndex === normalized.length - 1) {
    return ''
  }
  return normalized.slice(dotIndex + 1).toLowerCase()
}

function resolveImageMimeType(extension, fileMimeType) {
  const normalizedMimeType = String(fileMimeType || '').trim().toLowerCase()
  if (normalizedMimeType.startsWith('image/')) {
    return normalizedMimeType
  }
  const mapped = IMAGE_MIME_MAPPING[extension]
  if (!mapped) {
    throw new Error('暂不支持该图片格式')
  }
  return mapped
}

function resolveVideoMimeType(extension, fileMimeType) {
  const normalizedMimeType = String(fileMimeType || '').trim().toLowerCase()
  if (normalizedMimeType.startsWith('video/')) {
    return normalizedMimeType
  }
  const mapped = VIDEO_MIME_MAPPING[extension]
  if (!mapped) {
    throw new Error('暂不支持该视频格式')
  }
  return mapped
}

function resolveFileMimeType(extension, fileMimeType) {
  const normalizedMimeType = String(fileMimeType || '').trim().toLowerCase()
  if (normalizedMimeType.includes('/')) {
    return normalizedMimeType
  }
  return FILE_MIME_MAPPING[extension] || 'application/octet-stream'
}

function toPercent(loaded, total) {
  if (!Number.isFinite(loaded) || !Number.isFinite(total) || total <= 0) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round((loaded / total) * 100)))
}

async function uploadByPolicy(policy, file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('key', policy.objectKey)
  formData.append('policy', policy.policy || '')
  formData.append('accessKeyId', policy.accessKeyId || '')
  formData.append('signature', policy.signature || '')
  formData.append('success_action_status', String(policy.successActionStatus || 200))
  const onProgress = typeof options.onProgress === 'function' ? options.onProgress : null
  await http.post('/media/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (event) => {
      if (!onProgress) {
        return
      }
      onProgress(toPercent(event.loaded, event.total))
    }
  })
}

function loadImageDimension(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      resolve({
        width: Number(image.naturalWidth || image.width || 0),
        height: Number(image.naturalHeight || image.height || 0)
      })
      URL.revokeObjectURL(objectUrl)
    }
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('无法读取图片尺寸'))
    }
    image.src = objectUrl
  })
}

function loadVideoMeta(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.muted = true
    const cleanup = () => {
      URL.revokeObjectURL(objectUrl)
      video.removeAttribute('src')
      video.load()
    }
    video.onloadedmetadata = () => {
      resolve({
        width: Number(video.videoWidth || 0),
        height: Number(video.videoHeight || 0),
        duration: Number(video.duration || 0)
      })
      cleanup()
    }
    video.onerror = () => {
      cleanup()
      reject(new Error('无法读取视频信息'))
    }
    video.src = objectUrl
  })
}

async function captureVideoPosterFile(videoFile) {
  const objectUrl = URL.createObjectURL(videoFile)
  const video = document.createElement('video')
  video.preload = 'metadata'
  video.muted = true

  const cleanup = () => {
    URL.revokeObjectURL(objectUrl)
    video.removeAttribute('src')
    video.load()
  }

  try {
    await new Promise((resolve, reject) => {
      video.onloadeddata = () => resolve()
      video.onerror = () => reject(new Error('未获取到视频封面，请稍后再试'))
      video.src = objectUrl
    })
    const seekTarget = Number.isFinite(video.duration) && video.duration > 0.8 ? 0.8 : 0
    await new Promise((resolve, reject) => {
      const onSeeked = () => resolve()
      const onError = () => reject(new Error('未获取到视频封面，请稍后再试'))
      video.addEventListener('seeked', onSeeked, { once: true })
      video.addEventListener('error', onError, { once: true })
      try {
        video.currentTime = seekTarget
      } catch (error) {
        resolve()
      }
    })

    const width = Number(video.videoWidth || 0)
    const height = Number(video.videoHeight || 0)
    if (!width || !height) {
      throw new Error('未获取到视频封面，请稍后再试')
    }
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) {
      throw new Error('未获取到视频封面，请稍后再试')
    }
    context.drawImage(video, 0, 0, width, height)
    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob((result) => {
        if (!result) {
          reject(new Error('未获取到视频封面，请稍后再试'))
          return
        }
        resolve(result)
      }, 'image/jpeg', 0.9)
    })
    return new File([blob], `${Date.now()}_poster.jpg`, { type: 'image/jpeg' })
  } finally {
    cleanup()
  }
}

async function uploadImageWithPolicy(applyPolicyFunc, file, options = {}) {
  const extension = normalizeFileExtension(file.name)
  if (!extension) {
    throw new Error('无法识别图片格式')
  }
  const mimeType = resolveImageMimeType(extension, file.type)
  const { width, height } = await loadImageDimension(file)
  const policy = await applyPolicyFunc({
    extension,
    mimeType,
    size: Number(file.size || 0)
  })
  await uploadByPolicy(policy, file, options)
  return {
    mediaKey: policy.objectKey,
    mediaWidth: width || null,
    mediaHeight: height || null,
    mediaSize: Number(file.size || 0),
    mediaMimeType: mimeType
  }
}

async function uploadVideoWithPolicy(applyVideoPolicyFunc, applyPosterPolicyFunc, file, options = {}) {
  const extension = normalizeFileExtension(file.name)
  if (!extension) {
    throw new Error('无法识别视频格式')
  }
  const mimeType = resolveVideoMimeType(extension, file.type)
  const meta = await loadVideoMeta(file)
  const videoPolicy = await applyVideoPolicyFunc({
    extension,
    mimeType,
    size: Number(file.size || 0)
  })
  await uploadByPolicy(videoPolicy, file, options)
  const posterFile = await captureVideoPosterFile(file)
  const posterPolicy = await applyPosterPolicyFunc({
    extension: 'jpg',
    mimeType: 'image/jpeg',
    size: Number(posterFile.size || 0)
  })
  await uploadByPolicy(posterPolicy, posterFile, {})
  return {
    mediaKey: videoPolicy.objectKey,
    mediaPosterKey: posterPolicy.objectKey,
    mediaWidth: meta.width || null,
    mediaHeight: meta.height || null,
    mediaSize: Number(file.size || 0),
    mediaMimeType: mimeType
  }
}

export async function uploadChatImage(file, options = {}) {
  return uploadImageWithPolicy(applyChatImageUploadPolicy, file, options)
}

export async function uploadChatVideo(file, options = {}) {
  return uploadVideoWithPolicy(applyChatVideoUploadPolicy, applyChatImageUploadPolicy, file, options)
}

export async function uploadChatFile(file, options = {}) {
  const extension = normalizeFileExtension(file.name)
  if (!extension) {
    throw new Error('文件后缀不能为空')
  }
  const mimeType = resolveFileMimeType(extension, file.type)
  const policy = await applyChatFileUploadPolicy({
    extension,
    mimeType,
    size: Number(file.size || 0)
  })
  await uploadByPolicy(policy, file, options)
  return {
    mediaKey: policy.objectKey,
    mediaSize: Number(file.size || 0),
    mediaMimeType: mimeType,
    fileName: String(file.name || '文件')
  }
}

export async function uploadForumThreadImage(file, options = {}) {
  return uploadImageWithPolicy(applyForumThreadImageUploadPolicy, file, options)
}

export async function uploadForumThreadVideo(file, options = {}) {
  return uploadVideoWithPolicy(applyForumThreadVideoUploadPolicy, applyForumThreadImageUploadPolicy, file, options)
}

export async function uploadForumReplyImage(file, options = {}) {
  return uploadImageWithPolicy(applyForumReplyImageUploadPolicy, file, options)
}
