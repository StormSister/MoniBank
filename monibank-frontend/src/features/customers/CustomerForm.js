export const EMPTY_CUSTOMER_FORM = Object.freeze({
  countryCode: 'PL',
  nationalId: '',
  firstName: '',
  lastName: '',
  dateOfBirth: '',
})

const NAME_PATTERN = /^[A-Za-z][A-Za-z .'-]*$/

export function newCustomerForm() {
  return { ...EMPTY_CUSTOMER_FORM }
}

export function sanitizeCustomerField(field, rawValue) {
  if (field === 'countryCode') return rawValue.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 2)
  if (field === 'nationalId') return rawValue.replace(/\D/g, '').slice(0, 11)
  return rawValue
}

export function validateCustomerForm(form) {
  const errors = {}
  if (!/^[A-Z]{2}$/.test(form.countryCode)) errors.countryCode = 'Enter exactly two capital letters.'
  if (!/^\d{11}$/.test(form.nationalId)) errors.nationalId = 'National ID must contain exactly 11 digits.'
  if (!form.firstName.trim() || form.firstName.length > 30 || !NAME_PATTERN.test(form.firstName)) errors.firstName = 'Use 1–30 letters and the characters space, apostrophe, hyphen or dot.'
  if (!form.lastName.trim() || form.lastName.length > 40 || !NAME_PATTERN.test(form.lastName)) errors.lastName = 'Use 1–40 letters and the characters space, apostrophe, hyphen or dot.'
  if (!form.dateOfBirth) errors.dateOfBirth = 'Date of birth is required.'
  else if (form.dateOfBirth > todayIso()) errors.dateOfBirth = 'Date of birth cannot be in the future.'
  return errors
}

export function customerRequest(form) {
  return { ...form, dateOfBirth: form.dateOfBirth.replaceAll('-', '') }
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10)
}
