const TOKEN_KEY = 'payvault_token'
const USER_ID_KEY = 'payvault_user_id'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUserId() {
  return localStorage.getItem(USER_ID_KEY)
}

export function setSession(token, userId) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_ID_KEY, userId)
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
}

/**
 * Decodes the JWT payload client-side purely to check expiry for UI
 * purposes (e.g. skip a doomed API call and bounce to /login early).
 * This is NOT a security check - the Gateway is the only thing that
 * actually verifies a token's signature. A user could edit this decoded
 * value in devtools and it would change nothing about what the backend
 * accepts.
 */
export function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (!payload.exp) return false
    return Date.now() >= payload.exp * 1000
  } catch {
    return true
  }
}
