import FormField, { Input } from '../ui/FormField.jsx'
import { todayIso } from '../../features/customers/CustomerForm.js'

export default function CustomerFormFields({ form, errors, onChange, idPrefix = 'customer' }) {
  const field = (name) => `${idPrefix}-${name}`

  return (
    <>
      <FormField id={field('countryCode')} label="Country code" error={errors.countryCode} hint="Two-letter ISO code, for example PL.">
        <Input id={field('countryCode')} value={form.countryCode} onChange={(event) => onChange('countryCode', event.target.value)} maxLength="2" autoComplete="country" />
      </FormField>
      <FormField id={field('nationalId')} label="National ID" error={errors.nationalId} hint="Exactly 11 digits.">
        <Input id={field('nationalId')} value={form.nationalId} onChange={(event) => onChange('nationalId', event.target.value)} inputMode="numeric" maxLength="11" autoComplete="off" />
      </FormField>
      <FormField id={field('firstName')} label="First name" error={errors.firstName}>
        <Input id={field('firstName')} value={form.firstName} onChange={(event) => onChange('firstName', event.target.value)} maxLength="30" autoComplete="given-name" />
      </FormField>
      <FormField id={field('lastName')} label="Last name" error={errors.lastName}>
        <Input id={field('lastName')} value={form.lastName} onChange={(event) => onChange('lastName', event.target.value)} maxLength="40" autoComplete="family-name" />
      </FormField>
      <FormField id={field('dateOfBirth')} label="Date of birth" error={errors.dateOfBirth}>
        <Input id={field('dateOfBirth')} type="date" value={form.dateOfBirth} onChange={(event) => onChange('dateOfBirth', event.target.value)} max={todayIso()} autoComplete="bday" />
      </FormField>
    </>
  )
}
