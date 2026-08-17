import { useState, type FormEvent } from "react";
import type { AdminSession } from "../api";
import { login } from "../api";

interface LoginPageProps {
  initialError: string | null;
  onAuthenticated: (session: AdminSession) => void;
}

export default function LoginPage({ initialError, onAuthenticated }: LoginPageProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(initialError);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onAuthenticated(await login(email.trim(), password));
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "No se pudo iniciar sesión.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="login-shell">
      <section className="login-story" aria-label="BeautyBot promociones">
        <div className="login-brand">
          <span className="brand-mark">BB</span>
          <span>BeautyBot / Admin</span>
        </div>
        <div className="login-headline">
          <p className="eyebrow">CATÁLOGO COMERCIAL</p>
          <h1>La promo correcta, en el momento exacto.</h1>
          <p>
            Publicá, programá y probá las respuestas que el bot utiliza en cada conversación.
          </p>
        </div>
        <div className="login-example" aria-hidden="true">
          <span className="example-label">Mensaje detectado</span>
          <p>“Me interesa botox y rinomodelado”</p>
          <div className="signal-line">
            <span />
            <small>2 promociones encontradas</small>
          </div>
        </div>
      </section>

      <section className="login-panel">
        <form className="login-form" onSubmit={handleSubmit}>
          <p className="eyebrow">ACCESO PRIVADO</p>
          <h2>Ingresá al panel</h2>
          <p className="form-intro">Usá el usuario administrador configurado para la clínica.</p>

          <label>
            Email
            <input
              autoComplete="username"
              inputMode="email"
              required
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="admin@clinica.com"
            />
          </label>

          <label>
            Contraseña
            <input
              autoComplete="current-password"
              required
              minLength={12}
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••••••••"
            />
          </label>

          {error && <div className="inline-error" role="alert">{error}</div>}

          <button className="primary-button login-button" type="submit" disabled={busy}>
            {busy ? "Verificando…" : "Entrar al catálogo"}
          </button>
          <small className="security-note">Sesión protegida y limitada a tu clínica.</small>
        </form>
      </section>
    </main>
  );
}
