package io.github.potjerodekool.demo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class PersonController {

    @PostMapping("/persons")
    public ResponseEntity<Void> createPerson(final @RequestBody Person person) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<Person> getPersonById(final @PathVariable("id") int id) {
        return ResponseEntity.ok(new Person());
    }
}
