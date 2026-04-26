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

export const getDocumentList = () => {
  return request({
    url: '/doc/list',
    method: 'get',
  })
}

export const deleteDocument = (docId) => {
  return request({
    url: `/doc/delete/${docId}`,
    method: 'post',
  })
}
