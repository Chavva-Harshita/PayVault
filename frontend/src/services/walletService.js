import api from './api'

export function createWallet() {
  return api.post('/api/wallets').then((res) => res.data)
}

export function getMyWallet() {
  return api.get('/api/wallets/me').then((res) => res.data)
}

export function getMyBalance() {
  return api.get('/api/wallets/me/balance').then((res) => res.data)
}
