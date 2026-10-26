import { describe, it, expect } from 'vitest'
import { formatCents } from './money'

describe('formatCents', () => {
  it('formats EUR cents', () => {
    expect(formatCents(1000)).toBe('€10.00')
    expect(formatCents(1200)).toBe('€12.00')
  })
})
