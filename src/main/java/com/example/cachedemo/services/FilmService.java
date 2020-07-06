package com.example.cachedemo.services;


import com.example.cachedemo.model.Film;

import java.util.List;

public interface FilmService {

    List<Film> allFilms(int page);

    long filmsCount();

    void add(Film film);

    void delete(Film film);

    void edit(Film film);

    Film getById(int id);
}
