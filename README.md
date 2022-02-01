# SQL (AQA_Exercise_3.2-2)
## Домашнее задание по курсу "Автоматизированное тестирование"
## Тема: «3.2. SQL», задание №2: «Backend vs Frontend»
1. Создание Docker Container на базе MySQL 8:
	- прописано создание БД, пользователя, пароля
	- прописано использование приложенной схемы БД (schema.sql)
1. Запуск SUT (app-deadline.jar) через соотвествующие флаги
1. Тестирование функции перевода денег с карты на карту посредством выданного описания REST API:
	- Логин
	```gson
	POST http://localhost:9999/api/auth
	Content-Type: application/json

	{
	"login": "vasya",
	"password": "qwerty123"
	}
	```
	- Верификация
	```gson
	POST http://localhost:9999/api/auth/verification
	Content-Type: application/json

	{
	"login": "vasya",
	"code": "599640"
	}
	```
	- Просмотр карт
	```gson
	GET http://localhost:9999/api/cards
	Content-Type: application/json
	Authorization: Bearer token
	```
	Где token - это значение "token" с предыдущего шага
	- Перевод с карты на карту (любую)
	```gson
	POST http://localhost:9999/api/transfer
	Content-Type: application/json
	Authorization: Bearer token

	{
	"from": "5559 0000 0000 0002",
	"to": "5559 0000 0000 0008",
	"amount": 5000
	}
	```
### Предварительные требования
1. Получить доступ к удаленному серверу
1. На удаленном сервере должны быть установлены и доступны:
	- GIT
	- Docker	
	- Bash
1. На компьютере пользователя должна быть установлена:
	- Intellij IDEA
### Установка и запуск
1. Залогиниться на удаленный сервер
1. Склонировать проект на удаленный сервер командой
	```
	git clone https://github.com/Lognestix/AQA_Exercise_3.2-2
	```
1. Перейти в созданный каталог командой
	```
	cd AQA_Exercise_3.2-2
	```
1. Создать и запустить Docker Container на базе MySQL 8 командой
	```
	docker-compose up
	```
1. Склонировать проект на свой компьютер
	- открыть терминал
	- ввести команду 
		```
		git clone https://github.com/Lognestix/AQA_Exercise_3.2-2
		```
1. Открыть склонированный проект в Intellij IDEA
1. В Intellij IDEA перейти во вкладку Terminal (Alt+F12) и запустить SUT командой
	```
	java -jar artifacts/app-deadline.jar -P:jdbc.url=jdbc:mysql://185.119.57.164:3306/base -P:jdbc.user=adm -P:jdbc.password=9mRE
	```
1. Запустить авто-тесты (Shift+F10)