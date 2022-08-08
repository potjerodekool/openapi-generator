package io.github.potjerodekool.demo.api;

import io.github.potjerodekool.demo.api.model.PetResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@CrossOrigin
@SecurityRequirement(name = "bearerAuth")
public class PetstoreController implements PetstoreApi {
    @Override
    public ResponseEntity<Void> createPets(final HttpServletRequest request) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return PetstoreApi.super.createPets(request);
    }

    @Override
    public ResponseEntity<List<PetResponseDto>> listPets(final Integer limit,
                                                         final HttpServletRequest request) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return PetstoreApi.super.listPets(limit, request);
    }

    @Override
    public ResponseEntity<PetResponseDto> showPetById(final String petId,
                                                      final HttpServletRequest request) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return PetstoreApi.super.showPetById(petId, request);
    }
}
