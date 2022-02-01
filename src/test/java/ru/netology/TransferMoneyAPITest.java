package ru.netology;

import com.github.javafaker.CreditCardType;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.netology.data.DataHelper.Auth.authUser;
import static ru.netology.data.DataHelper.Cards.*;
import static ru.netology.data.DataHelper.Verification.userVerification;
import static ru.netology.data.DataHelper.cleaningAndAlignment;

class TransferMoneyAPITest {

    @Test
    @DisplayName("Transfer between own cards positive scenario")
    public void shouldTransferBetweenOwnCards() {
        authUser();
        //Получение токена авторизации:
        var token = userVerification();
        //Получение id и начального баланса своих карт:
        var cardsUpTo = getCardsAPI(token);
        //Получение номеров карт:
        var from = getNumberCards(cardsUpTo.get(0).getId());
        var to = getNumberCards(cardsUpTo.get(1).getId());
        //Осуществление перевода:
        int amount = 5_000;
        transferMoney(token, from, to, amount);
        //Получение id и итогового баланса своих карт:
        var cardsAfter = getCardsAPI(token);
        //Проверка баланса первой карты:
        assertEquals(cardsUpTo.get(0).getBalance() - amount,
                cardsAfter.get(0).getBalance());
        //Проверка баланса второй карты:
        assertEquals(cardsUpTo.get(1).getBalance() + amount,
                cardsAfter.get(1).getBalance());
    }

    @Test
    @DisplayName("Transfer to another card positive scenario")
    public void shouldTransferToAnotherCard() {
        Faker faker = new Faker();
        authUser();
        //Получение токена авторизации:
        var token = userVerification();
        //Получение id и начального баланса своих карт:
        var cardsUpTo = getCardsAPI(token);
        //Получение номеров карт:
        var from = getNumberCards(cardsUpTo.get(0).getId());
        var to = faker.finance().creditCard(CreditCardType.MASTERCARD);
        //Осуществление перевода:
        int amount = 5_000;
        transferMoney(token, from, to, amount);
        //Получение id и итогового баланса своих карт:
        var cardsAfter = getCardsAPI(token);
        //Проверка баланса своей карты:
        assertEquals(cardsUpTo.get(0).getBalance() - amount,
                cardsAfter.get(0).getBalance());
    }

    @Test
    @DisplayName("Transfer from another card positive scenario")
    public void shouldTransferFromAnotherCard() {
        Faker faker = new Faker();
        authUser();
        //Получение токена авторизации:
        var token = userVerification();
        //Получение id и начального баланса своих карт:
        var cardsUpTo = getCardsAPI(token);
        //Получение номеров карт:
        var from = faker.finance().creditCard(CreditCardType.MASTERCARD);
        var to = getNumberCards(cardsUpTo.get(0).getId());
        //Осуществление перевода:
        int amount = 5_000;
        transferMoney(token, from, to, amount);
        //Получение id и итогового баланса своих карт:
        var cardsAfter = getCardsAPI(token);
        //Проверка баланса своей карты:
        assertEquals(cardsUpTo.get(0).getBalance() + amount,
                cardsAfter.get(0).getBalance());
    }

    @Test
    @DisplayName("Transfer between own cards is a negative scenario, " +
            "the write-off exceeds the card balance")
    public void shouldTransferBetweenOwnCardsNegative() {
        authUser();
        //Получение токена авторизации:
        var token = userVerification();
        //Получение id и начального баланса своих карт:
        var cardsUpTo = getCardsAPI(token);
        //Получение номеров карт:
        var from = getNumberCards(cardsUpTo.get(0).getId());
        var to = getNumberCards(cardsUpTo.get(1).getId());
        //Осуществление перевода:
        int amount = 11_000;
        transferMoney(token, from, to, amount);
        //Получение id и итогового баланса своих карт:
        var cardsAfter = getCardsAPI(token);
        //Проверка баланса первой карты:
        assertEquals(cardsUpTo.get(0).getBalance(),
                cardsAfter.get(0).getBalance());
        //Проверка баланса второй карты:
        assertEquals(cardsUpTo.get(1).getBalance(),
                cardsAfter.get(1).getBalance());
    }

    @Test
    @DisplayName("Transfer to another card negative scenario, " +
            "transfer of a negative amount")
    public void shouldTransferToAnotherCardNegative() {
        Faker faker = new Faker();
        authUser();
        //Получение токена авторизации:
        var token = userVerification();
        //Получение id и начального баланса своих карт:
        var cardsUpTo = getCardsAPI(token);
        //Получение номеров карт:
        var from = getNumberCards(cardsUpTo.get(0).getId());
        var to = faker.finance().creditCard(CreditCardType.MASTERCARD);
        //Осуществление перевода:
        int amount = -5_000;
        transferMoney(token, from, to, amount);
        //Получение id и итогового баланса своих карт:
        var cardsAfter = getCardsAPI(token);
        //Проверка баланса своей карты:
        assertEquals(cardsUpTo.get(0).getBalance(),
                cardsAfter.get(0).getBalance());
    }

    @AfterEach
    public void clearingTablesAfterTests() {
        cleaningAndAlignment();
    }
}