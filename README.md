# Поисковый движок
### Описание:
        Spring-приложение для обхода страниц сайтов, заданных в конфигурационном файле и индексации страниц для 
    последующего обновления и поиска информации.

        Веб-интерфейс представляет собой одну веб-страницу, которая содержит в себе три вкладки: DASHBOARD, 
    MANAGEMENT и SEARCH

        DASHBOARD. Эта вкладка открывается по умолчанию. На ней отображается общая статистика по всем сайтам, 
    а также детальная статистика и статус по каждому из сайтов

        MANAGEMENT. На этой вкладке находятся инструменты управления поисковым движком — запуск и остановка
    полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке

        SEARCH. Эта страница предназначена для тестирования поискового движка. На ней находится поле поиска, 
    выпадающий список с выбором 1 сайта для поиска
    

### Системные требования: 
        Оперативная память не менее 8 Гб

### ПО для работы приложения
        OpenJDK Runtime Environment version 20.0.2 (или любая другая JRE не ниже указанной версии)
        MySQL Server 8.0 (и выше)

### Зависимости:
    org.springframework.boot:spring-boot-starter-data-jpa:jar:2.7.1
     com.mysql:mysql-connector-j:jar:8.0.33
     org.springframework.boot:spring-boot-starter-web:jar:2.7.1
     org.projectlombok:lombok:jar:1.18.24:
     org.jsoup:jsoup:jar:1.15.4
     org.apache.logging.log4j:log4j-api:jar:2.20.0
     org.apache.logging.log4j:log4j-core:jar:2.20.0
     org.apache.lucene.morphology:morph:jar:1.5
     org.apache.lucene.analysis:morphology:jar:1.5
     org.apache.lucene.morphology:dictionary-reader:jar:1.5
     org.apache.lucene.morphology:english:jar:1.5
     org.apache.lucene.morphology:russian:jar:1.5
     org.apache.commons:commons-lang3:jar:3.1

### Конфигурация
    Конфигурацию приложения осуществляет файл application.yaml

### Установка и запуск приложения
        Для запуска приложения на локальном компьютере или удаленном сервере необходимо:
    - клонировать проект с удаленного репозитория github вызовом команды 
                                        git clone https://github.com/StanislavChernoivanov/students-registration.git
    из папки, в которой будет локальный репозиторий с проектом
    - локально запустить mysql сервер и создать пустую БД с названием "search_engine"
    - запустить проект командой java -jar path/to/file/target/SearchEngine-1.0-SNAPSHOT.jar, указав полный путь к файлу
    SearchEngine-1.0-SNAPSHOT.jar (в папке target проекта)
    - для получения пользовательского интерфейса в web-браузере перейти в http://localhost:8080 (порт 8080 по умолчанию)
    
        Пользователь может изменять конфигурацию приложения, указывая при запуске другие значения переменных 
    окружения (-Dпеременная_окружения="другое значение")
        Переменные окружения:
    - URL_SITE_1, NAME_SITE_1, URL_SITE_2, NAME_SITE_2, URL_SITE_3, NAME_SITE_3 
    без указания, приложения обходит сайты, настроенные по умолчанию
    - DATASOURCE_USERNAME, DATASOURCE_PASSWORD
    необходимо указывать собственные логин и пароль подключения к mysql
    - DATASOURCE_URL
    по умолчанию - localhost:3306/search_engine?useSSL=false&requireSL=false&allowPublicKeyRetrieval=true
    - HIBERNATE_DALECT
    по умолчанию - org.hibernate.dialect.MariaDBDialect
    - SERVER_PORT
    порт инициальзации tomcat, по умолчанию - 8080

### Начало работы 
        После запуска приложения пользователь может запустить обход страниц, нажав кнопку "START INDEXING" во вкладке 
    MANAGEMENT и дождаться полной индексации страниц, или отменить индексацию, нажав "STOP INDEXING".
        После полной индексации пользователь может:
    - обновить любую из проиндексированных страниц, или добавить новую страницу, нажав "ADD/UPDATE" после 
        указания в поле ввода url страницы
    - просматривать подробную информацию о статусе сайтов, количестве сайтов, страниц, лемм и тд.
    - искать необходимую информацию по сайту, указанному в графе "All sites". Необходимо написать запрос в 
        поле ввода и нажать "SEARCH". Если сайт не указан, поиск осуществляется по всем сайтам

###  Автор
        Черноиванов Станислав Александрович, студент курса Java-разработчик Skillbox

    
     
