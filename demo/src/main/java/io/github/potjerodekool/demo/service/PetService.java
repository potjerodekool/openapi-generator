package io.github.potjerodekool.demo.service;

import io.github.petstore.model.PetPatchRequestDto;
import io.github.petstore.model.PetRequestDto;
import io.github.potjerodekool.demo.model.Pet;
import io.github.potjerodekool.demo.model.ResourceWithMediaType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PetService {

    private final Map<Long, Pet> pets = new HashMap<>();

    public Long createPet(final PetRequestDto petRequestDto) {
        final var pet = new Pet();
        final long id = pets.size() + 1;
        pet.setId(id);
        copyValues(petRequestDto, pet);
        pets.put(id, pet);
        return id;
    }

    private void copyValues(final PetRequestDto petRequestDto,
                            final Pet pet) {
        pet.setName(petRequestDto.getName());
        pet.setTag(petRequestDto.getTag());
    }

    public Optional<Pet> getPetById(final long petId) {
        return Optional.ofNullable(pets.get(petId));
    }

    public List<Pet> listPets(final Integer limit) {
        var petStream = pets.values().stream();

        if (limit != null) {
            petStream = petStream.limit(limit);
        }

        return petStream.toList();
    }

    public CrudOperationResult updatePet(final long petId,
                                         final PetRequestDto petRequestDto) {
        final var pet = pets.get(petId);

        if (pet == null) {
            return CrudOperationResult.notFound();
        }

        copyValues(petRequestDto, pet);
        return CrudOperationResult.success();
    }

    public CrudOperationResult patchPet(final long petId,
                                        final PetPatchRequestDto petPatchRequestDto) {
        final var pet = pets.get(petId);

        if (pet == null) {
            return CrudOperationResult.notFound();
        }

        petPatchRequestDto.getName().ifPresent(pet::setName);
        petPatchRequestDto.getTag().ifPresent(pet::setTag);

        return CrudOperationResult.success();
    }

    public CrudOperationResult deletePet(final long petId) {
        final var deletedPet = pets.remove(petId);
        return deletedPet != null ? CrudOperationResult.success() : CrudOperationResult.notFound();
    }

    public Optional<ResourceWithMediaType> getPetImage(final long petId) {
        final var petOptional = getPetById(petId);

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

    public Optional<Map<String, Object>> getPetDetailsById(final long petId) {
        final var pet = pets.get(petId);

        if (pet == null) {
            return Optional.empty();
        }

        final var details = new HashMap<String, Object>();
        details.put("name", pet.getName());
        details.put("tage", pet.getTag());
        details.put("dateOfBirth", null);

        return Optional.of(details);
    }
}
