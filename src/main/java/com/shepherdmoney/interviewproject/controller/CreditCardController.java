package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ListIterator;
import java.time.Instant;
import java.util.LinkedList;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    @Autowired
    private UserRepository users;
    @Autowired 
    private CreditCardRepository cards;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        if (users.existsById(payload.getUserId())) {
            // User exists. Create a new card based on payload information.
            CreditCard newCard = new CreditCard(
                payload.getCardIssuanceBank(), 
                payload.getCardNumber(), 
                users.getReferenceById(payload.getUserId()));
            // Save the card to the cards repository and update database.
            cards.save(newCard);
            // Return an OK response with the card's id.
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(newCard.getId());
        } else {
            // User does not exist. Return a BAD_REQUEST response.
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(400);
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        List<CreditCardView> cardViews = new LinkedList<CreditCardView>();
        if (users.existsById(userId)) {
            // Iterate through the user's list of credit cards
            CreditCardView view;
            for (CreditCard c : users.getReferenceById(userId).getCreditCards()) {
                view = new CreditCardView(c.getIssuanceBank(), c.getNumber());
                cardViews.add(view);
            }
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(cardViews);
        } else {
            // User does not exist. Return a BAD_REQUEST response with an empty list.
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cardViews);
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        CreditCard card = cards.getByNumber(creditCardNumber);
        if (card == null) {
            // Card does not exist / No user owns this card. Return a BAD_REQUEST response with -1
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(400);
        } else {
            // Return a OK responsee with the user's id
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(card.getUser().getId());
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        
        // Do an initial check on the transactions. If all cards that need be updated exist, continue. Otherwise, return a 400 BAD_REQUEST
        for (UpdateBalancePayload u : payload) {
            if(cards.getByNumber(u.getCreditCardNumber()) == null) {
                // Card number is not associated with a card. Return a BAD_REQUEST
                return ResponseEntity   
                .status(HttpStatus.BAD_REQUEST)
                .body(400);
            }
            
        }
        // All cards must be valid. Proceed to update every card's balance history.
        CreditCard card;
        LinkedList<BalanceHistory> cardHistory;
        for (UpdateBalancePayload u : payload) {
            card = cards.getByNumber(u.getCreditCardNumber());
            cardHistory = card.getBalanceHistory();
            // cardHistory is in chronological order. Iterate through it while the transaction date is less or equal to the current date.
            ListIterator<BalanceHistory> iter = cardHistory.listIterator();
            // This variable keeps track of where to insert a Balance History corresponding to the current transaction.
            int positionInList = 0;
            while (iter.hasNext()) {
                BalanceHistory curr = iter.next();
                // Compare curr's time to the transactions
                if (u.getTransactionTime().compareTo(curr.getDate()) >= 0) {
                    positionInList++;
                    // Update curr's value. Since ListIterator returns a reference, balanceHistory will represent the updated list at end of loop.
                    curr.setBalance(curr.getBalance() + u.getTransactionAmount());
                    // If the time was equal, break out the loop to avoid adding an additional node.
                    if (u.getTransactionTime().compareTo(curr.getDate()) == 0) {
                        positionInList = -1;
                        break;
                    }
                } else {
                    break;
                }  
            }
            // If positionInList is not -1, we must insert a new Balance History.
            if (positionInList >= 0) {
                // The new balance will be the balance prior to the current one + transactionAmount
                double newBalance = !cardHistory.isEmpty() ? cardHistory.get(positionInList).getBalance() + u.getTransactionAmount() : u.getTransactionAmount();
                BalanceHistory newHistory = new BalanceHistory(u.getTransactionTime(), card);
                newHistory.setBalance(newBalance);
                cardHistory.add(positionInList, newHistory);
            }
            // Lastly, add a balance history representing the current date.
            BalanceHistory currBalanceHistory = new BalanceHistory(Instant.now(), card);
            currBalanceHistory.setBalance(cardHistory.getFirst().getBalance());
            cardHistory.addFirst(currBalanceHistory);
            card.setBalanceHistory(cardHistory);
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(200);
    }
    
}
