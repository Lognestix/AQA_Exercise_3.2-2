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