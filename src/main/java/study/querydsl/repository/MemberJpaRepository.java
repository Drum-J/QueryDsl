package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    /*
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }
    */

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);

        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAll_QueryDsl() {
        return query
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username =:username", Member.class)
                .setParameter("username",username)
                .getResultList();
    }

    public List<Member> findByUsername_QueryDsl(String username) {
        return query
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
}
