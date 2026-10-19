package bo.umss.market.umss_market_api.application.usecases;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.ports.*;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.dto.DuplicateCheckResponse.Match;
@Service
public class DetectDuplicatePublicationsUseCase {
    private final StoreRepositoryPort stores;
    private final UserRepositoryPort users;
    private final PublicationRepositoryPort publications;
    private final DuplicateCatalogPort catalog;
    private final DuplicateEmbeddingPort embeddings;
    private final double threshold;
    private final int limit;
    public DetectDuplicatePublicationsUseCase(StoreRepositoryPort stores, UserRepositoryPort users,
            PublicationRepositoryPort publications, DuplicateCatalogPort catalog, DuplicateEmbeddingPort embeddings,
            @Value("${duplicates.threshold:0.90}") double threshold,
            @Value("${duplicates.max-results:5}") int limit) {
        if (!Double.isFinite(threshold) || threshold<0 || threshold>1 || limit<1 || limit>50)
            throw new IllegalArgumentException("Configuracion de duplicados invalida");
        this.stores=stores;this.users=users;this.publications=publications;this.catalog=catalog;
        this.embeddings=embeddings;this.threshold=threshold;this.limit=limit;
    }
    public DuplicateCheckResponse execute(UUID userId, UUID storeId, DuplicateCheckRequest request) {
        if (userId==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Autenticacion requerida");
        var user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuario no disponible"));
        if (user.getRole()!=Role.EMPRENDEDOR) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Se requiere emprendedor");
        var store=stores.findById(storeId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tienda no encontrada"));
        if (!userId.equals(store.getUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tienda ajena");
        if (request==null || request.title()==null || request.title().isBlank() || request.title().length()>200
                || request.description()==null || request.description().isBlank() || request.description().length()>2000)
            throw new IllegalArgumentException("title (1..200) y description (1..2000) son obligatorios");
        if (request.excludePublicationId()!=null) {
            var own=publications.findById(request.excludePublicationId())
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Publicacion no encontrada"));
            if (!storeId.equals(own.getStoreId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Publicacion ajena");
        }
        var candidates=catalog.findByStore(storeId).stream()
                .filter(p->!p.id().equals(request.excludePublicationId())).toList();
        if (candidates.isEmpty()) return response(storeId,List.of());
        String query=text(request.title(),request.description());
        var texts=new LinkedHashSet<String>();texts.add(query);
        candidates.forEach(p->texts.add(text(p.title(),p.description())));
        var inputs=List.copyOf(texts);
        Map<String,double[]> vectors=new HashMap<>();
        try {
            var batch=embeddings.embed(inputs);
            if (batch==null || batch.size()!=inputs.size()) throw new IllegalStateException("Lote invalido");
            int dimensions=-1;
            for(int i=0;i<inputs.size();i++) {
                var normalized=normalize(batch.get(i));
                if (dimensions!=-1 && normalized.length!=dimensions) throw new IllegalStateException("Dimensiones incompatibles");
                dimensions=normalized.length;vectors.put(inputs.get(i),normalized);
            }
        } catch(RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "EMBEDDINGS_UNAVAILABLE: No se pudo evaluar duplicados; intente nuevamente",ex);
        }
        var matches=new ArrayList<Match>();
        for(var candidate:candidates) {
            String other=text(candidate.title(),candidate.description());
            double score=query.equals(other)?1:cosine(vectors.get(query),vectors.get(other));
            if(score>=threshold) matches.add(new Match(candidate.id(),candidate.title(),score));
        }
        var sorted=matches.stream().sorted(Comparator.comparingDouble(Match::similarity).reversed()
                .thenComparing(m->m.publicationId().toString())).limit(limit).toList();
        return response(storeId,sorted);
    }
    private DuplicateCheckResponse response(UUID storeId,List<Match> matches) {
        return new DuplicateCheckResponse(storeId,"EVALUATED",
                "Posibles coincidencias para revision humana. La deteccion no bloquea ni modifica publicaciones.",threshold,limit,matches);
    }
    private static String text(String title,String description) {
        return "Titulo: "+Objects.toString(title,"").strip()+"\nDescripcion: "+Objects.toString(description,"").strip();
    }
    private static double[] normalize(List<Double> vector) {
        if(vector==null || vector.isEmpty()) throw new IllegalStateException("Vector vacio");
        double max=0;
        for(var value:vector) {
            if(value==null || !Double.isFinite(value)) throw new IllegalStateException("Vector no numerico");
            max=Math.max(max,Math.abs(value));
        }
        if(max==0) throw new IllegalStateException("Vector nulo");
        var result=new double[vector.size()];double squared=0;
        for(int i=0;i<result.length;i++) {result[i]=vector.get(i)/max;squared+=result[i]*result[i];}
        double norm=Math.sqrt(squared);
        for(int i=0;i<result.length;i++) result[i]/=norm;
        return result;
    }
    private static double cosine(double[] a,double[] b) {
        double dot=0;for(int i=0;i<a.length;i++) dot+=a[i]*b[i];
        return Math.max(-1,Math.min(1,dot));
    }
}
