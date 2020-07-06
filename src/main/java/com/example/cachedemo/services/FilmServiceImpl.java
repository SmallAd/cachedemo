package com.example.cachedemo.services;

import com.example.cachedemo.model.Film;
import com.example.cachedemo.repositories.FilmRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FilmServiceImpl implements FilmService {

    private final FilmRepository filmRepository;

    public FilmServiceImpl(FilmRepository filmRepository) {
        this.filmRepository = filmRepository;
    }
    
    @Override
    @Transactional
    public long filmsCount() {
        return filmRepository.count();
    }

    @Override
    @Transactional
    public List<Film> allFilms(int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Film> films = filmRepository.findAll(pageable);
        return films.toList();
    }

    @Override
    @Transactional
    public void add(Film film) {
        filmRepository.save(film);
    }

    @Override
    @Transactional
    public void delete(Film film) {
        filmRepository.delete(film);
    }

    @Override
    @Transactional
    public void edit(Film film) {
        filmRepository.save(film);
    }

    @Override
    @Transactional
    public Film getById(int id) {
        return filmRepository.findById(id).get();
    }

}
