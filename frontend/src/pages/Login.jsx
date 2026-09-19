import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { extractErrorMessage } from '../services/api'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await login({ email, password })
      const destination = location.state?.from?.pathname || '/dashboard'
      navigate(destination, { replace: true })
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="font-display text-3xl text-ink">
            Pay<span className="text-vault">Vault</span>
          </div>
          <p className="mt-2 text-sm text-fade">Sign in to your wallet</p>
        </div>

        <form onSubmit={handleSubmit} className="ledger-stub space-y-4 p-6">
          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          {error && <p className="text-sm text-clay">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-vault px-4 py-2 text-sm font-medium text-white transition hover:bg-vault-dark disabled:opacity-50"
          >
            {isSubmitting ? 'Signing in…' : 'Log in'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-fade">
          New here?{' '}
          <Link to="/register" className="font-medium text-vault hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  )
}
