import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="flex h-16 items-center justify-between border-b border-line bg-ink px-6 text-paper">
      <div className="font-display text-xl tracking-tight">
        Pay<span className="text-vault-light">Vault</span>
      </div>
      <div className="flex items-center gap-4">
        {user?.name && <span className="hidden font-mono text-sm text-paper/70 sm:inline">{user.name}</span>}
        <button
          onClick={handleLogout}
          className="rounded-md border border-paper/20 px-3 py-1.5 text-sm transition hover:bg-paper/10"
        >
          Log out
        </button>
      </div>
    </header>
  )
}
