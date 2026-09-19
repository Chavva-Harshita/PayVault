import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import * as userService from '../services/userService'
import { extractErrorMessage } from '../services/api'

export default function Profile() {
  const { user, refreshProfile } = useAuth()
  const [form, setForm] = useState({ name: user?.name || '', phone: user?.phone || '' })
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  function update(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setMessage(null)
    setIsSaving(true)
    try {
      await userService.updateMe(form)
      await refreshProfile()
      setMessage('Profile updated')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSaving(false)
    }
  }

  if (!user) return null

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="font-display text-2xl text-ink">Profile</h1>

      <div className="ledger-stub divide-y divide-line py-2 pl-6 pr-4 text-sm">
        <Row label="User ID" value={user.userId} mono />
        <Row label="Email" value={user.email} />
        <Row label="Account status" value={user.status || '—'} />
      </div>

      <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-line bg-paper-raised p-6">
        <div>
          <label className="block text-sm font-medium text-ink" htmlFor="name">
            Name
          </label>
          <input
            id="name"
            type="text"
            value={form.name}
            onChange={update('name')}
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
            value={form.phone}
            onChange={update('phone')}
            className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
          />
        </div>

        {message && <p className="text-sm text-vault">{message}</p>}
        {error && <p className="text-sm text-clay">{error}</p>}

        <button
          type="submit"
          disabled={isSaving}
          className="rounded-md bg-vault px-4 py-2 text-sm font-medium text-white transition hover:bg-vault-dark disabled:opacity-50"
        >
          {isSaving ? 'Saving…' : 'Save changes'}
        </button>
      </form>
    </div>
  )
}

function Row({ label, value, mono }) {
  return (
    <div className="flex items-center justify-between py-3">
      <span className="text-fade">{label}</span>
      <span className={mono ? 'figure text-ink' : 'text-ink'}>{value}</span>
    </div>
  )
}
