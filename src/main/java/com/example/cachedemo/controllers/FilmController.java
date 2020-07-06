package com.example.cachedemo.controllers;

import com.example.cachedemo.model.Film;
import com.example.cachedemo.services.FilmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class FilmController {

    private final FilmService filmService;
    private int page;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @RequestMapping(value = "/")
    public String allFilms(@RequestParam(defaultValue = "1") int page, Model model) {
        this.page = page;
        List<Film> films = filmService.allFilms(page);
        long filmsCount = filmService.filmsCount();
        long pagesCount = (filmsCount + 9) / 10;

        model.addAttribute("page", page);
        model.addAttribute("filmsList", films);
        model.addAttribute("filmsCount", filmsCount);
        model.addAttribute("pagesCount", pagesCount);
        return "films";
    }

    @RequestMapping(value = "/edit/{id}")
    public String editPage(@PathVariable int id, Model model) {
        Film film = filmService.getById(id);
        model.addAttribute("film", film);
        return "editPage";
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String editFilm(@ModelAttribute("film") Film film) {
        filmService.edit(film);
        return "redirect:/?page=" + this.page;
    }

    @RequestMapping(value = "/add")
    public String addPage() {
        return "editPage";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String addFilm(@ModelAttribute("film") Film film) {
        filmService.add(film);
        return "redirect:/?page=" + this.page;
    }

    @RequestMapping(value = "/delete/{id}")
    public String deleteFilm(@PathVariable int id) {
        Film film = filmService.getById(id);
        filmService.delete(film);
        return "redirect:/?page=" + this.page;
    }
}
