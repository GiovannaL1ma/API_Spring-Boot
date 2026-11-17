import { useCallback, useEffect, useState } from "react"
import { getApiKey } from "./api"

// busca simples: { data, error, loading, reload }
export function useFetch<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | undefined>()
  const [error, setError] = useState<Error | undefined>()
  const [loading, setLoading] = useState(true)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let alive = true
    setLoading(true)
    fn()
      .then((d) => {
        if (!alive) return
        setData(d)
        setError(undefined)
      })
      .catch((e) => alive && setError(e))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  return { data, error, loading, reload }
}

export function useApiKey() {
  const [key, setKey] = useState(getApiKey)
  useEffect(() => {
    const on = () => setKey(getApiKey())
    window.addEventListener("api-key-change", on)
    return () => window.removeEventListener("api-key-change", on)
  }, [])
  return key
}

export function useDebounced<T>(value: T, ms = 300) {
  const [v, setV] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms)
    return () => clearTimeout(t)
  }, [value, ms])
  return v
}
