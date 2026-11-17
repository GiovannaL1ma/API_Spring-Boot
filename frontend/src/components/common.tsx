import { useState, type ReactNode } from "react"
import { Link } from "react-router-dom"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ApiError, type Page } from "@/lib/api"
import { useApiKey } from "@/lib/hooks"
import { cn } from "@/lib/utils"

export function showError(e: unknown) {
  if (e instanceof ApiError) {
    const fields = e.errors ? Object.entries(e.errors).map(([k, v]) => `${k}: ${v}`) : []
    toast.error(e.status ? `Erro ${e.status}` : "Sem conexão", {
      description: fields.length ? fields.join(" · ") : e.message,
    })
  } else {
    toast.error("Algo deu errado", { description: String(e) })
  }
}

export function PageHeader({ title, count, children }: { title: string; count?: number; children?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3 border-b pb-4 mb-6">
      <h1 className="text-2xl font-semibold tracking-tight">
        {title}
        {count !== undefined && <span className="ml-2 text-base font-normal text-muted-foreground tnum">{count}</span>}
      </h1>
      <div className="flex flex-wrap items-center gap-2">{children}</div>
    </div>
  )
}

// botao de escrita: sem chave, avisa em vez de abrir o formulario
export function WriteButton({ onClick, children, ...props }: React.ComponentProps<typeof Button>) {
  const key = useApiKey()
  return (
    <Button
      {...props}
      onClick={(e) => {
        if (!key) {
          toast("Precisa de uma chave de API", { description: "Gere ou cole uma chave no botão Chave, lá em cima." })
          return
        }
        onClick?.(e)
      }}
    >
      {children}
    </Button>
  )
}

export function Field({
  label,
  error,
  children,
  hint,
}: {
  label: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="grid gap-1.5">
      <Label className={cn(error && "text-destructive")}>{label}</Label>
      {children}
      {error ? (
        <p className="text-xs text-destructive">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted-foreground">{hint}</p>
      ) : null}
    </div>
  )
}

export function TextField({
  label,
  value,
  onChange,
  error,
  hint,
  ...props
}: { label: string; value: string | number; onChange: (v: string) => void; error?: string; hint?: string } & Omit<
  React.ComponentProps<"input">,
  "value" | "onChange"
>) {
  return (
    <Field label={label} error={error} hint={hint}>
      <Input value={value} onChange={(e) => onChange(e.target.value)} aria-invalid={!!error} {...props} />
    </Field>
  )
}

// dialog de formulario: cuida do "salvando..." e passa os erros de campo da API para o form
export function FormDialog({
  open,
  onOpenChange,
  title,
  description,
  onSubmit,
  children,
  submitLabel = "Salvar",
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  title: string
  description?: string
  onSubmit: () => Promise<void>
  children: ReactNode
  submitLabel?: string
}) {
  const [saving, setSaving] = useState(false)
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <form
          className="grid gap-4"
          onSubmit={async (e) => {
            e.preventDefault()
            setSaving(true)
            try {
              await onSubmit()
              onOpenChange(false)
            } catch (err) {
              showError(err)
            } finally {
              setSaving(false)
            }
          }}
        >
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            {description && <DialogDescription>{description}</DialogDescription>}
          </DialogHeader>
          {children}
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Salvando…" : submitLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function ConfirmDelete({
  open,
  onOpenChange,
  what,
  extra,
  onConfirm,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  what: string
  extra?: string
  onConfirm: () => Promise<void>
}) {
  const [busy, setBusy] = useState(false)
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Excluir {what}?</DialogTitle>
          <DialogDescription>{extra ?? "Não dá pra desfazer."}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={busy}
            onClick={async () => {
              setBusy(true)
              try {
                await onConfirm()
                onOpenChange(false)
              } catch (e) {
                showError(e)
              } finally {
                setBusy(false)
              }
            }}
          >
            {busy ? "Excluindo…" : "Excluir"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function Pager({ page, onPage }: { page?: Page; onPage: (n: number) => void }) {
  if (!page || page.totalPages <= 1) return null
  return (
    <div className="flex items-center justify-end gap-3 pt-4 text-sm text-muted-foreground">
      <span className="tnum">
        página {page.number + 1} de {page.totalPages}
      </span>
      <Button variant="outline" size="sm" disabled={page.number === 0} onClick={() => onPage(page.number - 1)}>
        Anterior
      </Button>
      <Button
        variant="outline"
        size="sm"
        disabled={page.number + 1 >= page.totalPages}
        onClick={() => onPage(page.number + 1)}
      >
        Próxima
      </Button>
    </div>
  )
}

// escolha de varios itens (artistas, generos) como etiquetas clicaveis
export function MultiPick<T extends { id: number; name: string }>({
  options,
  value,
  onChange,
}: {
  options: T[]
  value: number[]
  onChange: (ids: number[]) => void
}) {
  if (!options.length) return <p className="text-sm text-muted-foreground">Nada cadastrado ainda.</p>
  return (
    <div className="flex max-h-36 flex-wrap gap-1.5 overflow-y-auto rounded-md border p-2">
      {options.map((o) => {
        const on = value.includes(o.id)
        return (
          <button
            type="button"
            key={o.id}
            onClick={() => onChange(on ? value.filter((v) => v !== o.id) : [...value, o.id])}
            className={cn(
              "rounded-sm border px-2 py-0.5 text-sm transition-colors",
              on ? "border-foreground bg-foreground text-background" : "hover:bg-muted",
            )}
          >
            {o.name}
          </button>
        )
      })}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return (
    <div className="rounded-md border border-dashed p-8 text-center">
      <p className="font-medium">Não carregou.</p>
      <p className="mt-1 text-sm text-muted-foreground">{error.message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" className="mt-4" onClick={onRetry}>
          Tentar de novo
        </Button>
      )}
    </div>
  )
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="rounded-md border border-dashed p-8 text-center text-sm text-muted-foreground">{children}</p>
}

export function ArtistLinks({ artists }: { artists: { id: number; name: string }[] }) {
  return (
    <>
      {artists.map((a, i) => (
        <span key={a.id}>
          {i > 0 && ", "}
          <Link to={`/artistas/${a.id}`} className="hover:underline underline-offset-2">
            {a.name}
          </Link>
        </span>
      ))}
    </>
  )
}
