package com.example.cachedemo.controllers;

import com.example.cachedemo.model.Film;
import com.example.cachedemo.services.FilmService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    private final FilmService filmService;

    public ApiController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping(value = "/films/{id}")
    public Film getFilm(@PathVariable int id) {
        return filmService.getById(id);
    }

    @PostMapping(value = "/films")
    public void getFilm(@RequestBody Film film) {
        filmService.add(film);
    }
}
