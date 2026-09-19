import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import * as walletService from '../services/walletService'
import * as transactionService from '../services/transactionService'
import { connectWalletUpdates } from '../services/socketService'
import BalanceCard from '../components/BalanceCard'
import TransferForm from '../components/TransferForm'
import TransactionCard from '../components/TransactionCard'
import LoadingSpinner from '../components/LoadingSpinner'
import { extractErrorMessage } from '../services/api'

export default function Dashboard() {
  const { user } = useAuth()
  const [wallet, setWallet] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [walletMissing, setWalletMissing] = useState(false)
  const [error, setError] = useState(null)
  const [isLive, setIsLive] = useState(false)

  const loadData = useCallback(async () => {
    setError(null)
    try {
      const [walletData, txPage] = await Promise.all([
        walletService.getMyWallet(),
        transactionService.listTransactions({ page: 0, size: 5 }),
      ])
      setWallet(walletData)
      setTransactions(txPage.content)
      setWalletMissing(false)
    } catch (err) {
      if (err.response?.status === 404 && err.response?.data?.error === 'WALLET_NOT_FOUND') {
        setWalletMissing(true)
      } else {
        setError(extractErrorMessage(err))
      }
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Only connect once there's a wallet to actually update - no point
  // opening a socket while still showing the "create your wallet" screen.
  useEffect(() => {
    if (!wallet || !user?.userId) return

    const disconnect = connectWalletUpdates((update) => {
      setWallet((prev) => (prev ? { ...prev, balance: update.balanceAfter } : prev))

      const synthetic = {
        transactionId: update.transactionId,
        senderId: update.direction === 'DEBIT' ? user.userId : update.counterpartyId,
        receiverId: update.direction === 'DEBIT' ? update.counterpartyId : user.userId,
        amount: update.amount,
        currency: update.currency,
        status: 'SUCCESS',
        createdAt: update.timestamp,
      }
      setTransactions((prev) => [synthetic, ...prev.filter((t) => t.transactionId !== synthetic.transactionId)].slice(0, 5))
    })

    setIsLive(true)
    return () => {
      setIsLive(false)
      disconnect()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [Boolean(wallet), user?.userId])

  async function handleCreateWallet() {
    setIsLoading(true)
    try {
      await walletService.createWallet()
      await loadData()
    } catch (err) {
      setError(extractErrorMessage(err))
      setIsLoading(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <LoadingSpinner label="Loading your wallet" />
      </div>
    )
  }

  if (walletMissing) {
    return (
      <div className="mx-auto max-w-md py-20 text-center">
        <p className="font-display text-xl text-ink">No wallet yet</p>
        <p className="mt-2 text-sm text-fade">Create one to start sending and receiving money.</p>
        <button
          onClick={handleCreateWallet}
          className="mt-6 rounded-md bg-vault px-5 py-2 text-sm font-medium text-white transition hover:bg-vault-dark"
        >
          Create my wallet
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {error && <p className="rounded-md bg-clay/10 p-3 text-sm text-clay">{error}</p>}

      <BalanceCard wallet={wallet} />

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="rounded-xl border border-line bg-paper-raised p-6 lg:col-span-2">
          <h2 className="font-display text-lg text-ink">Quick transfer</h2>
          <div className="mt-4">
            <TransferForm onSuccess={loadData} />
          </div>
        </div>

        <div className="lg:col-span-3">
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h2 className="font-display text-lg text-ink">Recent activity</h2>
              {isLive && (
                <span className="flex items-center gap-1 rounded-full bg-vault/10 px-2 py-0.5 text-xs font-medium text-vault">
                  <span className="h-1.5 w-1.5 rounded-full bg-vault" />
                  Live
                </span>
              )}
            </div>
            <Link to="/transactions" className="text-sm font-medium text-vault hover:underline">
              View all →
            </Link>
          </div>
          {transactions.length === 0 ? (
            <div className="rounded-xl border border-dashed border-line py-12 text-center text-fade">
              No transactions yet.
            </div>
          ) : (
            <div className="space-y-3">
              {transactions.map((transaction) => (
                <TransactionCard
                  key={transaction.transactionId}
                  transaction={transaction}
                  currentUserId={user?.userId}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
