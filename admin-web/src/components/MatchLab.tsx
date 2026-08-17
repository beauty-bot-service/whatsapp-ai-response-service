import { useState, type FormEvent } from "react";
import { matchPromotions, type PromotionMatch } from "../api";

export default function MatchLab() {
  const [message, setMessage] = useState("Me interesa botox y rinomodelado");
  const [matches, setMatches] = useState<PromotionMatch[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      setMatches(await matchPromotions(message));
    } catch (matchError) {
      setError(matchError instanceof Error ? matchError.message : "No se pudo probar el mensaje.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="match-lab">
      <div className="lab-number">02</div>
      <div>
        <p className="eyebrow">LABORATORIO</p>
        <h3>Probá una consulta real</h3>
        <p>El resultado usa el mismo matcher que atiende WhatsApp.</p>
      </div>
      <form onSubmit={handleSubmit}>
        <textarea
          required
          maxLength={2000}
          rows={3}
          value={message}
          onChange={(event) => setMessage(event.target.value)}
        />
        <button className="dark-button" type="submit" disabled={busy}>
          {busy ? "Analizando…" : "Detectar promos"}
        </button>
      </form>
      {error && <div className="inline-error" role="alert">{error}</div>}
      {matches && (
        <div className="match-results" aria-live="polite">
          {matches.length === 0 ? (
            <span>No se detectaron promociones activas.</span>
          ) : (
            matches.map((match) => (
              <span key={match.id}><b>/{match.code}</b>{match.title}</span>
            ))
          )}
        </div>
      )}
    </section>
  );
}
