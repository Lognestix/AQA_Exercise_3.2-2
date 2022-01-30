## В build.gradle добавленна поддержка JUnit-Jupiter, Lombok, JavaFaker, Rest-Assured, Gson, MySQL-connector-Java, Commons-DBUtils.
```gradle
plugins {
    id 'java'
}

group 'ru.netology'
version '1.0-SNAPSHOT'

sourceCompatibility = 11

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.22'
    annotationProcessor 'org.projectlombok:lombok:1.18.22'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2'
    testImplementation 'com.github.javafaker:javafaker:1.0.2'
    testImplementation 'io.rest-assured:rest-assured:4.4.0'
    testImplementation 'com.google.code.gson:gson:2.8.9'
    testImplementation 'mysql:mysql-connector-java:8.0.28'
    testImplementation 'commons-dbutils:commons-dbutils:1.7'

    testCompileOnly 'org.projectlombok:lombok:1.18.22'
    testAnnotationProcessor 'org.projectlombok:lombok:1.18.22'
}

test {
    useJUnitPlatform()
}
```
## Код Java для оптимизации авто-тестов.
```Java
package ru.netology.data;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class DataHelper {
    private static final RequestSpecification requestSpec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(9999)
            .setAccept(ContentType.JSON)
            .setContentType(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();
    private static final String login = "vasya";
    private static final String pass = "qwerty123";

    private DataHelper() {
    }

    private static void sendRequestAuth(AuthUserData user) {
        //Запрос
        given() //"дано"
                .spec(requestSpec)              //Указывается, какая спецификация используется
                .body(new AuthUserData(         //Передача в теле объекта, который будет преобразован в JSON,
                        user.getLogin(),        //собственно логин,
                        user.getPassword()))    //пароль.
                .when()                         //"когда"
                .post("/api/auth")     //На какой путь, относительно BaseUri отправляется запрос
                .then()                         //"тогда ожидаем"
                .statusCode(200);           //Код 200, все хорошо
    }

    public static class Auth {
        private Auth() {
        }

        public static void authUser() {
            AuthUserData user = new AuthUserData(login, pass);
            sendRequestAuth(user);
        }
    }

    @Value
    public static class AuthUserData {
        private String login;
        private String password;
    }

    private static String sendRequestVerification(UserVerificationData code) {
        String token = given()
                .spec(requestSpec)
                .body(new UserVerificationData(
                        code.getLogin(),
                        code.getCode()))
                .when()
                .post("/api/auth/verification")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
        return token;
    }

    public static class Verification {
        private Verification() {
        }

        @SneakyThrows
        private static String getCode() {
            var runner = new QueryRunner();
            var userSQL = "SELECT id FROM users WHERE login = ?;";
            var codeSQL = "SELECT code FROM auth_codes WHERE user_id = ?;";

            try (var connection = DriverManager.getConnection(
                    "jdbc:mysql://185.119.57.164:3306/base", "adm", "9mRE")) {
                String userId = runner.query(connection, userSQL, login, new ScalarHandler<>());
                String code = runner.query(connection, codeSQL, userId, new ScalarHandler<>());
                return code;
            }
        }

        public static String userVerification() {
            UserVerificationData code = new UserVerificationData(login, getCode());
            return sendRequestVerification(code);
        }
    }

    @Value
    public static class UserVerificationData {
        private String login;
        private String code;
    }

    @Value
    public static class User {
        private String id;
        private String login;
        private String password;
        private String status;
    }

    public static class Cards {
        private Cards() {
        }

        private static List<CardAPI> sendRequestCardsAPI(String token) {
            var cards = Arrays.asList(given()
                    .spec(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/cards")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(CardAPI[].class));
            return cards;
        }

        public static List<CardAPI> getCardsAPI(String token) {
            return sendRequestCardsAPI(token);
        }

        @SneakyThrows
        private static String sendRequestCardsBase(String id) {
            var runner = new QueryRunner();
            var cardSQL = "SELECT number FROM cards WHERE id = ?;";

            try (var connection = DriverManager.getConnection(
                    "jdbc:mysql://185.119.57.164:3306/base", "adm", "9mRE")) {
                String numberCard = runner.query(connection, cardSQL, id, new ScalarHandler<>());
                return numberCard;
            }
        }

        public static String getNumberCards(String id) {
            return sendRequestCardsBase(id);
        }

        private static void sendRequestTransfer(String token, TransferInfo info) {
            given()
                    .spec(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .body(new TransferInfo(
                            info.getFrom(),
                            info.getTo(),
                            info.getAmount()))
                    .when()
                    .post("/api/transfer")
                    .then()
                    .statusCode(200);
        }

        public static void transferMoney(String token, String from, String to, int amount) {
            var info = new TransferInfo(from, to, amount);
            sendRequestTransfer(token, info);
        }
    }

    @Value
    public static class CardAPI {
        private String id;
        private String number;
        private int balance;
    }

    @Value
    public static class TransferInfo {
        private String from;
        private String to;
        private int amount;
    }
}
```
```Java
package ru.netology.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardTransaction {
    private String id;
    private String source;
    private String target;
    private int amount_in_kopecks;
    private String created;
}
```
## Авто-тесты находящиеся в этом репозитории.
```Java
package ru.netology;

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
        var to = faker.finance().creditCard();
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
        var from = faker.finance().creditCard();
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
        var to = faker.finance().creditCard();
        //Осуществление перевода:
        int amount = 5_000;
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
```