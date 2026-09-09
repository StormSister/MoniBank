package com.monibank.mainframe.transaction;

import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.transaction.api.CashTransactionRequest;
import com.monibank.mainframe.transaction.api.TransactionResponse;
import com.monibank.mainframe.transaction.mainframe.TransactionMainframeOperations;
import com.monibank.mainframe.transaction.mainframe.TransactionRecordMapper;
import com.monibank.mainframe.transaction.mainframe.TransactionRecordParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionRecordParser transactionRecordParser;
    private final KicksMainframeOperationExecutor
            kicksMainframeOperationExecutor;

    public TransactionResponse deposit(
            CashTransactionRequest request
    ) {

        return post(
                request,
                transactionRecordMapper.toDepositRecord(request)
        );
    }

    public TransactionResponse withdraw(
            CashTransactionRequest request
    ) {

        return post(
                request,
                transactionRecordMapper.toWithdrawalRecord(request)
        );
    }

    public List<TransactionResponse> getTransactions() {

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        TransactionMainframeOperations.LIST_TRANSACTIONS,
                        ""
                );

        return parseTransactions(result);
    }

    public List<TransactionResponse> getRecentTransactions(
            int limit
    ) {

        return getTransactions()
                .stream()
                .limit(limit)
                .toList();
    }

    public List<TransactionResponse> getTransactions(
            String accountId
    ) {

        validateAccountId(accountId);

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        TransactionMainframeOperations.LIST_TRANSACTIONS,
                        accountId
                );

        List<TransactionResponse> transactions =
                parseTransactions(result);

        if (transactions.stream().anyMatch(
                transaction -> !accountId.equals(
                        transaction.accountId()
                )
        )) {
            throw new IllegalStateException(
                    "LISTTXN returned a transaction for another account."
            );
        }

        return transactions;
    }

    private TransactionResponse post(
            CashTransactionRequest request,
            String inputRecord
    ) {

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        TransactionMainframeOperations.POST_TRANSACTION,
                        inputRecord
                );

        String createdTransactionId =
                result.header().entityId();

        validateTransactionId(createdTransactionId);

        List<TransactionResponse> transactions =
                parseTransactions(result);

        if (transactions.size() != 1) {
            throw new IllegalStateException(
                    "POSTTXN returned "
                            + transactions.size()
                            + " TXN records; expected 1"
            );
        }

        TransactionResponse transaction =
                transactions.getFirst();

        if (!createdTransactionId.equals(
                transaction.transactionId()
        )) {
            throw new IllegalStateException(
                    "POSTTXN returned transaction "
                            + transaction.transactionId()
                            + ", expected "
                            + createdTransactionId
            );
        }

        if (!request.accountId().equals(
                transaction.accountId()
        )) {
            throw new IllegalStateException(
                    "POSTTXN returned account "
                            + transaction.accountId()
                            + ", expected "
                            + request.accountId()
            );
        }

        return transaction;
    }

    private List<TransactionResponse> parseTransactions(
            MainframeResult result
    ) {

        return result.data()
                .stream()
                .filter(this::isTransaction)
                .map(MainframeDataRecord::payload)
                .map(transactionRecordParser::parse)
                .sorted(
                        Comparator.comparing(
                                        TransactionResponse::createdAt
                                )
                                .thenComparing(
                                        TransactionResponse::transactionId
                                )
                                .reversed()
                )
                .toList();
    }

    private boolean isTransaction(
            MainframeDataRecord record
    ) {
        return "TXN".equals(record.entityType());
    }

    private void validateTransactionId(
            String transactionId
    ) {

        if (transactionId == null
                || !transactionId.matches("T\\d{12}")) {
            throw new IllegalStateException(
                    "POSTTXN returned invalid transaction ID: "
                            + transactionId
            );
        }
    }

    private void validateAccountId(
            String accountId
    ) {

        if (accountId == null
                || !accountId.matches("A\\d{12}")) {
            throw new IllegalArgumentException(
                    "Account ID must match A followed by 12 digits."
            );
        }
    }
}
