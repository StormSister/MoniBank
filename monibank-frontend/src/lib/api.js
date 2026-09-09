const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
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
  const response = await fetch(apiUrl(path), {
    method,
    headers: {
      Accept: 'application/json',
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

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
    return body.message || body.detail || body.error || `Request failed with status ${status}`
  }
  return `Request failed with status ${status}`
}
