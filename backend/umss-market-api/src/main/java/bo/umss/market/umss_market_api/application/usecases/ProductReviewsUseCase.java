package bo.umss.market.umss_market_api.application.usecases;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.domain.model.ProductReview;
import bo.umss.market.umss_market_api.domain.ports.*;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor
public class ProductReviewsUseCase {
    private final PublicationRepositoryPort publications;
    private final UserRepositoryPort users;
    private final ReviewRepositoryPort reviews;
    public ReviewResponse create(UUID publicationId, UUID buyerId, ReviewRequest request) {
        if (buyerId==null || users.findById(buyerId).isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Comprador no disponible");
        requireProduct(publicationId);
        if (request==null || request.rating()==null || request.rating()<1 || request.rating()>5)
            throw new IllegalArgumentException("rating debe estar entre 1 y 5");
        if (request.comment()==null || request.comment().isBlank() || request.comment().length()>1000)
            throw new IllegalArgumentException("comment debe contener entre 1 y 1000 caracteres");
        return ReviewResponse.from(reviews.save(new ProductReview(UUID.randomUUID(),publicationId,buyerId,
                request.rating(),request.comment().strip(),Instant.now())));
    }
    public List<ReviewResponse> list(UUID publicationId, int limit) {
        requireProduct(publicationId);
        if (limit<1 || limit>50) throw new IllegalArgumentException("limit debe estar entre 1 y 50");
        return reviews.findLatestByPublicationId(publicationId,limit).stream().map(ReviewResponse::from).toList();
    }
    public void requireProduct(UUID id) {
        if (publications.findById(id).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Publicacion no encontrada");
    }
}
