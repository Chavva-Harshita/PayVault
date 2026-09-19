import { NavLink } from 'react-router-dom'

const LINKS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/wallet', label: 'Wallet' },
  { to: '/transfer', label: 'Transfer' },
  { to: '/transactions', label: 'Ledger' },
  { to: '/profile', label: 'Profile' },
]

export default function Sidebar() {
  return (
    <nav className="hidden w-48 shrink-0 border-r border-line bg-paper-raised p-4 sm:block">
      <ul className="space-y-1">
        {LINKS.map((link) => (
          <li key={link.to}>
            <NavLink
              to={link.to}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm font-medium transition ${
                  isActive ? 'bg-vault text-white' : 'text-ink hover:bg-line/60'
                }`
              }
            >
              {link.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
