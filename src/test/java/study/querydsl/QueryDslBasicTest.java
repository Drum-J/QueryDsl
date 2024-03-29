package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*; // 서브쿼리 Static Import
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*; // Static Import 로 변경
import static study.querydsl.entity.QTeam.*;

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

    //@Test
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
    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() throws Exception {
        //데이터 추가
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> fetch = query.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member memberNull = fetch.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isEqualTo(null);

    }

    @Test
    void paging1() throws Exception {
        List<Member> result = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2_Deprecated() throws Exception {
        QueryResults<Member> results = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4); // select count(m.id) from Member m
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @Test
    void paging3() throws Exception {
        // .fetchResults() 가 deprecated 됨에 따라 우선 이렇게 count 쿼리를 따로 작성해야 한다는 것만 알아두자.

        List<Member> members = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Long total = query.select(member.count())
                .from(member)
                .fetchOne();

        assertThat(members.size()).isEqualTo(2);
        assertThat(total).isEqualTo(4);
    }

    @Test
    void aggregation() throws Exception {
        // 실무에서는 Tuple 보다는 DTO로 직접 뽑아오는 걸 더 많이 사용한다고 한다.
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(86);
        assertThat(tuple.get(member.age.avg())).isEqualTo(21.5);
        assertThat(tuple.get(member.age.max())).isEqualTo(23);
        assertThat(tuple.get(member.age.min())).isEqualTo(20);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void groupBy() throws Exception {
        List<Tuple> result = query
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(20.5);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(22.5);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    void basicJoin() throws Exception {
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 (연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    void thetaJoin() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조회, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    void join_on_filtering() throws Exception {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team) //member.team , team 이 아니라 그냥 team 만 들어가 있다!!
                // join(member.team, team) 사용하면 자동으로 on member.team.id = team.id 이렇게 생긴다.
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit EntityManagerFactory emf;
    @Test
    void fetchJoinNo() throws Exception {
        // fetch join 테스트를 위한 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne(); // 여기서는 Member 만 조회한다. Team은 LAZY 이기 때문에

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoinUse() throws Exception {
        // fetch join 테스트를 위한 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .join(member.team,team)
                .fetchJoin() //Member와 Team이 한번에 조회한다.
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() throws Exception {
        QMember subMember = new QMember("subMember"); // 서브쿼리용 QMember 생성 alias 가 겹치면 안되기 때문

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        // 서브쿼리 사용
                        select(subMember.age.max())
                                .from(subMember)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(23);

        assertThat(result.get(0).getUsername()).isEqualTo("member4");
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    void subQueryGoe() throws Exception {
        QMember subMember = new QMember("subMember");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        select(subMember.age.avg())
                                .from(subMember)
                ))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("age").containsExactly(22, 23);
    }

    @Test
    void subQueryIn() throws Exception {
        QMember subMember = new QMember("subMember");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        select(subMember.age)
                                .from(subMember)
                                .where(subMember.age.gt(20))
                ))
                .fetch();

        //List<Member> result = query.selectFrom(member).where(member.age.gt(20)).fetch();

        assertThat(result.size()).isEqualTo(3);
        assertThat(result).extracting("age").containsExactly(21,22, 23);
    }

    @Test
    void selectSubQuery() throws Exception {
        QMember subMember = new QMember("subMember");

        List<Tuple> result = query
                .select(member.username,
                        select(subMember.age.avg())
                                .from(subMember))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void basicCase() throws Exception {
        List<String> result = query
                .select(member.age
                        .when(20).then("스무살")
                        .when(21).then("스물한살")
                        .otherwise("기타")) // SQL과 달리 case, end 없이 바로 when then otherwise(else)를 사용한다.
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() throws Exception {
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("30살 이상"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() throws Exception {
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection() throws Exception {
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("username = " + s);
        }
    }

    @Test
    void tupleProjection() throws Exception {
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoBySetter() throws Exception {
        // MemberDto의 기본 생성자가 있어야 한다. QueryDsl이 MemberDto의 기본 생성자를 만든 이후에 Setter 를 사용해서 데이터를 설정하기 때문
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class, // 타입 지정 -> MemberDto 타입으로 가져올 것이다.
                        member.username, // 꺼내올 값 지정 -> username 과 age 를 가져올 것이다.
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDtoBySetter() throws Exception {
        List<UserDto> result = query
                .select(Projections.bean(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByField() throws Exception {
        //필드에다가 바로 값을 꽂아버린다. getter,setter 필요가 없음. 단! 기본 생성자는 마찬가지로 필요하다.
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class, // 타입 지정 -> MemberDto 타입으로 가져올 것이다.
                        member.username, // 꺼내올 값 지정 -> username 과 age 를 가져올 것이다.
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDto() throws Exception {
        QMember subMember = new QMember("subMember");

        List<UserDto> result = query
                .select(Projections.fields(UserDto.class,
                        //member.username, // UserDto의 필드명과 일치 하지 않아서 무시된다. null처리됨
                        member.username.as("name"), // 별칭을 사용해서 필드명과 일치시켜준다.

                        // member.age,
                        ExpressionUtils.as(JPAExpressions
                                        .select(subMember.age.max())
                                        .from(subMember),"age")
                        // 서브쿼리를 사용할 경우에는 별칭을 주지 못하기 때문에 ExpressionUtils.as(source,alias) 를 사용해서 별칭을 지정해줘야 한다.
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByConstructor() throws Exception {
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class, // 타입 지정 -> MemberDto 타입으로 가져올 것이다.
                        member.username, // 꺼내올 값 지정 -> username 과 age 를 가져올 것이다.
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDtoBuConstructor() throws Exception {
        //생성자를 통한 DTO 반환의 경우에는 별칭을 사용하지 않아도 된다. 생성자와 동일한 파라미터를 넘겨주기만 하면 OK
        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 20;
        
        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 20;

        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return query
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
                //.where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    void bulkUpdate() throws Exception {
        //member1.age = 20 -> 비회원
        //member2.age = 21 -> 비회원
        //member3.age = 22
        //member4.age = 23

        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(22))
                .execute();

        //매우 중요!! Bulk 연산 후에는 영속성 컨텍스트를 초기화 해주는 게 좋다.
        em.flush();
        em.clear();

        System.out.println("count = " + count); //업데이트 수행 횟수를 반환한다.
    }

    @Test
    void bulkAdd() throws Exception {
        long addAge = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() throws Exception {
        long delete = query
                .delete(member)
                .where(member.age.gt(22))
                .execute();
    }

    @Test
    void sqlFunction_replace() throws Exception {
        List<String> result = query
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        System.out.println("result = " + result);
    }

    @Test
    void sqlFunction_lower() throws Exception {
        List<String> result = query
                .select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate(
                        "function('lower', {0})"
                        , member.username)))
                //.where(member.username.eq(member.username.lower()))
                .fetch();

        System.out.println("result = " + result);
    }
}
