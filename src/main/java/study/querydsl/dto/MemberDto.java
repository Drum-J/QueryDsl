package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberDto {

    private String username;
    private int age;

    public MemberDto() {

    }

    @QueryProjection // gradle 의 compileJava 를 하면 DTO도 QClass 생성
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
