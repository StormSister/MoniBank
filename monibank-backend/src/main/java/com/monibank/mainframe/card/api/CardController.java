package com.monibank.mainframe.card.api;

import com.monibank.mainframe.card.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CreateCardRequest request
    ) {

        return ResponseEntity.ok(
                cardService.createCard(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards() {

        return ResponseEntity.ok(
                cardService.getCards()
        );
    }

    @PatchMapping("/{cardId}/status")
    public ResponseEntity<CardResponse> changeStatus(
            @PathVariable
            @Pattern(regexp = "K\\d{12}")
            String cardId,
            @Valid @RequestBody ChangeCardStatusRequest request
    ) {

        return ResponseEntity.ok(
                cardService.changeStatus(
                        cardId,
                        request.status()
                )
        );
    }
}
