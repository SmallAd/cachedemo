package com.example.cachedemo.cache;

import com.example.cachedemo.model.Film;

public class FilmCache2Q extends Cache2QImpl<Integer, Film> {

    public FilmCache2Q(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(Integer key, Film film) {
        return (film.getTitle().length() + film.getGenre().length()) * 2 + 9;
    }
}
