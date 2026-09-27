package bo.umss.market.umss_market_api.infrastructure.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public class AIMonitoringEvent {

    private OffsetDateTime ts;

    private String usuario;

    private String version;

    private String pregunta;

    private String respuesta;

    private List<String> fuentes;

    private String camino;

    private String herramienta;

    private String guard;

    @JsonProperty("latencia_ms")
    private long latenciaMs;

    private Integer tokens;

    private String feedback;

    public AIMonitoringEvent() {
    }

    public OffsetDateTime getTs() {
        return ts;
    }

    public void setTs(OffsetDateTime ts) {
        this.ts = ts;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public List<String> getFuentes() {
        return fuentes;
    }

    public void setFuentes(List<String> fuentes) {
        this.fuentes = fuentes;
    }

    public String getCamino() {
        return camino;
    }

    public void setCamino(String camino) {
        this.camino = camino;
    }

    public String getHerramienta() {
        return herramienta;
    }

    public void setHerramienta(String herramienta) {
        this.herramienta = herramienta;
    }

    public String getGuard() {
        return guard;
    }

    public void setGuard(String guard) {
        this.guard = guard;
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }

    public void setLatenciaMs(long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}