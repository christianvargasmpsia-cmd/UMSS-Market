package bo.umss.market.umss_market_api.infrastructure.graphql;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
public record CatalogoFiltroInput(String texto, PublicationType tipo, String precioMin,
                                  String precioMax, String tiendaId, String categoria,
                                  Boolean disponible) { }
