package bo.umss.market.umss_market_api.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.domain.ports.InteractionRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteInteractionUseCase {

    private final InteractionRepositoryPort interactionRepository;

    public void execute(UUID userId, UUID id) {

        interactionRepository.findById(id)
                        .filter(i -> i.getUserId().equals(userId))
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Interaccion no encontrada")
                );

        interactionRepository.deleteById(id);
    }
}