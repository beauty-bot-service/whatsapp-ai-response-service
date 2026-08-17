import { useEffect, useState } from "react";
import type { AdminSession } from "./api";
import { getSession } from "./api";
import Dashboard from "./components/Dashboard";
import LoginPage from "./components/LoginPage";

export default function App() {
  const [session, setSession] = useState<AdminSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [startupError, setStartupError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getSession()
      .then((currentSession) => {
        if (active) {
          setSession(currentSession);
        }
      })
      .catch(() => {
        if (active) {
          setStartupError("No se pudo conectar con el servicio administrativo.");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <main className="boot-screen" aria-live="polite">
        <span className="brand-mark">BB</span>
        <p>Preparando el catálogo</p>
      </main>
    );
  }

  if (!session) {
    return (
      <LoginPage
        initialError={startupError}
        onAuthenticated={(authenticatedSession) => {
          setStartupError(null);
          setSession(authenticatedSession);
        }}
      />
    );
  }

  return <Dashboard session={session} onLoggedOut={() => setSession(null)} />;
}
