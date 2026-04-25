import request from '@/utils/request'

export const sendMessage = (question) => {
  return request({
    url: '/chat/send',
    method: 'post',
    data: { question },
  })
}
