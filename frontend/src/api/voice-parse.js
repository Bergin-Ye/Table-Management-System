import request from './request'

/**
 * 语音/文字解析 — 将口语化文字拆解为表单字段
 * @param {string} text 语音输入的文字
 * @param {string} table 表类型: delivery-record | original-record | machine-material | delivery-stats | settlement-machine | machine-detail | machine-count | material
 */
export function parseVoiceText(text, table) {
  return request.post('/voice-parse', { text, table })
}
