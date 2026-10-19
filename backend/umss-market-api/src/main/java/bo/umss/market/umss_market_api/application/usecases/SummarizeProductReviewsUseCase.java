package bo.umss.market.umss_market_api.application.usecases;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import bo.umss.market.umss_market_api.application.dto.ReviewSummaryResponse;
import bo.umss.market.umss_market_api.application.dto.ReviewSummaryResponse.Aspect;
import bo.umss.market.umss_market_api.domain.model.ProductReview;
import bo.umss.market.umss_market_api.domain.ports.*;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor
public class SummarizeProductReviewsUseCase {
    private final ProductReviewsUseCase products;
    private final ReviewRepositoryPort reviews;
    private final ReviewSummaryPort ai;
    private static final int SAMPLE_LIMIT=10;
    private static final Pattern INSTRUCTION=Pattern.compile("(?iu)(ignora|ignore|instruccion|instructions|system|assistant|prompt|<\\|)");
    private record Fragment(UUID reviewId, String quote, int rating) {}

    public ReviewSummaryResponse execute(UUID publicationId) {
        products.requireProduct(publicationId);
        var sample=reviews.findLatestByPublicationId(publicationId,SAMPLE_LIMIT).stream()
                .filter(r->publicationId.equals(r.publicationId())).limit(SAMPLE_LIMIT).toList();
        if (sample.isEmpty()) return result(publicationId,0,0,"NO_REVIEWS","Este producto aun no tiene resenas.",List.of(),List.of());
        var fragments=sample.stream().flatMap(r->Arrays.stream(r.comment().split("[.!?\r\n]+"))
                .map(String::strip).map(s->new Fragment(r.id(),s,r.rating())))
                .filter(f->f.quote().length()>=4 && f.quote().length()<=240)
                .filter(f->!INSTRUCTION.matcher(f.quote()).find()).limit(24).toList();
        if (fragments.isEmpty()) return noEvidence(publicationId,0,sample.size());
        int reviewCount=(int)fragments.stream().map(Fragment::reviewId).distinct().count();
        var evidence=new ArrayList<ReviewSummaryPort.Evidence>();
        for(int i=0;i<fragments.size();i++) evidence.add(new ReviewSummaryPort.Evidence(i,fragments.get(i).quote(),fragments.get(i).rating()));
        try {
            var selection=ai.select(List.copyOf(evidence));
            var used=new HashSet<Integer>();
            var positive=aspects(selection.positive(),fragments,used);
            var negative=aspects(selection.negative(),fragments,used);
            if (used.isEmpty()) return noEvidence(publicationId,reviewCount,sample.size());
            String summary="Se analizaron " + reviewCount + " resenas. Positivos: " + describe(positive)
                    + ". Negativos: " + describe(negative) + ".";
            return result(publicationId,reviewCount,sample.size(),"GENERATED",summary,positive,negative);
        } catch (RuntimeException ex) {
            return result(publicationId,reviewCount,sample.size(),"UNAVAILABLE",
                    "No se pudo generar el resumen. Las resenas originales siguen disponibles.",List.of(),List.of());
        }
    }
    private static List<Aspect> aspects(List<Integer> indices,List<Fragment> fragments,Set<Integer> used) {
        if (indices==null) throw new IllegalArgumentException("Seleccion invalida");
        var result=new ArrayList<Aspect>();
        for(Integer index:indices) {
            if (index==null || index<0 || index>=fragments.size() || !used.add(index))
                throw new IllegalArgumentException("Evidencia no valida");
            var fragment=fragments.get(index);
            result.add(new Aspect(fragment.reviewId(),fragment.quote()));
        }
        // Validate every reference before limiting the presentation, including omitted evidence.
        return result.stream().limit(3).toList();
    }
    private static String describe(List<Aspect> aspects) {
        if (aspects.isEmpty()) return "sin evidencia suficiente";
        return aspects.stream().map(a->"["+a.quote()+"]").collect(Collectors.joining("; "));
    }
    private static ReviewSummaryResponse noEvidence(UUID id,int count,int considered) {
        return result(id,count,considered,"NO_EVIDENCE","No se identificaron aspectos con evidencia suficiente.",List.of(),List.of());
    }
    private static ReviewSummaryResponse result(UUID id,int count,int considered,String status,String summary,List<Aspect> positive,List<Aspect> negative) {
        return new ReviewSummaryResponse(id,count,considered,SAMPLE_LIMIT,status,summary,positive,negative);
    }
}
