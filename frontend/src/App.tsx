import { BrowserRouter, Link, Route, Routes } from "react-router-dom"
import { Layout } from "@/components/layout"
import { Toaster } from "@/components/ui/sonner"
import { AlbumPage, AlbumsPage, TracksPage } from "@/pages/albums"
import { ArtistPage, ArtistsPage, GenresPage, LabelsPage } from "@/pages/catalog"

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<AlbumsPage />} />
          <Route path="albuns/:id" element={<AlbumPage />} />
          <Route path="artistas" element={<ArtistsPage />} />
          <Route path="artistas/:id" element={<ArtistPage />} />
          <Route path="faixas" element={<TracksPage />} />
          <Route path="generos" element={<GenresPage />} />
          <Route path="gravadoras" element={<LabelsPage />} />
          <Route
            path="*"
            element={
              <p className="text-muted-foreground">
                Página não encontrada. <Link to="/" className="underline">Voltar</Link>
              </p>
            }
          />
        </Route>
      </Routes>
      <Toaster position="bottom-right" />
    </BrowserRouter>
  )
}
