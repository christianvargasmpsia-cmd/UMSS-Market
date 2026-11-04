package bo.umss.market.umss_market_api.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;

/** DTO seguro para la lectura publica del catalogo: no expone embeddings. */
public record PagedPublicationItem(
        UUID id, UUID storeId, String nombre, String descripcion,
        BigDecimal precio, PublicationType tipo, Integer stock) { }
