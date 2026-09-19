import api from './api'

export function register({ name, email, phone, password }) {
  return api.post('/api/auth/register', { name, email, phone, password }).then((res) => res.data)
}

export function login({ email, password }) {
  return api.post('/api/auth/login', { email, password }).then((res) => res.data)
}
