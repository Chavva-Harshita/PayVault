import api from './api'

export function getMe() {
  return api.get('/api/users/me').then((res) => res.data)
}

export function updateMe({ name, phone, profileImage }) {
  return api.put('/api/users/me', { name, phone, profileImage }).then((res) => res.data)
}
