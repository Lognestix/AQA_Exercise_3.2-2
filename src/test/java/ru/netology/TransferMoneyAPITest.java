package ru.netology;

import com.github.javafaker.CreditCardType;
import com.github.javafaker.Faker;
import lombok.SneakyThrows;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.netology.data.CardTransaction;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.netology.data.DataHelper.Auth.authUser;
import static ru.netology.data.DataHelper.Cards.*;
import static ru.netology.data.DataHelper.Verification.userVerification;

class TransferMoneyAPITest {

    @Test
    @DisplayName("Transfer between own cards positive scenario")
    void shouldTransferBetweenOwnCards() {
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
    void shouldTransferToAnotherCard() {
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
    void shouldTransferFromAnotherCard() {
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
    void shouldTransferBetweenOwnCardsNegative() {
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
    void shouldTransferToAnotherCardNegative() {
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
    @SneakyThrows
    public void cleaningAndAlignment() {
        var runner = new QueryRunner();
        var codeDelSQL = "DELETE FROM auth_codes;";
        var transactSQL = "SELECT * FROM card_transactions;";
        var transactDelSQL = "DELETE FROM card_transactions;";

        try (var connection = DriverManager.getConnection(
                "jdbc:mysql://185.119.57.164:3306/base",
                "adm", "9mRE")) {
            runner.update(connection, codeDelSQL);
            authUser();
            var token = userVerification();
            runner.update(connection, codeDelSQL);
            var info = runner.query(connection, transactSQL,
                    new BeanHandler<>(CardTransaction.class));
            transferMoney(token, info.getTarget(), info.getSource(),
                    info.getAmount_in_kopecks() / 100);
            runner.update(connection, transactDelSQL);
        }
    }
}