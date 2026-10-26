import { useQuery, useMutation } from '@tanstack/react-query'

type Product = { id: number; name: string; description: string; priceCents: number; active: boolean }

export default function Catalog() {
  const { data, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: async () => {
      const res = await fetch('/api/products')
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
      return res.json()
    }
  })

  if (isLoading) return <div>Loading...</div>
  const products: Product[] = data?.content ?? []
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Products</h1>
      {products.map(p => (
        <div key={p.id} className="border p-4 mb-2 rounded bg-white">
          <div className="font-semibold">{p.name}</div>
          <div className="text-sm text-gray-600">{p.description}</div>
          <div className="mt-1">Price: {p.priceCents} cents</div>
          <div>Active: {String(p.active)}</div>
          <button
            className="mt-2 px-3 py-1 bg-blue-600 text-white rounded"
            onClick={() => addMutation.mutate(p.id)}
          >
            Add to cart
          </button>
        </div>
      ))}
      {addMutation.isSuccess && <div className="text-green-600 mt-2">Added to cart</div>}
    </div>
  )
}
