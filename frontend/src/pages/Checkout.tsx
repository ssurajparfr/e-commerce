import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'

export default function Checkout() {
  const navigate = useNavigate()
  const [idempotencyKey] = useState(() => 'idem-' + Math.random().toString(36).slice(2))
  const { data: cart, isLoading, isError, refetch } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => {
      const res = await fetch('/api/cart')
      if (!res.ok) throw new Error('Failed to load cart')
      return res.json()
    }
  })
  const mutation = useMutation({
    mutationFn: async () => {
      const res = await fetch('/api/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify({})
      })
      if (!res.ok) throw new Error('Checkout failed. Please try again.')
      return res.json()
    },
    onSuccess: (data) => navigate(`/orders/${data.id}`)
  })

  if (isLoading) return <div>Loading cart...</div>
  if (isError) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-4">Checkout</h1>
        <p className="text-red-600">Could not load cart.</p>
        <button className="mt-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    )
  }

  const isEmpty = !cart?.items?.length
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>
      {isEmpty ? (
        <div className="mb-4 text-gray-700">Your cart is empty.</div>
      ) : (
        <div className="mb-4 text-gray-700">Items in cart: {cart.items.length}</div>
      )}
      <button
        className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending || isEmpty}
      >
        {mutation.isPending ? 'Processing...' : 'Place order'}
      </button>
      {mutation.isError && <div className="text-red-600 mt-2">Checkout failed. Please try again.</div>}
    </div>
  )
}
