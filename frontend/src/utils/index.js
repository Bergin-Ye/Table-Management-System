import dayjs from 'dayjs'

/**
 * 格式化日期
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return ''
  return dayjs(date).format(format)
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date) {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * 从日期计算 yearMonth (FYyyMM 格式)
 */
export function getYearMonth(date) {
  if (!date) return ''
  const d = dayjs(date)
  const year = String(d.year()).slice(2)
  const month = String(d.month() + 1).padStart(2, '0')
  return `FY${year}${month}`
}

/**
 * 下载 Blob 文件
 */
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 获取当前月份天数
 */
export function getDaysInMonth(yearMonth) {
  if (!yearMonth) return 31
  const year = 2000 + parseInt(yearMonth.slice(2, 4))
  const month = parseInt(yearMonth.slice(4, 6))
  return dayjs(`${year}-${String(month).padStart(2, '0')}-01`).daysInMonth()
}
