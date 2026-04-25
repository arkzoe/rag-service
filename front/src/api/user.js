import request from '@/utils/request'

export const login = (username, password) => {
  return request({
    url: '/user/login',
    method: 'post',
    data: { username, password },
  })
}

export const logout = () => {
  return request({
    url: '/user/logout',
    method: 'post',
  })
}

export const getUserInfo = () => {
  return request({
    url: '/user/info',
    method: 'get',
  })
}
