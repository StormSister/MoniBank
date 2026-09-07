package com.monibank.mainframe.account;

import com.monibank.mainframe.account.api.AccountResponse;
import com.monibank.mainframe.account.api.CreateAccountRequest;
import com.monibank.mainframe.account.mainframe.AccountMainframeOperations;
import com.monibank.mainframe.account.mainframe.AccountRecordMapper;
import com.monibank.mainframe.account.mainframe.AccountRecordParser;
import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRecordMapper accountRecordMapper;
    private final AccountRecordParser accountRecordParser;
    private final KicksMainframeOperationExecutor
            kicksMainframeOperationExecutor;

    public List<AccountResponse> getAccounts() {

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        AccountMainframeOperations.LIST_ACCOUNTS,
                        ""
                );

        return parseAccounts(result);
    }

    public AccountResponse createAccount(
            CreateAccountRequest request
    ) {

        String inputRecord =
                accountRecordMapper.toRecord(request);

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        AccountMainframeOperations.ADD_ACCOUNT,
                        inputRecord
                );

        String createdAccountId =
                result.header().entityId();

        validateAccountId(
                createdAccountId,
                AccountMainframeOperations.ADD_ACCOUNT
        );

        return parseSingleAccount(
                result,
                createdAccountId,
                AccountMainframeOperations.ADD_ACCOUNT
        );
    }

    public AccountResponse changeStatus(
            String accountId,
            String status
    ) {

        String inputRecord =
                accountRecordMapper.toStatusUpdateRecord(
                        accountId,
                        status
                );

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        AccountMainframeOperations.CHANGE_STATUS,
                        inputRecord
                );

        return parseSingleAccount(
                result,
                accountId,
                AccountMainframeOperations.CHANGE_STATUS
        );
    }

    private List<AccountResponse> parseAccounts(
            MainframeResult result
    ) {

        return result.data()
                .stream()
                .filter(this::isAccount)
                .map(MainframeDataRecord::payload)
                .map(accountRecordParser::parse)
                .toList();
    }

    private AccountResponse parseSingleAccount(
            MainframeResult result,
            String expectedAccountId,
            String operation
    ) {

        List<AccountResponse> accounts =
                parseAccounts(result);

        if (accounts.size() != 1) {
            throw new IllegalStateException(
                    operation
                            + " returned "
                            + accounts.size()
                            + " ACCOUNT records; expected 1"
            );
        }

        AccountResponse account =
                accounts.getFirst();

        if (!expectedAccountId.equals(
                account.accountId()
        )) {
            throw new IllegalStateException(
                    operation
                            + " returned account "
                            + account.accountId()
                            + ", expected "
                            + expectedAccountId
            );
        }

        return account;
    }

    private boolean isAccount(
            MainframeDataRecord record
    ) {

        return "ACCOUNT".equals(
                record.entityType()
        );
    }

    private void validateAccountId(
            String accountId,
            String operation
    ) {

        if (accountId == null
                || !accountId.matches("A\\d{12}")) {
            throw new IllegalStateException(
                    operation
                            + " returned invalid account ID: "
                            + accountId
            );
        }
    }
}
