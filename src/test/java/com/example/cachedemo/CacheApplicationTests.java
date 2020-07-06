package com.example.cachedemo;

import com.example.cachedemo.model.Film;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Random;

class CacheApplicationTests {

    @BeforeEach
    void setUp(){

        for(int i = 0; i < 1000; i++) {
            Film film = getRandomFilm();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(film)
                    .post("/films")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }
    }

    @Test
    @Ignore
    void testCache() {

        for (int i = 0; i < 10000; i++) {
            RestAssured.given()
                    .get("/films/" + getRandomId(1000))
                    .then()
                    .statusCode(200);
        }

    }

    private int getRandomId(int bound) {
        return new Random().nextInt(bound);
    }


    private Film getRandomFilm() {
        Film film = Film.builder()
                .title(getRandomString(8))
                .year(new Random().nextInt(2020))
                .genre(getRandomString(5))
                .watched(new Random().nextBoolean())
                .build();

        return film;
    }

    private String getRandomString(int len){
        return RandomStringUtils.random(len, true, false);
    }

}
