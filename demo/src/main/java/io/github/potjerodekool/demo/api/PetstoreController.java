package io.github.potjerodekool.demo.api;

import io.github.potjerodekool.demo.api.model.ErrorResponseDto;
import io.github.potjerodekool.demo.api.model.PetPatchRequestDto;
import io.github.potjerodekool.demo.api.model.PetRequestDto;
import io.github.potjerodekool.demo.api.model.PetResponseDto;
import io.github.potjerodekool.demo.model.Pet;
import io.github.potjerodekool.demo.service.CrudOperationResult;
import io.github.potjerodekool.demo.service.PetService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class PetstoreController implements PetstoreApi {

    private final PetService petService;

    public PetstoreController(final PetService petService) {
        this.petService = petService;
    }

    @Override
    public ResponseEntity<Void> createPet(final PetRequestDto petRequestDto,
                                          final HttpServletRequest request) {
        final var id = petService.createPet(petRequestDto);

        if (id != null) {
            return ResponseEntity.created(ApiUtilsKt.createLocation(request, id)).build();
        } else {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @Override
    public ResponseEntity<PetResponseDto> getPetById(final long petId,
                                                     final HttpServletRequest request) {
        return petService.getPetById(petId)
                .map(pet -> ResponseEntity.ok(createResponse(pet)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<PetResponseDto>> listPets(final Integer limit,
                                                         final HttpServletRequest request) {

        var pets = petService.listPets(limit);

        return ResponseEntity.ok(
                pets.stream()
                        .map(this::createResponse)
                        .toList()
        );
    }

    @Override
    public ResponseEntity<ErrorResponseDto> updatePet(final long petId, PetRequestDto petRequestDto, HttpServletRequest request) {
        final var result = petService.updatePet(petId, petRequestDto);
        return switch (result.status()) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.unprocessableEntity().body(new ErrorResponseDto(0, result.message()));
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }

    @Override
    public ResponseEntity<ErrorResponseDto> patchPet(final long petId, PetPatchRequestDto petPatchRequestDto, HttpServletRequest request) {
        final var result = petService.patchPet(petId, petPatchRequestDto);
        return switch (result.status()) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.unprocessableEntity().body(new ErrorResponseDto(0, result.message()));
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }

    @Override
    public ResponseEntity<Void> deletePet(final long petId,
                                          final HttpServletRequest request) {
        final var result = petService.deletePet(petId);

        if (result.status() == CrudOperationResult.Status.SUCCESS) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<Void> updatePetImage(final long petId,
                                               final MultipartFile file,
                                               final HttpServletRequest request) {
        try {
            petService.updatePetImage(
                    petId,
                    file.getContentType(),
                    file.getBytes()
            );
        } catch (final IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<? extends org.springframework.core.io.Resource> getPetImageById(final long petId,
                                                                                          final HttpServletRequest request) {
        final var resourceWithMimeTypeOptional = petService.getPetImage(petId);

        return resourceWithMimeTypeOptional.map(resourceWithMimeType ->
                    ResponseEntity.ok()
                            .contentType(resourceWithMimeType.mimeType())
                            .body(new InputStreamResource(resourceWithMimeType.inputStream()))
        ).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Map<String, Object>> getPetDetailsById(final long petId,
                                                                 final HttpServletRequest request) {
        return petService.getPetDetailsById(petId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PetResponseDto createResponse(final Pet pet) {
        final var response = new PetResponseDto(pet.getId(), pet.getName(),
                LocalDate.now(),
                pet.getTag(
                )
        );
        return response;
    }
}
