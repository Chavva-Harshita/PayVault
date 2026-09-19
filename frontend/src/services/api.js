import axios from 'axios'
import { getToken, clearSession } from '../utils/token'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: BASE_URL,
})

// Every authenticated call automatically carries the JWT - no component
// or service function needs to attach this header itself.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// A 401 from the Gateway (invalid/expired token) always means "log out
// and send them to /login" - handled once here instead of in every
// service function or page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

/**
 * Every backend error response follows the same shape - see the
 * architecture doc's error handling section. This pulls the
 * human-readable message out consistently so pages don't each
 * reimplement the same defensive fallback chain.
 */
export function extractErrorMessage(error) {
  return (
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    'Something went wrong'
  )
}

export default api
