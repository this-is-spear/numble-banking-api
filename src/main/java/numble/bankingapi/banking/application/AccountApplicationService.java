package numble.bankingapi.banking.application;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import numble.bankingapi.alarm.dto.AlarmMessage;
import numble.bankingapi.alarm.dto.TaskStatus;
import numble.bankingapi.alarm.dto.TaskType;
import numble.bankingapi.banking.domain.Account;
import numble.bankingapi.banking.domain.AccountHistory;
import numble.bankingapi.banking.domain.AccountNumber;
import numble.bankingapi.banking.domain.AccountService;
import numble.bankingapi.banking.domain.Money;
import numble.bankingapi.banking.domain.NotifyService;
import numble.bankingapi.banking.dto.HistoryResponse;
import numble.bankingapi.banking.dto.HistoryResponses;
import numble.bankingapi.banking.dto.TargetResponse;
import numble.bankingapi.banking.dto.TargetResponses;
import numble.bankingapi.banking.dto.TransferCommand;
import numble.bankingapi.banking.exception.InvalidMemberException;
import numble.bankingapi.member.domain.Member;
import numble.bankingapi.member.domain.MemberService;
import numble.bankingapi.social.domain.Friend;
import numble.bankingapi.social.domain.FriendService;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {
	private final MemberService memberService;
	private final FriendService friendService;
	private final AccountService accountService;
	private final NotifyService notifyService;
	private final ConcurrencyFacade concurrencyFacade;

	public HistoryResponses getHistory(String principal, String stringAccountNumber) {
		final var accountNumber = getAccountNumber(stringAccountNumber);
		final var account = accountService.getAccountByAccountNumber(accountNumber);
		validateMember(principal, account);

		return new HistoryResponses(account.getBalance(),
			accountService.findAccountHistoriesByFromAccountNumber(account)
				.stream().map(this::getHistoryResponse).collect(Collectors.toList())
		);
	}

	public void deposit(String principal, String number, Money money) {
		final var accountNumber = getAccountNumber(number);
		final var account = accountService.getAccountByAccountNumber(accountNumber);
		validateMember(principal, account);

		concurrencyFacade.depositWithLock(accountNumber, money);
		notifyService.notify(account.getUserId(),
			new AlarmMessage(TaskStatus.SUCCESS, TaskType.DEPOSIT));
	}

	public void withdraw(String principal, String number, Money money) {
		final var accountNumber = getAccountNumber(number);
		final var account = accountService.getAccountByAccountNumber(accountNumber);
		validateMember(principal, account);

		concurrencyFacade.withdrawWithLock(accountNumber, money);
		notifyService.notify(account.getUserId(),
			new AlarmMessage(TaskStatus.SUCCESS, TaskType.WITHDRAW));
	}

	public void transfer(String principal, String accountNumber, TransferCommand command) {
		final var fromAccountNumber = getAccountNumber(accountNumber);
		final var toAccountNumber = getAccountNumber(command.toAccountNumber());

		final var fromAccount = accountService.getAccountByAccountNumber(fromAccountNumber);
		final var toAccount = accountService.getAccountByAccountNumber(toAccountNumber);

		validateMember(principal, fromAccount);
		Money money = command.amount();

		concurrencyFacade.transferWithLock(fromAccountNumber, toAccountNumber, money);

		notifyService.notify(fromAccount.getUserId(),
			new AlarmMessage(TaskStatus.SUCCESS, TaskType.TRANSFER));
		notifyService.notify(toAccount.getUserId(),
			new AlarmMessage(TaskStatus.SUCCESS, TaskType.DEPOSIT));
	}

	private void validateMember(String principal, Account account) {
		final var member = memberService.findByEmail(principal);

		if (!member.getId().equals(account.getUserId())) {
			throw new InvalidMemberException();
		}
	}

	public TargetResponses getTargets(String principal, String stringAccountNumber) {
		final var accountNumber = new AccountNumber(stringAccountNumber);
		final var account = accountService.getAccountByAccountNumber(accountNumber);

		final var member = memberService.findByEmail(principal);
		if (!member.getId().equals(account.getUserId())) {
			throw new InvalidMemberException();
		}

		final var friendIds = friendService.findFriends(member.getId())
			.stream()
			.map(Friend::getToMemberId)
			.toList();

		final var targetList = memberService.findAllById(friendIds);
		final var targetResponseList = accountService.getFriendAccounts(friendIds)
			.stream()
			.map(friendAccount -> {
				Member friend = targetList.stream()
					.filter(target -> target.getId().equals(friendAccount.getUserId()))
					.findFirst()
					.orElseThrow(IllegalArgumentException::new);
				return new TargetResponse(friend.getName(), friend.getEmail(), friendAccount.getAccountNumber());
			}).collect(Collectors.toList());

		return new TargetResponses(targetResponseList);
	}

	private HistoryResponse getHistoryResponse(AccountHistory accountHistory) {
		return new HistoryResponse(accountHistory.getType(), accountHistory.getMoney(),
			accountHistory.getFromAccountNumber(), accountHistory.getToAccountNumber(),
			accountHistory.getCreatedDate());
	}

	private AccountNumber getAccountNumber(String accountNumber) {
		return new AccountNumber(accountNumber);
	}
}
