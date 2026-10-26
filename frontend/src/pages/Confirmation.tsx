import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'

type OrderItem = { id: number; description: string; unitPriceCents: number; quantity: number; lineTotalCents: number }
type Order = {
  id: number; status: string; paymentStatus: string; subtotalCents: number; vatCents: number; totalCents: number;
  items: OrderItem[]
}

export default function Confirmation() {
  const { id } = useParams()
  const { data, isLoading } = useQuery<Order>({
    queryKey: ['order', id],
    queryFn: async () => (await fetch(`/api/orders/${id}`)).json()
  })
  if (isLoading) return <div>Loading...</div>
  if (!data) return <div>Not found</div>
  return (
    <div data-testid="confirmation">
      <h1 className="text-2xl font-bold mb-4">Order confirmation</h1>
      <div>Order ID: {data.id}</div>
      <div>Order state: {data.status}</div>
      <div>Payment status: {data.paymentStatus}</div>
      <ul className="mt-2">
        {data.items.map(i => (
          <li key={i.id}>{i.description} - {i.unitPriceCents} x {i.quantity} = {i.lineTotalCents}</li>
        ))}
      </ul>
      <div className="mt-2">Subtotal: {data.subtotalCents}</div>
      <div>VAT: {data.vatCents}</div>
      <div>Total: {data.totalCents}</div>
    </div>
  )
}
