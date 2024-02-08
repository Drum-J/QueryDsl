package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QueryDslApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryDslApplication.class, args);
    }

    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
