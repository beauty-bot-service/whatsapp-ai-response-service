import type { CSSProperties } from "react";
import type { Promotion } from "../api";

interface PromotionListProps {
  promotions: Promotion[];
  selectedId: number | null;
  loading: boolean;
  onSelect: (promotion: Promotion) => void;
}

const statusLabels = {
  DRAFT: "Borrador",
  ACTIVE: "Publicada",
  ARCHIVED: "Archivada"
} as const;

export default function PromotionList({
  promotions,
  selectedId,
  loading,
  onSelect
}: PromotionListProps) {
  if (loading) {
    return <div className="list-state">Actualizando catálogo…</div>;
  }
  if (promotions.length === 0) {
    return (
      <div className="list-state empty-state">
        <strong>No hay promociones acá.</strong>
        <span>Creá una nueva o cambiá los filtros.</span>
      </div>
    );
  }

  return (
    <div className="promotion-list">
      {promotions.map((promotion, index) => (
        <button
          className={`promotion-row ${selectedId === promotion.id ? "is-selected" : ""}`}
          key={promotion.id}
          onClick={() => onSelect(promotion)}
          style={{ "--row-index": index } as CSSProperties}
          type="button"
        >
          <span className={`status-dot status-${promotion.status.toLowerCase()}`} />
          <span className="promotion-row-main">
            <span className="promotion-title-line">
              <strong>{promotion.title}</strong>
              <small>{statusLabels[promotion.status]}</small>
            </span>
            <span className="promotion-code">/{promotion.code}</span>
            <span className="alias-line">
              {promotion.aliases.length > 0
                ? promotion.aliases.slice(0, 3).join(" · ")
                : "Sin alias adicionales"}
            </span>
          </span>
          <span className="row-arrow" aria-hidden="true">↗</span>
        </button>
      ))}
    </div>
  );
}
