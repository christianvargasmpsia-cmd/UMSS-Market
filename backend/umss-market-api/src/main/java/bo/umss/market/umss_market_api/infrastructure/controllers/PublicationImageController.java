package bo.umss.market.umss_market_api.infrastructure.controllers;

import bo.umss.market.umss_market_api.application.dto.PublicationImageResponse;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.StoreRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationImageEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationImageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/publications/{publicationId}/images")
public class PublicationImageController {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final PublicationRepositoryPort publications;
    private final StoreRepositoryPort stores;
    private final JpaPublicationImageRepository images;
    private final Path directory;

    public PublicationImageController(PublicationRepositoryPort publications, StoreRepositoryPort stores,
            JpaPublicationImageRepository images, @Value("${publication.images.directory:./uploads/publications}") String directory) {
        this.publications = publications;
        this.stores = stores;
        this.images = images;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    private Publication publication(UUID id) {
        return publications.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicacion no encontrada"));
    }

    private void owner(UUID publicationId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT requerido");
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_EMPRENDEDOR"))) {
            throw new AccessDeniedException("Se requiere rol EMPRENDEDOR");
        }
        Publication p = publication(publicationId);
        Store store = stores.findByUserId(userId).orElseThrow(() -> new AccessDeniedException("Tienda no encontrada"));
        if (!store.getId().equals(p.getStoreId())) throw new AccessDeniedException("Publicacion de otra tienda");
    }

    private PublicationImageEntity image(UUID publicationId, UUID imageId) {
        PublicationImageEntity img = images.findById(imageId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        if (!img.getPublicationId().equals(publicationId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        return img;
    }

    private PublicationImageResponse response(PublicationImageEntity img) {
        return new PublicationImageResponse(img.getId(), img.getPublicationId(), img.getContentType(), img.getSizeBytes(),
                "/api/publications/" + img.getPublicationId() + "/images/" + img.getId() + "/content");
    }

    private String detectedType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan JPEG, PNG o WebP validos");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicationImageResponse> upload(@PathVariable UUID publicationId,
            @RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        owner(publicationId, authentication);
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio o mayor a 5 MB");
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo mayor a 5 MB");
        String type = detectedType(bytes);
        if (!type.equals(file.getContentType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Type no coincide con el archivo");
        UUID id = UUID.randomUUID();
        String extension = type.equals("image/jpeg") ? ".jpg" : type.equals("image/png") ? ".png" : ".webp";
        String filename = id + extension;
        Files.createDirectories(directory);
        Path target = directory.resolve(filename);
        Files.write(target, bytes);
        PublicationImageEntity img = new PublicationImageEntity();
        img.setId(id);
        img.setPublicationId(publicationId);
        img.setFilename(filename);
        img.setContentType(type);
        img.setSizeBytes(bytes.length);
        img.setCreatedAt(LocalDateTime.now());
        try {
            images.saveAndFlush(img);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response(img));
    }

    @GetMapping
    public List<PublicationImageResponse> list(@PathVariable UUID publicationId) {
        publication(publicationId);
        return images.findByPublicationIdOrderByCreatedAtAsc(publicationId).stream().map(this::response).toList();
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID publicationId, @PathVariable UUID imageId) {
        publication(publicationId);
        PublicationImageEntity img = image(publicationId, imageId);
        Path file = directory.resolve(img.getFilename()).normalize();
        if (!file.getParent().equals(directory) || !Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, img.getContentType())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicationId, @PathVariable UUID imageId,
            Authentication authentication) throws IOException {
        owner(publicationId, authentication);
        PublicationImageEntity img = image(publicationId, imageId);
        images.delete(img);
        images.flush();
        Files.deleteIfExists(directory.resolve(img.getFilename()));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acceso denegado"));
    }
}
