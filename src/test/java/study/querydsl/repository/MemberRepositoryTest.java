package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;

    @Test
    void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(()-> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));



        List<Member> findAll = memberRepository.findAll();

        List<Member> findByUsername = memberRepository.findByUsername("member1");

        assertThat(findMember).isEqualTo(member);
        assertThat(findAll).containsExactly(member);
        assertThat(findByUsername).containsExactly(member);
    }
}