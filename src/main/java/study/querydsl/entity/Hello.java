package study.querydsl.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
public class Hello {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String name;

    public Hello() {

    }

    public Hello(String name) {
        this.name = name;
    }
}
