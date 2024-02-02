package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*; // Static Import 로 변경

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired EntityManager em;

    JPAQueryFactory query;
    @BeforeEach
    public void before() {
        query = new JPAQueryFactory(em); // 필드 레벨에서 처리해도 괜찮다고 한다.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 20, teamA);
        Member member2 = new Member("member2", 21, teamA);

        Member member3 = new Member("member3", 22, teamB);
        Member member4 = new Member("member4", 23, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() throws Exception {
        //member1을 찾아라.
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl() throws Exception {
        Member findMember = query.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() throws Exception {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(20)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() throws Exception {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1")
                        ,member.age.eq(20)) // .and() 대신 , 으로 해도 된다!
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() throws Exception {
        List<Member> fetch = query.selectFrom(member).fetch();

        Member fetchOne = query.selectFrom(member).fetchOne();

        Member fetchFirst = query.selectFrom(member).fetchFirst();

        QueryResults<Member> fetchResults = query.selectFrom(member).fetchResults(); // deprecated 되었다.
        fetchResults.getTotal(); // 총 갯수에 대한 카운트 쿼리를 한 번 더 날리게 된다. 그래서 getTotal()을 사용해서 총 갯수를 가져올 수 있음. getTotal 이 쿼리를 날리는 건 아니다.
        List<Member> results = fetchResults.getResults(); // 실제 데이터 이건 위의 .fetch() 와 같다
        //fetchResults.getLimit();
        //fetchResults.getOffset(); 이렇게 페이징 처리에 필요한 메서드도 제공한다.
        //Deprecated 되긴 했으나 QueryDsl의 변화가 빠르진 않기 때문에 사용은 가능하다고 한다. 하지만 몇 가지 문제가 있으니 fetch()를 사용하고 페이징은 직접 구현하는게 좋을 듯.

        long total1 = query.selectFrom(member).fetchCount(); //이것도 마찬가지로 deprecated 되었다. 아래와 같이 바꿔서 사용하는게 좋다.
        Long total2 = query.select(member.count())
                .from(member).fetchOne();
        
    }
}
