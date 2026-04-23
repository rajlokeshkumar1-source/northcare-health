export function downloadCsv<T extends Record<string, unknown>>(
  rows: T[],
  filename: string
): void {
  if (rows.length === 0) return
  const keys = Object.keys(rows[0])
  const escape = (v: unknown): string => {
    if (v === null || v === undefined) return ''
    const s = Array.isArray(v) ? v.join('; ') : String(v)
    return s.includes(',') || s.includes('"') || s.includes('\n')
      ? `"${s.replace(/"/g, '""')}"` : s
  }
  const header = keys.join(',')
  const lines = rows.map(row => keys.map(k => escape(row[k])).join(','))
  const csv = [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
