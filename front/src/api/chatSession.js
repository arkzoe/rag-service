import request from '@/utils/request'

/**
 * 创建对话会话
 * @param {Object} data - 请求数据
 * @param {string} data.title - 对话标题
 * @param {string} data.description - 对话描述
 */
export function createSession(data) {
  return request({
    url: '/api/chat/session/create',
    method: 'post',
    data,
  })
}

/**
 * 获取用户对话列表
 */
export function getSessionList() {
  return request({
    url: '/api/chat/session/list',
    method: 'get',
  })
}

/**
 * 获取会话详情
 * @param {string} sessionId - 会话ID
 */
export function getSessionDetail(sessionId) {
  return request({
    url: `/api/chat/session/detail/${sessionId}`,
    method: 'get',
  })
}

/**
 * 删除单个对话会话
 * @param {string} sessionId - 会话ID
 */
export function deleteSession(sessionId) {
  return request({
    url: `/api/chat/session/delete/${sessionId}`,
    method: 'post',
  })
}

/**
 * 批量删除对话会话
 * @param {Object} data - 请求数据
 * @param {string[]} data.sessionIds - 会话ID列表
 */
export function batchDeleteSessions(data) {
  return request({
    url: '/api/chat/session/delete/batch',
    method: 'post',
    data,
  })
}

/**
 * 更新对话会话信息
 * @param {string} sessionId - 会话ID
 * @param {Object} data - 请求数据
 * @param {string} data.title - 对话标题
 * @param {string} data.description - 对话描述
 */
export function updateSession(sessionId, data) {
  return request({
    url: `/api/chat/session/update/${sessionId}`,
    method: 'post',
    data,
  })
}
