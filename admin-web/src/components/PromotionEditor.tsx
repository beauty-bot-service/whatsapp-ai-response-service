import { useState, type FormEvent } from "react";
import {
  activatePromotion,
  archivePromotion,
  createPromotion,
  updatePromotion,
  type Promotion,
  type PromotionPayload
} from "../api";
import WhatsAppPreview from "./WhatsAppPreview";

interface PromotionEditorProps {
  promotion: Promotion | null;
  onSaved: (promotion: Promotion) => void;
}

interface Draft {
  code: string;
  title: string;
  messageBody: string;
  aliases: string;
  validFrom: string;
  validUntil: string;
}

function toLocalDateTime(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function initialDraft(promotion: Promotion | null): Draft {
  return {
    code: promotion?.code ?? "",
    title: promotion?.title ?? "",
    messageBody: promotion?.messageBody ?? "",
    aliases: promotion?.aliases.join(", ") ?? "",
    validFrom: toLocalDateTime(promotion?.validFrom ?? null),
    validUntil: toLocalDateTime(promotion?.validUntil ?? null)
  };
}

function statusLabel(promotion: Promotion | null): string {
  if (!promotion) return "Nueva";
  if (promotion.status === "DRAFT") return "Borrador";
  if (promotion.status === "ARCHIVED") return "Archivada";
  return promotion.currentlyActive ? "Publicada" : "Programada";
}

export default function PromotionEditor({ promotion, onSaved }: PromotionEditorProps) {
  const [draft, setDraft] = useState<Draft>(() => initialDraft(promotion));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function updateField<K extends keyof Draft>(field: K, value: Draft[K]) {
    setDraft((current) => ({ ...current, [field]: value }));
  }

  function payload(): PromotionPayload {
    const aliases = draft.aliases
      .split(/[,\n]/)
      .map((alias) => alias.trim())
      .filter(Boolean);
    return {
      code: draft.code,
      title: draft.title,
      messageBody: draft.messageBody,
      aliases: [...new Set(aliases)],
      validFrom: draft.validFrom ? new Date(draft.validFrom).toISOString() : null,
      validUntil: draft.validUntil ? new Date(draft.validUntil).toISOString() : null
    };
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const saved = promotion
        ? await updatePromotion(promotion, payload())
        : await createPromotion(payload());
      onSaved(saved);
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "No se pudo guardar la promoción.");
    } finally {
      setBusy(false);
    }
  }

  async function changePublication(action: "activate" | "archive") {
    if (!promotion) return;
    if (action === "archive" && !window.confirm("La promoción dejará de utilizarse inmediatamente. ¿Continuar?")) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const saved = action === "activate"
        ? await activatePromotion(promotion)
        : await archivePromotion(promotion);
      onSaved(saved);
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "No se pudo cambiar el estado.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="editor-layout">
      <form className="promotion-form" onSubmit={handleSubmit}>
        <div className="editor-header">
          <div>
            <p className="eyebrow">{promotion ? `ID ${promotion.id}` : "NUEVO REGISTRO"}</p>
            <h2>{promotion ? "Editar promoción" : "Crear promoción"}</h2>
          </div>
          <span className={`editor-status ${promotion?.status.toLowerCase() ?? "new"}`}>
            {statusLabel(promotion)}
          </span>
        </div>

        <div className="field-grid two-columns">
          <label className="code-field">
            Código de respuesta
            <span className="input-with-prefix">
              <i>/</i>
              <input
                required
                maxLength={50}
                value={draft.code}
                onChange={(event) => updateField("code", event.target.value)}
                placeholder="botox"
              />
            </span>
            <small>Identificador único, sin espacios.</small>
          </label>

          <label>
            Título interno
            <input
              required
              maxLength={120}
              value={draft.title}
              onChange={(event) => updateField("title", event.target.value)}
              placeholder="Promo Botox Agosto"
            />
            <small>Visible en el panel y en la vista previa.</small>
          </label>
        </div>

        <label>
          Alias de detección
          <input
            value={draft.aliases}
            onChange={(event) => updateField("aliases", event.target.value)}
            placeholder="toxina, dysport, arrugas"
          />
          <small>Separalos con comas. El código también se detecta automáticamente.</small>
        </label>

        <label>
          Respuesta exacta
          <textarea
            required
            maxLength={1800}
            rows={10}
            value={draft.messageBody}
            onChange={(event) => updateField("messageBody", event.target.value)}
            placeholder="Pegá acá el texto completo que debe recibir el cliente…"
          />
          <span className="character-count">{draft.messageBody.length} / 1800</span>
        </label>

        <fieldset className="schedule-fieldset">
          <legend>Vigencia opcional</legend>
          <div className="field-grid two-columns">
            <label>
              Disponible desde
              <input
                type="datetime-local"
                value={draft.validFrom}
                onChange={(event) => updateField("validFrom", event.target.value)}
              />
            </label>
            <label>
              Disponible hasta
              <input
                type="datetime-local"
                value={draft.validUntil}
                onChange={(event) => updateField("validUntil", event.target.value)}
              />
            </label>
          </div>
          <small>Sin fechas, permanece disponible hasta que la archives.</small>
        </fieldset>

        {error && <div className="inline-error" role="alert">{error}</div>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={busy}>
            {busy ? "Guardando…" : promotion ? "Guardar cambios" : "Crear borrador"}
          </button>
          {promotion && promotion.status !== "ACTIVE" && (
            <button className="secondary-button" type="button" disabled={busy} onClick={() => changePublication("activate")}>
              Publicar
            </button>
          )}
          {promotion && promotion.status !== "ARCHIVED" && (
            <button className="text-button danger-button" type="button" disabled={busy} onClick={() => changePublication("archive")}>
              Archivar
            </button>
          )}
        </div>
      </form>

      <WhatsAppPreview title={draft.title} messageBody={draft.messageBody} />
    </div>
  );
}
