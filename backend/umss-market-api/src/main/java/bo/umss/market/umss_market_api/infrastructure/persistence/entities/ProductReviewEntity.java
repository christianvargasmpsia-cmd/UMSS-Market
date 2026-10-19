package bo.umss.market.umss_market_api.infrastructure.persistence.entities;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="product_reviews", indexes=@Index(name="idx_reviews_publication_created",columnList="publication_id,created_at,id"))
@org.hibernate.annotations.Check(constraints="rating between 1 and 5 and char_length(trim(comment)) between 1 and 1000")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductReviewEntity {
    @Id private UUID id;
    @Column(name="publication_id",nullable=false) private UUID publicationId;
    @Column(name="buyer_id",nullable=false) private UUID buyerId;
    @Column(nullable=false) private Integer rating;
    @Column(nullable=false,length=1000) private String comment;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="publication_id",insertable=false,updatable=false) private PublicationEntity publication;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="buyer_id",insertable=false,updatable=false) private UserEntity buyer;
}
