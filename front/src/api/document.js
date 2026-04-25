import request from '@/utils/request'

export const uploadFile = (file, roles, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('roles', roles)

  return request({
    url: '/doc/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: onProgress,
  })
}
