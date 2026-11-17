import { useEffect, useState } from "react"
import { NavLink, Outlet } from "react-router-dom"
import { toast } from "sonner"
import { Field, TextField } from "@/components/common"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { API_URL, ApiError, api, setApiKey, type ApiKeyInfo } from "@/lib/api"
import { useApiKey } from "@/lib/hooks"
import { cn } from "@/lib/utils"
import { showError } from "./common"

const NAV = [
  { to: "/", label: "Álbuns", end: true },
  { to: "/artistas", label: "Artistas" },
  { to: "/faixas", label: "Faixas" },
  { to: "/generos", label: "Gêneros" },
  { to: "/gravadoras", label: "Gravadoras" },
]

export function Layout() {
  const key = useApiKey()
  const [keyOpen, setKeyOpen] = useState(false)
  const [rate, setRate] = useState<{ remaining: number; limit: number }>()

  useEffect(() => {
    const on = (e: Event) => setRate((e as CustomEvent).detail)
    window.addEventListener("rate-limit", on)
    return () => window.removeEventListener("rate-limit", on)
  }, [])

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b bg-background sticky top-0 z-10">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 flex h-14 items-center gap-6">
          <NavLink to="/" className="flex items-center gap-2 font-semibold tracking-tight shrink-0">
            <span className="inline-block size-3 rounded-full bg-brand ring-4 ring-brand/15" aria-hidden />
            Discoteca
          </NavLink>
          <nav className="flex gap-1 overflow-x-auto text-sm -mx-1">
            {NAV.map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                end={n.end}
                className={({ isActive }) =>
                  cn(
                    "rounded-sm px-2.5 py-1.5 whitespace-nowrap text-muted-foreground hover:text-foreground",
                    isActive && "text-foreground bg-muted font-medium",
                  )
                }
              >
                {n.label}
              </NavLink>
            ))}
          </nav>
          <Button
            variant={key ? "outline" : "default"}
            size="sm"
            className="ml-auto shrink-0"
            onClick={() => setKeyOpen(true)}
          >
            {key ? <span className="font-mono text-xs">{key.slice(0, 8)}…</span> : "Chave de API"}
          </Button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 sm:px-6 py-8">
        <Outlet />
      </main>

      <footer className="border-t text-xs text-muted-foreground">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 py-3 flex flex-wrap gap-x-4 gap-y-1 justify-between">
          <span className="font-mono">{API_URL}</span>
          <span className="flex gap-4">
            {rate && (
              <span className="tnum">
                {rate.remaining}/{rate.limit} req. neste minuto
              </span>
            )}
            <a href={`${API_URL}/swagger-ui.html`} target="_blank" rel="noreferrer" className="hover:text-foreground">
              Swagger
            </a>
          </span>
        </div>
      </footer>

      <KeyDialog open={keyOpen} onOpenChange={setKeyOpen} />
    </div>
  )
}

function KeyDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const key = useApiKey()
  const [me, setMe] = useState<ApiKeyInfo>()
  const [paste, setPaste] = useState("")
  const [ownerName, setOwnerName] = useState("")
  const [ownerEmail, setOwnerEmail] = useState("")
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open || !key) return setMe(undefined)
    api<ApiKeyInfo>("/api-keys/me")
      .then(setMe)
      .catch((e) => {
        setMe(undefined)
        if (e instanceof ApiError && e.status === 401) toast.error("Essa chave não vale mais", { description: e.message })
      })
  }, [open, key])

  async function generate(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setErrors({})
    try {
      setApiKey("") // gera sem mandar chave antiga
      const created = await api<ApiKeyInfo>("/api-keys", { method: "POST", body: { ownerName, ownerEmail } })
      if (created.apiKey) setApiKey(created.apiKey)
      toast.success("Chave gerada e salva neste navegador")
      setOwnerName("")
      setOwnerEmail("")
    } catch (err) {
      if (err instanceof ApiError && err.errors) setErrors(err.errors)
      showError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Chave de API</DialogTitle>
          <DialogDescription>
            Pra ler não precisa. Pra cadastrar, editar ou excluir, sim. Ela fica salva só neste navegador.
          </DialogDescription>
        </DialogHeader>

        {key ? (
          <div className="grid gap-4">
            <div className="rounded-md bg-muted p-3 font-mono text-xs break-all">{key}</div>
            {me && (
              <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
                <dt className="text-muted-foreground">Dono</dt>
                <dd>{me.ownerName}</dd>
                <dt className="text-muted-foreground">E-mail</dt>
                <dd>{me.ownerEmail}</dd>
                <dt className="text-muted-foreground">Nível</dt>
                <dd>
                  <Badge variant="outline">{me.role}</Badge>
                </dd>
              </dl>
            )}
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => navigator.clipboard?.writeText(key)}>
                Copiar
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setApiKey("")
                  toast("Chave removida deste navegador")
                }}
              >
                Esquecer chave
              </Button>
            </div>
          </div>
        ) : (
          <Tabs defaultValue="gerar">
            <TabsList className="w-full">
              <TabsTrigger value="gerar">Gerar nova</TabsTrigger>
              <TabsTrigger value="colar">Já tenho uma</TabsTrigger>
            </TabsList>
            <TabsContent value="gerar">
              <form className="grid gap-3 pt-2" onSubmit={generate}>
                <TextField label="Seu nome" value={ownerName} onChange={setOwnerName} error={errors.ownerName} />
                <TextField
                  label="E-mail"
                  type="email"
                  value={ownerEmail}
                  onChange={setOwnerEmail}
                  error={errors.ownerEmail}
                />
                <Button type="submit" disabled={busy}>
                  {busy ? "Gerando…" : "Gerar chave"}
                </Button>
              </form>
            </TabsContent>
            <TabsContent value="colar">
              <form
                className="grid gap-3 pt-2"
                onSubmit={(e) => {
                  e.preventDefault()
                  if (!paste.trim()) return
                  setApiKey(paste.trim())
                  setPaste("")
                  toast.success("Chave salva")
                }}
              >
                <Field label="Chave" hint="Começa com mca_ (ou a chave ADMIN do log da API)">
                  <Input value={paste} onChange={(e) => setPaste(e.target.value)} className="font-mono text-xs" />
                </Field>
                <Button type="submit">Usar esta chave</Button>
              </form>
            </TabsContent>
          </Tabs>
        )}
        <Separator />
        <p className="text-xs text-muted-foreground">
          A chave só aparece uma vez, quando é gerada. A API guarda apenas o hash dela.
        </p>
      </DialogContent>
    </Dialog>
  )
}
