package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired MemberJpaRepository memberRepository;

    @Test
    void basicTest() throws Exception {
        //given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        //when
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

        List<Member> findAll = memberRepository.findAll();

        List<Member> findByUsername = memberRepository.findByUsername("member1");

        //then
        assertThat(findMember).isEqualTo(member);
        assertThat(findAll).containsExactly(member);
        assertThat(findByUsername).containsExactly(member);
    }

    @Test
    void basicQueryDslTest() throws Exception {
        //given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        //when
        List<Member> findAll = memberRepository.findAll_QueryDsl();

        List<Member> findByUsername = memberRepository.findByUsername_QueryDsl("member1");

        //then
        assertThat(findAll).containsExactly(member);
        assertThat(findByUsername).containsExactly(member);
    }

}