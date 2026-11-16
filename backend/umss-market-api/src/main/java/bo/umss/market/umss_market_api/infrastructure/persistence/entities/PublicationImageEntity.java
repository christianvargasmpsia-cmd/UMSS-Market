package bo.umss.market.umss_market_api.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "publication_images", indexes = @Index(name = "idx_publication_images_publication", columnList = "publication_id"))
@Getter @Setter
public class PublicationImageEntity {
    @Id
    private UUID id;
    @Column(name = "publication_id", nullable = false)
    private UUID publicationId;
    @Column(nullable = false, unique = true)
    private String filename;
    @Column(nullable = false)
    private String contentType;
    @Column(nullable = false)
    private long sizeBytes;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
