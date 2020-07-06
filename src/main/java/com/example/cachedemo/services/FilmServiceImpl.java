package com.example.cachedemo.services;

import com.example.cachedemo.cache.Cache;
import com.example.cachedemo.model.Film;
import com.example.cachedemo.repositories.FilmRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FilmServiceImpl implements FilmService {

    private final FilmRepository filmRepository;
    private final Cache<Integer, Film> filmCache;

    public FilmServiceImpl(FilmRepository filmRepository, Cache<Integer, Film> filmCache) {
        this.filmRepository = filmRepository;
        this.filmCache = filmCache;
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
        Film filmResult = filmRepository.save(film);
        filmCache.put(filmResult.getId(), filmResult);
        log.info(filmCache.toString());
    }

    @Override
    @Transactional
    public void delete(Film film) {
        filmRepository.delete(film);
        filmCache.remove(film.getId());
        log.info(filmCache.toString());
    }

    @Override
    @Transactional
    public void edit(Film film) {
        filmRepository.save(film);
        filmCache.put(film.getId(), film);
        log.info(filmCache.toString());
    }

    @Override
    @Transactional
    public Film getById(int id) {
        Film resultFilm = filmCache.get(id);
        log.info(filmCache.toString());
        if (resultFilm != null) {
            return resultFilm;
        }
        Optional<Film> filmOptional = filmRepository.findById(id);
        if (filmOptional.isPresent()) {
            resultFilm = filmOptional.get();
            filmCache.put(resultFilm.getId(), resultFilm);
        }
        return resultFilm;
    }

}
