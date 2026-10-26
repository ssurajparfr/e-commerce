import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { formatCents } from '../lib/money'

export default function Checkout() {
  const navigate = useNavigate()
  const { data: cart } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => (await fetch('/api/cart')).json()
  })
  const mutation = useMutation({
    mutationFn: async () => {
      const res = await fetch('/api/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': 'idem-' + Math.random().toString(36).slice(2)
        },
        body: JSON.stringify({})
      })
      if (!res.ok) throw new Error('Checkout failed. Please try again.')
      return res.json()
    },
    onSuccess: (data) => navigate(`/orders/${data.id}`)
  })
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>
      {cart?.items?.length ? (
        <div className="mb-4 text-gray-700">Items in cart: {cart.items.length}</div>
      ) : (
        <div className="mb-4 text-gray-700">Your cart is empty.</div>
      )}
      <button
        className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending || !cart?.items?.length}
      >
        {mutation.isPending ? 'Processing...' : 'Place order'}
      </button>
      {mutation.isError && <div className="text-red-600 mt-2">Checkout failed. Please try again.</div>}
    </div>
  )
}
