import Panel from '../components/ui/Panel.jsx'
import Button from '../components/ui/Button.jsx'
import FormField, { Input, Select } from '../components/ui/FormField.jsx'

export default function PlaceholderPage({ title }) {
  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div><h1 className="text-2xl font-semibold">{title}</h1><p className="mt-1 text-sm text-mb-muted">This screen is prepared for the next frontend slice.</p></div>
      <Panel title={`${title} form components`}>
        <form className="grid gap-4 p-5 md:grid-cols-2" onSubmit={(event) => event.preventDefault()}>
          <FormField id="example-id" label="Identifier" hint="Fixed-width mainframe identifier"><Input id="example-id" placeholder="A000000000002" /></FormField>
          <FormField id="example-status" label="Status"><Select id="example-status" defaultValue="A"><option value="A">Active</option><option value="I">Inactive</option></Select></FormField>
          <div className="flex gap-2 md:col-span-2"><Button variant="primary">Save changes</Button><Button>Cancel</Button></div>
        </form>
      </Panel>
    </div>
  )
}
