package io.github.potjerodekool.demo.service;

import io.github.potjerodekool.demo.NotFoundException;
import io.github.potjerodekool.demo.api.PetsServiceApi;
import io.github.potjerodekool.demo.api.Request;
import io.github.potjerodekool.demo.api.model.PatchPetDto;
import io.github.potjerodekool.demo.api.model.PetDto;
import io.github.potjerodekool.demo.model.Pet;
import io.github.potjerodekool.demo.model.ResourceWithMediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PetService implements PetsServiceApi {

    private final Map<Long, Pet> pets = new HashMap<>();

    @Override
    public PetDto createPet(final PetDto petRequestDto,
                            final Request request) {
        final var pet = new Pet();
        final long id = pets.size() + 1;
        pet.setId(id);
        copyValues(petRequestDto, pet);
        pets.put(id, pet);

        return new PetDto().id(id)
                .name(petRequestDto.getName());
    }

    @Override
    public List<PetDto> listPets(final int limit,
                                 final Request request) {
        return List.of();
    }

    private void copyValues(final PetDto petRequestDto,
                            final Pet pet) {
        pet.setName(petRequestDto.getName());
        pet.setTag(petRequestDto.getTag());
    }

    @Override
    public PetDto getPetById(final long petId,
                             final Request request) {
        final var petOptional = findPetById(petId);

        if (petOptional.isEmpty()) {
            return null;
        }

        final var pet = petOptional.get();

        return new PetDto()
                .id(petId)
                .name(pet.getName());
    }

    private Optional<Pet> findPetById(final long petId) {
        final var pet = pets.get(petId);
        return Optional.of(pet);
    }

    public List<Pet> listPets(final Integer limit) {
        var petStream = pets.values().stream();

        if (limit != null) {
            petStream = petStream.limit(limit);
        }

        return petStream.toList();
    }

    @Override
    public void updatePet(final long petId,
                              final PetDto petRequestDto,
                              final Request request) {
        final var pet = pets.get(petId);

        if (pet == null) {
            throw new NotFoundException();
        }

        copyValues(petRequestDto, pet);
    }

    @Override
    public void patchPet(final long petId,
                         final PatchPetDto petPatchRequestDto,
                         final Request request) {
        final var pet = pets.get(petId);

        if (pet == null) {
            throw new NotFoundException();
        }

        petPatchRequestDto.getName().ifPresent(pet::setName);
        petPatchRequestDto.getTag().ifPresent(pet::setTag);
    }

    @Override
    public void deletePet(final long petId,
                          final Request request) {
        final var deletedPet = pets.remove(petId);
    }

    @Override
    public Resource getPetImageById(final long petId,
                                    final Request request) {
        return null;
    }

    @Override
    public void updatePetImage(final long petId,
                               final MultipartFile body,
                               final Request request) {
    }

    public Optional<ResourceWithMediaType> getPetImage(final long petId) {
        final var petOptional = findPetById(petId);

        if (petOptional.isEmpty()) {
            return Optional.empty();
        }

        final var pet = petOptional.get();

        final var image = pet.getImage();

        if (image == null) {
            if ("cat".equals(pet.getTag())) {
                return Optional.of(new ResourceWithMediaType(
                        getClass().getResourceAsStream("/cat.jpg"),
                        MediaType.IMAGE_JPEG
                    )
                );
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(new ResourceWithMediaType(
                    new ByteArrayInputStream(image.data()),
                    MediaType.parseMediaType(image.contentType())));
        }
    }

    public CrudOperationResult updatePetImage(final long petId,
                                              final String contentType,
                                              final byte[] data) {
        final var pet = pets.get(petId);

        if (pet == null) {
            return CrudOperationResult.notFound();
        }

        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType) || MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            pet.setImage(new Pet.Image(contentType, data));
            return CrudOperationResult.success();
        } else {
            return CrudOperationResult.failed(String.format("Content type %s is not supported, only %s and %s",
                        contentType,
                        MediaType.IMAGE_JPEG_VALUE,
                        MediaType.IMAGE_PNG_VALUE
                    )
            );
        }
    }

    @Override
    public Map<String, Object> getPetDetailsById(final long petId,
                                                 final Request request) {
        final var pet = pets.get(petId);

        if (pet == null) {
            return new HashMap<>();
        }

        final var details = new HashMap<String, Object>();
        details.put("name", pet.getName());
        details.put("tage", pet.getTag());
        details.put("dateOfBirth", null);

        return details;
    }
}
