import { startTransition, useDeferredValue, useEffect, useState } from "react";
import {
  ApiError,
  listPromotions,
  logout,
  type AdminSession,
  type Promotion,
  type PromotionStatus
} from "../api";
import MatchLab from "./MatchLab";
import PromotionEditor from "./PromotionEditor";
import PromotionList from "./PromotionList";

interface DashboardProps {
  session: AdminSession;
  onLoggedOut: () => void;
}

const filters: Array<{ value: PromotionStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "Todas" },
  { value: "ACTIVE", label: "Publicadas" },
  { value: "DRAFT", label: "Borradores" },
  { value: "ARCHIVED", label: "Archivadas" }
];

export default function Dashboard({ session, onLoggedOut }: DashboardProps) {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [selected, setSelected] = useState<Promotion | null>(null);
  const [creating, setCreating] = useState(false);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<PromotionStatus | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const deferredQuery = useDeferredValue(query);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listPromotions(deferredQuery, status)
      .then((page) => {
        if (!active) return;
        setPromotions(page.content);
        setError(null);
        if (selected) {
          const refreshed = page.content.find((promotion) => promotion.id === selected.id);
          if (refreshed) setSelected(refreshed);
        }
      })
      .catch((loadError) => {
        if (!active) return;
        if (loadError instanceof ApiError && loadError.status === 401) {
          onLoggedOut();
          return;
        }
        setError(loadError instanceof Error ? loadError.message : "No se pudo cargar el catálogo.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [deferredQuery, status, revision]);

  const activeCount = promotions.filter((promotion) => promotion.currentlyActive).length;
  const draftCount = promotions.filter((promotion) => promotion.status === "DRAFT").length;

  function handleSaved(saved: Promotion) {
    setSelected(saved);
    setCreating(false);
    setRevision((current) => current + 1);
  }

  async function handleLogout() {
    try {
      await logout();
    } finally {
      onLoggedOut();
    }
  }

  return (
    <main className="dashboard-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">BB</span>
          <span>
            <strong>BeautyBot</strong>
            <small>Control comercial</small>
          </span>
        </div>

        <nav className="sidebar-nav" aria-label="Navegación principal">
          <button className="nav-item is-active" type="button"><span>01</span> Promociones</button>
          <button className="nav-item" type="button" disabled><span>02</span> Leads <i>pronto</i></button>
          <button className="nav-item" type="button" disabled><span>03</span> Conversaciones <i>pronto</i></button>
        </nav>

        <div className="sidebar-foot">
          <span className="user-avatar">{session.email.slice(0, 2).toUpperCase()}</span>
          <span className="user-copy">
            <strong>{session.email}</strong>
            <small>Clínica #{session.clinicId}</small>
          </span>
          <button className="logout-button" type="button" onClick={handleLogout} aria-label="Cerrar sesión">↗</button>
        </div>
      </aside>

      <section className="workspace">
        <header className="workspace-header">
          <div>
            <p className="eyebrow">RESPUESTAS COMERCIALES</p>
            <h1>Promociones</h1>
            <p>El catálogo activo que consulta el bot en cada mensaje.</p>
          </div>
          <button
            className="primary-button add-button"
            type="button"
            onClick={() => {
              setCreating(true);
              setSelected(null);
            }}
          >
            <span>＋</span> Nueva promoción
          </button>
        </header>

        <section className="catalog-overview" aria-label="Resumen del catálogo">
          <div className="metric primary-metric">
            <span>Activas ahora</span>
            <strong>{activeCount.toString().padStart(2, "0")}</strong>
            <small>Disponibles para responder</small>
          </div>
          <div className="metric">
            <span>Borradores</span>
            <strong>{draftCount.toString().padStart(2, "0")}</strong>
            <small>Pendientes de publicación</small>
          </div>
          <div className="metric metric-note">
            <span>Actualización</span>
            <p>Los cambios impactan en el siguiente mensaje, sin reiniciar el bot.</p>
          </div>
        </section>

        <div className="content-grid">
          <section className="catalog-panel">
            <div className="catalog-toolbar">
              <label className="search-box">
                <span aria-hidden="true">⌕</span>
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Buscar por título o código"
                />
              </label>
              <div className="filter-row" role="group" aria-label="Filtrar promociones">
                {filters.map((filter) => (
                  <button
                    className={status === filter.value ? "is-active" : ""}
                    key={filter.value}
                    onClick={() => startTransition(() => setStatus(filter.value))}
                    type="button"
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>
            {error && <div className="inline-error panel-error" role="alert">{error}</div>}
            <PromotionList
              promotions={promotions}
              selectedId={selected?.id ?? null}
              loading={loading}
              onSelect={(promotion) => {
                setCreating(false);
                setSelected(promotion);
              }}
            />
            <MatchLab />
          </section>

          <section className="editor-panel">
            {!creating && !selected ? (
              <div className="editor-welcome">
                <span className="welcome-index">01</span>
                <p className="eyebrow">EDITOR</p>
                <h2>Elegí una promoción para trabajar.</h2>
                <p>También podés crear un borrador nuevo y publicarlo cuando esté listo.</p>
                <button className="secondary-button" type="button" onClick={() => setCreating(true)}>
                  Crear la primera
                </button>
              </div>
            ) : (
              <PromotionEditor
                key={selected?.id ?? "new"}
                promotion={creating ? null : selected}
                onSaved={handleSaved}
              />
            )}
          </section>
        </div>
      </section>
    </main>
  );
}
