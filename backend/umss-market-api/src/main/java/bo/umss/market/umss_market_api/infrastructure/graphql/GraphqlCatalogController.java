package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.CatalogQueryPort;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class GraphqlCatalogController {
    private final CatalogQueryPort catalogo;

    public record Pagina<T>(List<T> items, int total, int pagina, int limite, int paginas) { }

    @QueryMapping
    public Pagina<ProductoGraphql> catalogo(@Argument CatalogoFiltroInput filtro,
                                            @Argument Integer pagina, @Argument Integer limite) {
        int p = checkedPage(pagina), n = checkedSize(limite);
        CatalogoFiltroInput f = filtro == null ? new CatalogoFiltroInput(null, null, null, null, null, null, null) : filtro;
        BigDecimal min = money(f.precioMin()), max = money(f.precioMax());
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("precioMin no puede superar precioMax");
        }
        String texto = normalize(f.texto(), 120);
        String categoria = normalize(f.categoria(), 100);
        CatalogQuery q = new CatalogQuery(texto, f.tipo(), min, max, uuid(f.tiendaId()), categoria,
                f.disponible(), p, n);
        CatalogPage<Publication> result = catalogo.buscar(q);
        return toPage(result, ProductoGraphql::from);
    }

    @QueryMapping
    public ProductoGraphql producto(@Argument String id) {
        if (id == null) throw new IllegalArgumentException("El ID es obligatorio");
        return catalogo.detallePublico(uuid(id)).map(ProductoGraphql::from).orElse(null);
    }

    @QueryMapping
    public Pagina<TiendaGraphql> tiendas(@Argument String categoria, @Argument Integer pagina,
                                         @Argument Integer limite) {
        return toPage(catalogo.tiendas(normalize(categoria, 100), checkedPage(pagina), checkedSize(limite)),
                TiendaGraphql::from);
    }

    @QueryMapping
    public List<String> categorias() { return catalogo.categorias(); }

    /** Una sola consulta a tiendas por lote para evitar N+1 al solicitar Producto.tienda. */
    @BatchMapping(typeName = "Producto", field = "tienda")
    public Map<ProductoGraphql, TiendaGraphql> tienda(List<ProductoGraphql> productos) {
        Set<UUID> ids = productos.stream().map(ProductoGraphql::tiendaId).collect(Collectors.toSet());
        Map<UUID, TiendaGraphql> resolved = catalogo.tiendasPorIds(ids).stream()
                .collect(Collectors.toMap(Store::getId, TiendaGraphql::from));
        return productos.stream().filter(p -> resolved.containsKey(p.tiendaId()))
                .collect(Collectors.toMap(Function.identity(), p -> resolved.get(p.tiendaId()), (a,b) -> a));
    }

    @GraphQlExceptionHandler
    public GraphQLError invalidArgument(GraphqlErrorBuilder<?> builder, IllegalArgumentException ex) {
        return builder.errorType(ErrorType.BAD_REQUEST).message(ex.getMessage()).build();
    }

    private static <T,U> Pagina<U> toPage(CatalogPage<T> p, Function<T,U> transform) {
        return new Pagina<>(p.items().stream().map(transform).toList(),
                Math.toIntExact(p.total()), p.pagina(), p.limite(), p.paginas());
    }
    private static int checkedPage(Integer page) {
        int p = page == null ? 1 : page;
        if (p < 1 || p > 100_000) throw new IllegalArgumentException("pagina debe estar entre 1 y 100000");
        return p;
    }
    private static int checkedSize(Integer size) {
        int n = size == null ? 10 : size;
        if (n < 1 || n > 50) throw new IllegalArgumentException("limite debe estar entre 1 y 50");
        return n;
    }
    private static BigDecimal money(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 24) throw new IllegalArgumentException("Precio demasiado largo");
        try {
            BigDecimal money = new BigDecimal(value.trim());
            if (money.signum() < 0) throw new IllegalArgumentException("El precio debe ser >= 0");
            return money;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de precio invalido");
        }
    }
    private static UUID uuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("ID debe ser UUID valido"); }
    }
    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException("Filtro demasiado largo");
        return clean;
    }
}
