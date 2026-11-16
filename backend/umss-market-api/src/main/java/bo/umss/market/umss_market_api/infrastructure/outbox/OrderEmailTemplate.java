package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.util.Objects;

public final class OrderEmailTemplate {

    private OrderEmailTemplate() {
    }

    public static String render(String buyerName, String orderId,
                                String status, String eventType) {

        boolean created = "ORDER_CREATED".equals(eventType);

        String title = created
                ? "¡Tu pedido fue registrado!"
                : "Tu pedido tiene una actualización";

        String description = created
                ? "Recibimos tu pedido correctamente. Te avisaremos cuando cambie su estado."
                : "El estado de tu pedido ha cambiado. Puedes consultar sus detalles en UMSS Market.";

        String safeName = escape(buyerName);
        String safeOrderId = escape(orderId);
        String safeStatus = escape(status);
        String safeTitle = escape(title);
        String safeDescription = escape(description);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>UMSS Market</title>
                </head>
                <body style="margin:0;padding:24px 12px;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#334155;">
                  <table role="presentation" cellpadding="0" cellspacing="0"
                         style="width:100%%;max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:#163b68;padding:30px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:28px;">UMSS Market</h1>
                        <p style="margin:8px 0 0;color:#dce9fa;font-size:14px;">Marketplace universitario</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px 28px;">
                        <h2 style="color:#163b68;font-size:23px;margin:0 0 20px;">%s</h2>
                        <p>Hola, <strong>%s</strong>:</p>
                        <p style="line-height:1.7;">%s</p>

                        <table role="presentation" cellpadding="12" cellspacing="0"
                               style="width:100%%;background:#f1f5f9;border-radius:8px;margin:24px 0;">
                          <tr>
                            <td colspan="2" style="color:#64748b;font-size:12px;font-weight:bold;">
                              DETALLE DEL PEDIDO
                            </td>
                          </tr>
                          <tr>
                            <td style="color:#64748b;">Número de pedido</td>
                            <td style="font-size:12px;overflow-wrap:anywhere;">%s</td>
                          </tr>
                          <tr>
                            <td style="color:#64748b;">Estado actual</td>
                            <td>
                              <span style="background:#fef3c7;color:#92400e;padding:7px 12px;border-radius:16px;font-weight:bold;font-size:12px;">
                                %s
                              </span>
                            </td>
                          </tr>
                        </table>

                        <p style="line-height:1.7;">
                          Gracias por apoyar los emprendimientos de nuestra comunidad universitaria.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#e2e8f0;padding:20px;text-align:center;color:#64748b;font-size:12px;">
                        UMSS Market · Notificación automática
                        <p>Este correo fue generado automáticamente. No es necesario responder.</p>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                    safeTitle,
                    safeName,
                    safeDescription,
                    safeOrderId,
                    safeStatus
                );
    }

    private static String escape(String value) {
        return Objects.toString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
