const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.code = body && typeof body === 'object' ? body.code : undefined
    this.requestId = body && typeof body === 'object' ? body.requestId : undefined
    this.operation = body && typeof body === 'object' ? body.operation : undefined
    this.retryable = Boolean(body && typeof body === 'object' && body.retryable)
    this.fieldErrors = body && typeof body === 'object' ? body.fieldErrors || {} : {}
  }
}

export function apiUrl(path) {
  return `${API_BASE_URL}${path}`
}

export async function apiGet(path, { signal } = {}) {
  return apiRequest(path, { method: 'GET', signal })
}

export async function apiPost(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'POST', body })
}

export async function apiPatch(path, body, options = {}) {
  return apiRequest(path, { ...options, method: 'PATCH', body })
}

async function apiRequest(path, { method, body, signal }) {
  let response

  try {
    response = await fetch(apiUrl(path), {
      method,
      headers: {
        Accept: 'application/json',
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError') throw error

    const networkError = {
      code: 'NETWORK_ERROR',
      message: 'The MoniBank backend is unavailable. Check the connection and try again.',
      retryable: true,
    }

    throw new ApiError(networkError.message, 0, networkError)
  }

  if (!response.ok) {
    const errorBody = await readBody(response)
    throw new ApiError(errorMessage(errorBody, response.status), response.status, errorBody)
  }

  if (response.status === 204) return null
  return readBody(response)
}

async function readBody(response) {
  const text = await response.text()
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function errorMessage(body, status) {
  if (typeof body === 'string' && body.trim()) return body
  if (body && typeof body === 'object') {
    const base = body.message || body.detail || body.error || `Request failed with status ${status}`
    const fields = body.fieldErrors && typeof body.fieldErrors === 'object'
      ? [...new Set(Object.values(body.fieldErrors).filter(Boolean))]
      : []
    const fieldDetail = fields.length ? ` ${fields.join(' ')}` : ''
    const reference = body.requestId ? ` Reference: ${body.requestId}.` : ''
    return `${base}${fieldDetail}${reference}`
  }
  return `Request failed with status ${status}`
}
