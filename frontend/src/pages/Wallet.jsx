import { useEffect, useState } from 'react'
import * as walletService from '../services/walletService'
import BalanceCard from '../components/BalanceCard'
import LoadingSpinner from '../components/LoadingSpinner'
import { extractErrorMessage } from '../services/api'

export default function Wallet() {
  const [wallet, setWallet] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    walletService
      .getMyWallet()
      .then(setWallet)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <LoadingSpinner label="Loading wallet" />
      </div>
    )
  }

  if (error) {
    return <p className="rounded-md bg-clay/10 p-3 text-sm text-clay">{error}</p>
  }

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="font-display text-2xl text-ink">Wallet</h1>
      <BalanceCard wallet={wallet} />

      <div className="ledger-stub divide-y divide-line py-2 pl-6 pr-4">
        <Row label="Wallet ID" value={wallet.walletId} mono />
        <Row label="Owner" value={wallet.userId} mono />
        <Row label="Currency" value={wallet.currency} />
        <Row label="Status" value={wallet.status} />
      </div>
    </div>
  )
}

function Row({ label, value, mono }) {
  return (
    <div className="flex items-center justify-between py-3 text-sm">
      <span className="text-fade">{label}</span>
      <span className={mono ? 'figure text-ink' : 'text-ink'}>{value}</span>
    </div>
  )
}
