interface WhatsAppPreviewProps {
  title: string;
  messageBody: string;
}

export default function WhatsAppPreview({ title, messageBody }: WhatsAppPreviewProps) {
  return (
    <section className="preview-block">
      <div className="section-heading compact-heading">
        <div>
          <span className="eyebrow">VISTA PREVIA</span>
          <h3>Así llega a WhatsApp</h3>
        </div>
        <span className="live-pill"><i /> En vivo</span>
      </div>
      <div className="phone-preview">
        <div className="phone-topbar">
          <span className="phone-avatar">DB</span>
          <span>
            <strong>Doctor Beauty</strong>
            <small>en línea</small>
          </span>
        </div>
        <div className="chat-canvas">
          <div className="chat-bubble">
            {!messageBody.trim() ? (
              <span className="preview-placeholder">El texto de la promoción aparecerá acá.</span>
            ) : (
              <>
                {title.trim() && <strong className="preview-title">{title}</strong>}
                <span className="preview-copy">{messageBody}</span>
              </>
            )}
            <small className="message-time">18:42 ✓✓</small>
          </div>
        </div>
      </div>
    </section>
  );
}
