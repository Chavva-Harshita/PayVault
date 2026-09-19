import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { extractErrorMessage } from '../services/api'

export default function Register() {
  const { register, login } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', confirmPassword: '' })
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function update(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setIsSubmitting(true)
    try {
      await register({
        name: form.name,
        email: form.email,
        phone: form.phone,
        password: form.password,
      })
      // Registration doesn't return a token (see the API contract) - log
      // the person straight in afterward so they don't have to retype
      // their credentials on a second screen.
      await login({ email: form.email, password: form.password })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="font-display text-3xl text-ink">
            Pay<span className="text-vault">Vault</span>
          </div>
          <p className="mt-2 text-sm text-fade">Open a new wallet</p>
        </div>

        <form onSubmit={handleSubmit} className="ledger-stub space-y-4 p-6">
          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="name">
              Full name
            </label>
            <input
              id="name"
              type="text"
              required
              value={form.name}
              onChange={update('name')}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={form.email}
              onChange={update('email')}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="phone">
              Phone
            </label>
            <input
              id="phone"
              type="tel"
              required
              value={form.phone}
              onChange={update('phone')}
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
              minLength={8}
              value={form.password}
              onChange={update('password')}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink" htmlFor="confirmPassword">
              Confirm password
            </label>
            <input
              id="confirmPassword"
              type="password"
              required
              value={form.confirmPassword}
              onChange={update('confirmPassword')}
              className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
            />
          </div>

          {error && <p className="text-sm text-clay">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-vault px-4 py-2 text-sm font-medium text-white transition hover:bg-vault-dark disabled:opacity-50"
          >
            {isSubmitting ? 'Creating account…' : 'Register'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-fade">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-vault hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
