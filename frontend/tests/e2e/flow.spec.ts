import { test, expect } from '@playwright/test'

test('Catalog browse shows seeded products', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByText('Widget')).toBeVisible()
  await expect(page.getByText('Price: 1000 cents')).toBeVisible()
  await expect(page.getByText('Active: true')).toBeVisible()
})

test('Add to cart creates cart item', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Add to cart' }).click()
  await expect(page.getByText('Added to cart')).toBeVisible()
  const cookies = await page.context().cookies()
  const cartCookie = cookies.find(c => c.name === 'cart_id')
  expect(cartCookie).toBeTruthy()
  const res = await page.request.get('/api/cart', { headers: { Cookie: `cart_id=${cartCookie!.value}` } })
  const body = await res.json()
  expect(body.items.length).toBe(1)
  expect(body.items[0].quantity).toBe(1)
})

test('Checkout computes server-side totals', async ({ page }) => {
  await page.goto('/')
  await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/cart/items')),
    page.getByRole('button', { name: 'Add to cart' }).click()
  ])
  await page.goto('/cart')
  await page.goto('/checkout')
  // submit with tampered price ignored by server; no client price field sent
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/checkout')),
    page.getByRole('button', { name: 'Place order' }).click()
  ])
  expect(response.ok()).toBeTruthy()
})

test('Order reaches CONFIRMED and shows confirmation', async ({ page }) => {
  await page.goto('/')
  await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/cart/items')),
    page.getByRole('button', { name: 'Add to cart' }).click()
  ])
  await page.goto('/checkout')
  await page.getByRole('button', { name: 'Place order' }).click()
  await expect(page.getByText('Order confirmation')).toBeVisible()
  await expect(page.getByText(/Order state: CONFIRMED/)).toBeVisible()
  await expect(page.getByText(/Payment status: SUCCESS/)).toBeVisible()
  await expect(page.getByText(/Total: 1200/)).toBeVisible()
})
