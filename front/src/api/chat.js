import request from '@/utils/request'

export const sendMessage = (question, sessionId) => {
  return request({
    url: '/chat/send',
    method: 'post',
    data: { question, sessionId },
  })
}

export const getChatHistory = (sessionId) => {
  return request({
    url: `/chat/history/${sessionId}`,
    method: 'get',
  })
}
