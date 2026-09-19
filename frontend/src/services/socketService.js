import { Client } from '@stomp/stompjs'
import { getToken } from '../utils/token'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * Native WebSocket handshakes can't carry a custom Authorization header -
 * that's a browser platform limitation, not something fixable client-side.
 * So this connects first, then authenticates on the STOMP CONNECT frame
 * itself (a plain text frame sent over the already-open socket, which has
 * no such restriction) - see transaction-service's
 * StompAuthChannelInterceptor for the server side of this.
 */
function buildSocketUrl() {
  return BASE_URL.replace(/^http/, 'ws') + '/ws'
}

/**
 * Opens one connection and subscribes to this user's personal wallet-update
 * queue. Returns a cleanup function - call it on unmount to deactivate the
 * client and avoid leaking a connection per page visit.
 *
 * onMessage receives the parsed WalletUpdateMessage payload (see
 * transaction-service's WalletUpdateMessage) whenever a transfer this user
 * is part of completes, for as long as the socket stays connected.
 */
export function connectWalletUpdates(onMessage) {
  const token = getToken()
  if (!token) {
    return () => {}
  }

  const client = new Client({
    brokerURL: buildSocketUrl(),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    // Quiet by default - flip to console.log for debugging connection issues.
    debug: () => {},
  })

  client.onConnect = () => {
    client.subscribe('/user/queue/wallet-updates', (message) => {
      try {
        onMessage(JSON.parse(message.body))
      } catch {
        // Malformed payload - ignore rather than crash the socket handler.
      }
    })
  }

  client.activate()

  return () => {
    client.deactivate()
  }
}
