import { createContext, useEffect, useState, useCallback } from 'react'
import * as authService from '../services/authService'
import * as userService from '../services/userService'
import { getToken, getUserId, setSession, clearSession, isTokenExpired } from '../utils/token'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const loadProfile = useCallback(async () => {
    try {
      const profile = await userService.getMe()
      setUser(profile)
    } catch {
      // Profile fetch failing (e.g. user-service briefly down) shouldn't
      // silently log the person out - the token itself is still valid.
      // Fall back to a minimal identity from the token so the app stays usable.
      setUser({ userId: getUserId() })
    }
  }, [])

  useEffect(() => {
    const token = getToken()
    if (token && !isTokenExpired(token)) {
      loadProfile().finally(() => setIsLoading(false))
    } else {
      clearSession()
      setIsLoading(false)
    }
  }, [loadProfile])

  async function login(credentials) {
    const { token, userId } = await authService.login(credentials)
    setSession(token, userId)
    await loadProfile()
  }

  async function register(details) {
    return authService.register(details)
  }

  function logout() {
    clearSession()
    setUser(null)
  }

  const value = {
    user,
    isAuthenticated: Boolean(user),
    isLoading,
    login,
    register,
    logout,
    refreshProfile: loadProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
