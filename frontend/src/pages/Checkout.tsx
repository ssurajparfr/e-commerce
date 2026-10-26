import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

export default function Checkout() {
  const navigate = useNavigate()
  const [key, setKey] = useState('idem-' + Math.random().toString(36).slice(2))
  const mutation = useMutation({
    mutationFn: async () => {
      const res = await fetch('/api/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': key
        },
        body: JSON.stringify({})
      })
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    },
    onSuccess: (data) => navigate(`/orders/${data.id}`)
  })
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>
      <button
        className="px-3 py-1 bg-blue-600 text-white rounded"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? 'Processing...' : 'Place order'}
      </button>
      {mutation.isError && <div className="text-red-600 mt-2">{String(mutation.error)}</div>}
    </div>
  )
}
