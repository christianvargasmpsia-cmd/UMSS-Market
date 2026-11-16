package bo.umss.market.umss_market_api.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.StoreRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationImageRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

class PublicationImageControllerTest {
    @TempDir Path dir;
    private final UUID user = UUID.randomUUID();
    private final UUID publicationId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final PublicationRepositoryPort publications = mock(PublicationRepositoryPort.class);
    private final StoreRepositoryPort stores = mock(StoreRepositoryPort.class);
    private final JpaPublicationImageRepository images = mock(JpaPublicationImageRepository.class);

    private PublicationImageController controller() {
        return new PublicationImageController(publications, stores, images, dir.toString());
    }
    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    private void owned() {
        when(publications.findById(publicationId)).thenReturn(Optional.of(Publication.builder().id(publicationId).storeId(storeId).build()));
        when(stores.findByUserId(user)).thenReturn(Optional.of(Store.builder().id(storeId).userId(user).build()));
    }
    @Test void rejectsBuyer() {
        assertThrows(AccessDeniedException.class, () -> controller().upload(publicationId,
            new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}), auth("COMPRADOR")));
    }
    @Test void rejectsOtherStore() {
        owned();
        when(stores.findByUserId(user)).thenReturn(Optional.of(Store.builder().id(UUID.randomUUID()).build()));
        assertThrows(AccessDeniedException.class, () -> controller().upload(publicationId,
            new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}), auth("EMPRENDEDOR")));
    }
    @Test void rejectsFakeImage() {
        owned();
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> controller().upload(publicationId,
            new MockMultipartFile("file", "a.png", "image/png", "not a png".getBytes()), auth("EMPRENDEDOR")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }
    @Test void acceptsPng() throws Exception {
        owned();
        byte[] png = new byte[]{(byte)137,80,78,71,13,10,26,10,0,0};
        assertEquals(HttpStatus.CREATED, controller().upload(publicationId,
            new MockMultipartFile("file", "a.png", "image/png", png), auth("EMPRENDEDOR")).getStatusCode());
        verify(images).saveAndFlush(any());
    }
}
