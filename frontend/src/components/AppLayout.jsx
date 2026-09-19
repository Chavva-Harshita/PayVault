import Navbar from './Navbar'
import Sidebar from './Sidebar'

export default function AppLayout({ children }) {
  return (
    <div className="min-h-screen bg-paper">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6 sm:p-8">{children}</main>
      </div>
    </div>
  )
}
