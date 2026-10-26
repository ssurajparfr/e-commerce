import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { formatCents } from '../lib/money'

type OrderItem = { id: number; description: string; unitPriceCents: number; quantity: number; lineTotalCents: number }
type Order = {
  id: number; status: string; paymentStatus: string; subtotalCents: number; vatCents: number; totalCents: number;
  vatRateBasisPoints: number; items: OrderItem[]
}

export default function Confirmation() {
  const { id } = useParams()
  const { data, isLoading, isError } = useQuery<Order>({
    queryKey: ['order', id],
    queryFn: async () => {
      const res = await fetch(`/api/orders/${id}`)
      if (!res.ok) throw new Error('Order not found')
      return res.json()
    }
  })

  if (isLoading) return <div>Loading...</div>
  if (isError || !data) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-4">Order not found</h1>
        <Link to="/" className="text-blue-600 underline">Back to catalog</Link>
      </div>
    )
  }
  const items = data.items ?? []
  const vatPercent = Math.round(data.vatRateBasisPoints / 100)
  return (
    <div data-testid="confirmation">
      <h1 className="text-2xl font-bold mb-4">Order confirmation</h1>
      <div>Order ID: {data.id}</div>
      <div>Order state: {data.status}</div>
      <div>Payment status: {data.paymentStatus}</div>
      <ul className="mt-2">
        {items.map(i => (
          <li key={i.id}>
            {i.description} - {formatCents(i.unitPriceCents)} x {i.quantity} = {formatCents(i.unitPriceCents * i.quantity)}
          </li>
        ))}
      </ul>
      <div className="mt-2">Subtotal: {formatCents(data.subtotalCents)}</div>
      <div>VAT ({vatPercent}%): {formatCents(data.vatCents)}</div>
      <div>Total: {formatCents(data.totalCents)}</div>
    </div>
  )
}
