import { Routes, Route, Link } from 'react-router-dom'
import Catalog from './pages/Catalog'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import Confirmation from './pages/Confirmation'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="p-4 bg-white shadow flex gap-4">
        <Link to="/">Catalog</Link>
        <Link to="/cart">Cart</Link>
      </nav>
      <main className="p-4">
        <Routes>
          <Route path="/" element={<Catalog />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders/:id" element={<Confirmation />} />
        </Routes>
      </main>
    </div>
  )
}
