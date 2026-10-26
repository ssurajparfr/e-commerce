import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

type CartItem = { id: number; productId: number; quantity: number }
type Cart = { id: number; items: CartItem[] }
type Product = { id: number; name: string; priceCents: number }

export default function Cart() {
  const { data: cart } = useQuery<Cart>({
    queryKey: ['cart'],
    queryFn: async () => (await fetch('/api/cart')).json()
  })
  const { data: products } = useQuery<{ content: Product[] }>({
    queryKey: ['products'],
    queryFn: async () => (await fetch('/api/products')).json()
  })
  const nameOf = (id: number) => products?.content?.find(p => p.id === id)?.name ?? id
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Cart</h1>
      {(cart?.items ?? []).map(i => (
        <div key={i.id} className="border p-2 mb-1 bg-white">
          {nameOf(i.productId)} x {i.quantity}
        </div>
      ))}
      {(cart?.items ?? []).length === 0 && <div>Empty cart</div>}
      <Link to="/checkout" className="inline-block mt-4 px-3 py-1 bg-green-600 text-white rounded">Checkout</Link>
    </div>
  )
}
