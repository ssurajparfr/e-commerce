import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { formatCents } from '../lib/money'

type Product = { id: number; name: string; description: string; priceCents: number; active: boolean }

export default function Catalog() {
  const queryClient = useQueryClient()
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['products'],
    queryFn: async () => {
      const res = await fetch('/api/products')
      if (!res.ok) throw new Error('Failed to load products')
      return res.json()
    }
  })
  const addMutation = useMutation({
    mutationFn: async (productId: number) => {
      const res = await fetch('/api/cart/items', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, quantity: 1 })
      })
      if (!res.ok) throw new Error('Could not add to cart')
      return res.json()
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] })
  })

  if (isLoading) return <div>Loading products...</div>
  if (isError) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-4">Products</h1>
        <p className="text-red-600">Could not load products.</p>
        <button className="mt-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    )
  }
  const products: Product[] = data?.content ?? []
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Products</h1>
      {products.map(p => (
        <div key={p.id} className="border p-4 mb-2 rounded bg-white">
          <div className="font-semibold">{p.name}</div>
          <div className="text-sm text-gray-600">{p.description}</div>
          <div className="mt-1">Price: {formatCents(p.priceCents)}</div>
          <div>Active: {String(p.active)}</div>
          <button
            className="mt-2 px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
            disabled={addMutation.isPending}
            aria-label={`Add ${p.name} to cart`}
            onClick={() => addMutation.mutate(p.id)}
          >
            Add to cart
          </button>
        </div>
      ))}
      {addMutation.isError && <div className="text-red-600 mt-2">Could not add to cart, try again.</div>}
      {addMutation.isSuccess && <div className="text-green-700 mt-2">Added to cart</div>}
    </div>
  )
}
