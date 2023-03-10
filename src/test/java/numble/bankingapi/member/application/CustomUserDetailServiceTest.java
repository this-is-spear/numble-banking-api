package numble.bankingapi.member.application;

import static numble.bankingapi.fixture.MemberFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

import numble.bankingapi.member.domain.Member;
import numble.bankingapi.member.domain.MemberRepository;
import numble.bankingapi.member.exception.NotExistMemberException;

@Transactional
@SpringBootTest
class CustomUserDetailServiceTest {

	@Autowired
	UserDetailsService userDetailsService;
	@Autowired
	MemberRepository memberRepository;

	@Test
	@DisplayName("principal 을 입력해 사용자 정보를 조회한다.")
	void loadUserByUsername() {
		memberRepository.save(new Member(EMAIL, NAME, PASSWORD));

		assertDoesNotThrow(
			() -> userDetailsService.loadUserByUsername(EMAIL)
		);
	}

	@Test
	@DisplayName("principal 로 조회되지 않으면 NotExistMemberException 예외가 발생한다.")
	void loadUserByUsername_NotExistMember() {
		var invalidEmail = EMAIL;
		assertThatThrownBy(
			() -> userDetailsService.loadUserByUsername(invalidEmail)
		).isInstanceOf(NotExistMemberException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("principal 이 비어있으면 예외가 발생한다.")
	void loadUserByUsername_principalNotNull(String invalidEmail) {
		assertThatThrownBy(
			() -> userDetailsService.loadUserByUsername(invalidEmail)
		).isInstanceOf(NotExistMemberException.class);
	}
}
