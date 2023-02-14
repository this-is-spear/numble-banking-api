package numble.bankingapi.banking.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import numble.bankingapi.banking.domain.Account;
import numble.bankingapi.banking.domain.AccountNumber;
import numble.bankingapi.banking.domain.AccountRepository;

@Repository
public interface JpaAccountRepository extends JpaRepository<Account, Long>, AccountRepository {
	@Override
	Optional<Account> findById(Long id);
	@Override
	Optional<Account> findByAccountNumber(AccountNumber accountNumber);

	@Override
	<S extends Account> S save(S entity);
}