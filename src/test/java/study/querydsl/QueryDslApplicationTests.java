package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.controller.HelloController;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class QueryDslApplicationTests {

    @Autowired EntityManager em;

    @Test
    @Transactional
    void contextLoads() {
        Hello hello = new Hello("승호");
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);

        QHello qHello = QHello.hello;

        Hello findHello = query.selectFrom(qHello).fetchOne();

        assertThat(hello).isEqualTo(findHello);
        assertThat(hello.getId()).isEqualTo(findHello.getId());
        assertThat(hello.getName()).isEqualTo(findHello.getName());
    }

}
