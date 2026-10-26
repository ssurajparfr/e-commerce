import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { formatCents } from '../lib/money'

type CartItem = { id: number; productId: number; quantity: number }
type Cart = { id: number; items: CartItem[] }
type Product = { id: number; name: string; priceCents: number }

export default function Cart() {
  const { data: cart, isLoading, isError, refetch } = useQuery<Cart>({
    queryKey: ['cart'],
    queryFn: async () => {
      const res = await fetch('/api/cart')
      if (!res.ok) throw new Error('Failed to load cart')
      return res.json()
    }
  })
  const { data: products } = useQuery<{ content: Product[] }>({
    queryKey: ['products'],
    queryFn: async () => {
      const res = await fetch('/api/products')
      if (!res.ok) throw new Error('Failed to load products')
      return res.json()
    }
  })

  if (isLoading) return <div>Loading cart...</div>
  if (isError) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-4">Cart</h1>
        <p className="text-red-600">Could not load cart.</p>
        <button className="mt-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    )
  }

  const items = cart?.items ?? []
  const priceOf = (id: number) => products?.content?.find(p => p.id === id)?.priceCents ?? 0
  const total = items.reduce((sum, i) => sum + priceOf(i.productId) * i.quantity, 0)

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Cart</h1>
      {items.length === 0 && <div>Your cart is empty.</div>}
      {items.map(i => (
        <div key={i.id} className="border p-2 mb-1 bg-white flex justify-between">
          <span>{products?.content?.find(p => p.id === i.productId)?.name ?? i.productId} x {i.quantity}</span>
          <span>{formatCents(priceOf(i.productId) * i.quantity)}</span>
        </div>
      ))}
      {items.length > 0 && (
        <>
          <div className="mt-2 font-semibold">Total: {formatCents(total)}</div>
          <Link to="/checkout" className="inline-block mt-4 px-3 py-1 bg-blue-600 text-white rounded">
            Checkout
          </Link>
        </>
      )}
    </div>
  )
}
