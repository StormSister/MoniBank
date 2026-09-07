package com.monibank.mainframe.card;

import com.monibank.mainframe.card.api.CardResponse;
import com.monibank.mainframe.card.api.CreateCardRequest;
import com.monibank.mainframe.card.mainframe.CardMainframeOperations;
import com.monibank.mainframe.card.mainframe.CardRecordMapper;
import com.monibank.mainframe.card.mainframe.CardRecordParser;
import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRecordMapper cardRecordMapper;
    private final CardRecordParser cardRecordParser;
    private final KicksMainframeOperationExecutor
            kicksMainframeOperationExecutor;

    public List<CardResponse> getCards() {

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        CardMainframeOperations.LIST_CARDS,
                        ""
                );

        return parseCards(result);
    }

    public CardResponse createCard(
            CreateCardRequest request
    ) {

        String inputRecord =
                cardRecordMapper.toRecord(request);

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        CardMainframeOperations.ADD_CARD,
                        inputRecord
                );

        String createdCardId =
                result.header().entityId();

        validateCardId(
                createdCardId,
                CardMainframeOperations.ADD_CARD
        );

        return parseSingleCard(
                result,
                createdCardId,
                CardMainframeOperations.ADD_CARD
        );
    }

    public CardResponse changeStatus(
            String cardId,
            String status
    ) {

        String inputRecord =
                cardRecordMapper.toStatusUpdateRecord(
                        cardId,
                        status
                );

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        CardMainframeOperations.CHANGE_STATUS,
                        inputRecord
                );

        return parseSingleCard(
                result,
                cardId,
                CardMainframeOperations.CHANGE_STATUS
        );
    }

    private List<CardResponse> parseCards(
            MainframeResult result
    ) {

        return result.data()
                .stream()
                .filter(this::isCard)
                .map(MainframeDataRecord::payload)
                .map(cardRecordParser::parse)
                .toList();
    }

    private CardResponse parseSingleCard(
            MainframeResult result,
            String expectedCardId,
            String operation
    ) {

        List<CardResponse> cards =
                parseCards(result);

        if (cards.size() != 1) {
            throw new IllegalStateException(
                    operation
                            + " returned "
                            + cards.size()
                            + " CARD records; expected 1"
            );
        }

        CardResponse card =
                cards.getFirst();

        if (!expectedCardId.equals(
                card.cardId()
        )) {
            throw new IllegalStateException(
                    operation
                            + " returned card "
                            + card.cardId()
                            + ", expected "
                            + expectedCardId
            );
        }

        return card;
    }

    private boolean isCard(
            MainframeDataRecord record
    ) {

        return "CARD".equals(
                record.entityType()
        );
    }

    private void validateCardId(
            String cardId,
            String operation
    ) {

        if (cardId == null
                || !cardId.matches("K\\d{12}")) {
            throw new IllegalStateException(
                    operation
                            + " returned invalid card ID: "
                            + cardId
            );
        }
    }
}
