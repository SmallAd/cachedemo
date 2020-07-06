package com.example.cachedemo.repositories;

import com.example.cachedemo.model.Film;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FilmRepository extends PagingAndSortingRepository<Film, Integer> {
}
