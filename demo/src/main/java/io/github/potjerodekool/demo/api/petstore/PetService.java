package io.github.potjerodekool.demo.api.petstore;

import io.github.potjerodekool.demo.petstore.api.PetServiceApi;
import io.github.potjerodekool.demo.petstore.api.Request;
import io.github.potjerodekool.demo.petstore.api.model.ApiResponse;
import io.github.potjerodekool.demo.petstore.api.model.Pet;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PetService implements PetServiceApi {

    private final Map<Long, Pet> pets = new HashMap<>();

    private long idGenerator = 0;

    @Override
    public Pet addPet(final Pet pet, final Request request) {
        idGenerator++;
        pet.setId(idGenerator);
        pets.put(pet.getId(), pet);
        return pet;
    }

    @Override
    public Pet updatePet(final Pet pet, final Request request) {
        pets.put(pet.getId(), pet);
        return pet;
    }

    @Override
    public List<Pet> findPetsByStatus(final Enum status, final Request request) {
        return pets.values().stream()
                .filter(pet -> pet.getStatus() == status)
                .toList();
    }

    @Override
    public List<Pet> findPetsByTags(final List<String> tags, final Request request) {
        return pets.values().stream()
                .filter(pet -> pet.getTags() != null)
                .filter(pet -> pet.getTags().containsAll(tags))
                .toList();
    }

    @Override
    public void updatePetWithForm(final long petId, final String name, final String status, final Request request) {
        Pet pet = pets.get(petId);
        pet.setName(name);
        pet.setStatus(Pet.StatusEnum.valueOf(status));
    }

    @Override
    public Pet getPetById(final long petId, final Request request) {
        return pets.get(petId);
    }

    @Override
    public void deletePet(final String api_key, final long petId, final Request request) {
        this.pets.remove(petId);
    }

    @Override
    public ApiResponse uploadFile(final long petId, final String additionalMetadata, final Object body, final Request request) {
        return null;
    }
}
