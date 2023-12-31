package botLogic;

import JavaBots.Bot;
import dataBase.Data;
import parser.Parser;
import utils.Calendar;
import utils.Time;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Проверка обработки команд
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommandTest {
    @Mock
    Data database;

    @Mock
    Parser parser;

    @Mock
    ScheduledExecutorService scheduler;

    @Mock
    Bot bot;

    MockedStatic<Clock> clockMock;

    Time time;
    User user;
    Logic logic;

    @Captor
    ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    final LocalDate localDate = LocalDate.of(2023, 11, 27);
    final String dateString = "%s.%s".formatted(localDate.getDayOfMonth(), localDate.getMonthValue());
    final String absoluteDateString = "%s.%s.%s".formatted(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());
    final LocalTime localTime = LocalTime.of(6, 0);
    final List<String> schedule = Arrays.asList("8:00 Матан", "10:00 Алгем", "12:00 Дискретка");
    final String id = "T228";
    final String group = "МЕН-220201";

    /**
     * Инициализация переменных database, parser, user перед каждым тестом
     * @throws SQLException не бросается
     * @throws IOException не бросается
     * @throws NoSuchElementException не бросается
     */
    @BeforeEach
    public void MockitoInit() throws NoSuchElementException, IOException, SQLException{
        time = new Calendar();
        user = new User(database, id, parser, time, bot, scheduler);
        logic = new Logic(database, parser, time, scheduler);

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        Instant instant = localDateTime.atZone(zoneId).toInstant();
        Instant fixedInstant = Instant.ofEpochMilli(instant.toEpochMilli());    //в миллисекундах
        Clock fixedClock = Clock.fixed(fixedInstant, zoneId);

        clockMock = Mockito.mockStatic(Clock.class);
        clockMock.when(Clock::systemDefaultZone).thenReturn(fixedClock);
        Assertions.assertEquals(localTime, LocalTime.now());

        Mockito.when(database.getUsersGroup(id)).thenReturn(group);
        Mockito.when(database.getNotificationShift(id)).thenReturn(10);                      //10 минут
        Mockito.when(database.getSchedule(id, time.getShift())).thenReturn(schedule);
        Mockito.when(database.tableIsExist(group)).thenReturn(true);
        Mockito.when(database.getUserIdNotification()).thenReturn(List.of(id));

        Mockito.when(parser.parse(time, group)).thenReturn(List.of(schedule));
    }

    /**
     * Удаление mock объектов перед стартов нового теста
     */
    @AfterEach
    public void MockitoAfter(){
        clockMock.close();
    }

    /**
     * Проверка команды /schedule
     */
    @Test
    public void getScheduleVerifyTest(){
            user.processMessage("/schedule");
            String answer = user.processMessage(dateString);

            String stringSchedule = String.join("\n", schedule) + "\n";
            Assertions.assertEquals(stringSchedule, answer);
    }

    /**
     * Проверка команды /schedule с ещё не введённым номером группы
     */
    @Test
    public void getScheduleWithNoGroupTest() throws SQLException{
        Mockito.when(database.getUsersGroup(id)).thenThrow(SQLException.class);

        user.processMessage("/schedule");
        String answer = user.processMessage(dateString);

        Assertions.assertEquals("Для начала укажите свою группу", answer);
    }

    /**
     * Проверка команды /change_group на запись в базу данных
     * @throws SQLException не бросается
     */
    @Test
    public void changeGroupVerifyTest() throws SQLException{
        user.processMessage("/change_group");
        String answer = user.processMessage(group);

        Mockito.verify(database).addUserGroup(id, group);
        Assertions.assertEquals("Группа успешно обновлена!", answer);
    }

    /**
     * Проверка команды /change_group с некорректной группой
     * @throws SQLException не вызывается
     */
    @Test
    public void changeGroupWithIncorrectArgumentTest() throws SQLException{
        String incorrectGroup = "-220201";
        user.processMessage("/change_group");
        String answer = user.processMessage(incorrectGroup);

        Mockito.verify(database, Mockito.never()).addUserGroup(id, incorrectGroup);
        Assertions.assertEquals("Группа введена некорректно", answer);
    }

    /**
     * Проверка команды /change_group с группой, у которой нечитабельное расписание
     * @throws IOException обрабатывается
     * @throws SQLException не бросается
     */
    @Test
    public void changeGroupUnreadScheduleTest() throws NoSuchElementException, IOException, SQLException{
        Mockito.when(database.getSchedule(id, 0)).thenThrow(SQLException.class);
        Mockito.when(parser.parse(time, group)).thenThrow(IOException.class);

        user.processMessage("/change_group");
        String answer = user.processMessage(group);

        Mockito.verify(parser).parse(time, group);
        Mockito.verify(database, Mockito.never()).setSchedule(Mockito.anyString(), Mockito.any());
        Assertions.assertEquals("Не удалось прочесть расписание", answer);
    }

    /**
     * Проверка команды /change_group с группы, которой нет на сайте UrFU
     * @throws IOException не бросается
     * @throws SQLException не бросается
     */
    @Test
    public void changeGroupNoSuchGroupTest() throws NoSuchElementException, IOException, SQLException{
        Mockito.when(database.getSchedule(id, 0)).thenThrow(SQLException.class);
        Mockito.when(parser.parse(time, group)).thenThrow(NoSuchElementException.class);

        user.processMessage("/change_group");
        String answer = user.processMessage(group);

        Assertions.assertEquals("Не удалось найти группу с таким номером", answer);
        Mockito.verify(parser).parse(time, group);
        Mockito.verify(database, Mockito.never()).setSchedule(Mockito.anyString(), Mockito.any());
    }

    /**
     * Проверка команды /change_schedule
     * @throws SQLException не бросается
     */
    @Test
    public void changeScheduleVerifyTest() throws SQLException{
        List<String> customSchedule = List.of("8:00 Матан", "10:00 Алгем");
        String customScheduelString = String.join("\n", customSchedule);

        String answer;

        answer = user.processMessage("/change_schedule");
        answer = user.processMessage(dateString);
        answer = user.processMessage(customScheduelString);

        Assertions.assertEquals("Расписание обновлено", answer);
        Mockito.verify(database).setCastomSchedule(id, customSchedule, time.getShift());
    }

    /**
     * Проверка команды /next_lesson
     * @throws SQLException не бросается
     */
    @Test
    public void nextLessonVerify() throws SQLException{
        String answer = user.processMessage("/next_lesson");

        Assertions.assertEquals(schedule.get(0), answer);
        Mockito.verify(database).getSchedule(id, time.getShift());
    }

    /**
     * Проверка корректности включения уведомлений
     */
    @Test
    public void setNotificationVerifyTest() throws SQLException{
        Mockito.when(database.getStatusNotifications(id)).thenReturn(1);
        String answer = user.processMessage("/notification_on");

        Assertions.assertEquals("Уведомления включены", answer);
        Mockito.verify(scheduler, Mockito.times(schedule.size())).schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS));
        long currentTime = time.getSecondsOfDay();
        long shiftTime = database.getNotificationShift(id) * 60;

        for(String lesson : schedule) {
            long lessonTime = time.getSecondsOfDay(lesson);
            Mockito.verify(scheduler).schedule(runnableCaptor.capture(), Mockito.eq(lessonTime - currentTime - shiftTime), Mockito.eq(TimeUnit.SECONDS));
            runnableCaptor.getValue().run();
            Mockito.verify(bot).sendMessage(Long.parseLong(id.substring(1)), lesson);
        }
    }

    /**
     * Проверка установки времени уведомлений
     * @throws SQLException не обрабатывается
     */
    @Test
    public void settingsNotificationVerifyTest() throws SQLException{
        int timeShift = 50;

        user.processMessage("/notification_set");
        String answer = user.processMessage(Integer.toString(timeShift));

        Assertions.assertEquals("Время установлено", answer);
        Mockito.verify(database).setNotificationShift(id, timeShift);
    }

    /**
     * Проверка установки времени уведомлений с некорректным количеством минут
     * @throws SQLException не обрабатывается
     */
    @Test
    public void settingsNotificationIncorrectTest() throws SQLException{
        user.processMessage("/notification_set");
        String answer = user.processMessage("dkfjo");

        Assertions.assertEquals("Введено не число", answer);
        Mockito.verify(database, Mockito.never()).setNotificationShift(Mockito.eq(id), Mockito.anyInt());
    }

    /**
     * Проверка установки времени уведомлений с выходом за границы установленного времени
     * @throws SQLException не обрабатывается
     */
    @Test
    public void settingsNotificationOutOfRangeTest() throws SQLException{
        user.processMessage("/notification_set");
        String answer = user.processMessage("1021");

        Assertions.assertEquals("Ожидается число от 0 до 90", answer);
        Mockito.verify(database, Mockito.never()).setNotificationShift(Mockito.eq(id), Mockito.anyInt());
    }

    /**
     * Проверка установки дедлайнов
     */
    @Test
    public void setDeadlinesTest() throws SQLException{
        List<String>deadlines = List.of("12:00 Записаться на курсы по английскому", "18:00 Сдать задачу по дискретной математике");
        String stringDeadlines = String.join("\n", deadlines) + "\n";

        String answer;

        user.processMessage("/add_deadlines");
        user.processMessage(dateString);
        answer = user.processMessage(stringDeadlines);

        Assertions.assertEquals("Дедлайны успешно установлены", answer);
        Mockito.verify(database).addDeadlines(id, deadlines, absoluteDateString);
    }

    /**
     * Проверка не установки дедлайнов при неправильных параметрах
     */
    @Test
    public void setDeadlinesIncorrectInputTest() throws SQLException{
        List<String>deadlines = List.of("10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому");
        String stringDeadlines = String.join("\n", deadlines) + "\n";

        String answer;

        user.processMessage("/add_deadlines");
        user.processMessage(dateString);
        answer = user.processMessage(stringDeadlines);

        Assertions.assertEquals("для строки '%s' не указано время".formatted(deadlines.get(0)), answer);
        Mockito.verify(database, Mockito.never()).addDeadlines(id, deadlines, absoluteDateString);
    }

    /**
     * Проверка складывания дедлайнов командой add_deadline
     */
    @Test
    public void addDeadlineTest() throws SQLException{
        List<String>addedDeadlines = new ArrayList<>(List.of("20:00 Сделать тест по истории"));
        String stringAddedDeadlines = String.join("\n", addedDeadlines) + "\n";

        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(addedDeadlines);

        String answer;

        user.processMessage("/add_deadlines");
        user.processMessage(dateString);
        answer = user.processMessage(stringAddedDeadlines);

        Assertions.assertEquals("Дедлайны успешно установлены", answer);

        Mockito.verify(database).addDeadlines(id, addedDeadlines, absoluteDateString);
    }

    /**
     * Проверка редактирования дедлайнов
     */
    @Test
    public void editDeadlineTest() throws SQLException{
        List<String>deadlines = new ArrayList<>(List.of("19:10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому"));
        String stringDeadlines = String.join("\n", deadlines) + "\n";

        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(new ArrayList<>());

        user.processMessage("/edit_deadlines");
        user.processMessage(dateString);
        String answer = user.processMessage(stringDeadlines);

        Assertions.assertEquals("Дедлайны успешно обновлены", answer);
        Mockito.verify(database).editDeadlines(id, deadlines, absoluteDateString);
    }

    /**
     * Проверка редактирования дедлайнов с неправильными данными
     */
    @Test
    public void editDeadlinesIncorrectInputTest() throws SQLException{
        List<String>deadlines = List.of("10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому");
        String stringDeadlines = String.join("\n", deadlines) + "\n";

        String answer;

        user.processMessage("/edit_deadlines");
        user.processMessage(dateString);
        answer = user.processMessage(stringDeadlines);

        Assertions.assertEquals("для строки '%s' не указано время".formatted(deadlines.get(0)), answer);
        Mockito.verify(database, Mockito.never()).addDeadlines(id, deadlines, absoluteDateString);
    }

    /**
     * Проверка редактирования дедлайнов в первый раз
     */
    @Test
    public void editDeadlineFirstTimeTest() throws SQLException{
        List<String>deadlines = new ArrayList<>(List.of("19:10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому"));
        String stringDeadlines = String.join("\n", deadlines) + "\n";

        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenThrow(SQLException.class);

        user.processMessage("/edit_deadlines");
        user.processMessage(dateString);
        String answer = user.processMessage(stringDeadlines);

        Assertions.assertEquals("На этот день нет дедлайнов", answer);
        Mockito.verify(database, Mockito.never()).editDeadlines(id, deadlines, absoluteDateString);
    }

    /**
     * Проверка команды /next_deadline
     */
    @Test
    public void getNextDeadlineTest() throws SQLException{
        List<String>deadlines;
        String answer;

        deadlines = List.of("19:10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому");
        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(deadlines);
        answer = user.processMessage("/next_deadlines");
        Assertions.assertEquals(deadlines.get(0), answer);

        deadlines = List.of("1:10 Записаться на курсы по английскому", "8:20 Записаться на курсы по английскому");
        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(deadlines);
        answer = user.processMessage("/next_deadlines");
        Assertions.assertEquals(deadlines.get(1), answer);

        deadlines = List.of("1:10 Записаться на курсы по английскому", "4:20 Записаться на курсы по английскому");
        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(deadlines);
        answer = user.processMessage("/next_deadlines");
        Assertions.assertEquals("Дедлайнов на сегодня нет", answer);
    }

    /**
     * Проверка показа дедлайнов на определённую дату
     */
    @Test
    public void getDeadlinesTest() throws SQLException{
        List<String>deadlines = List.of("19:10 Записаться на курсы по английскому", "19:20 Записаться на курсы по английскому");
        String stringDeadlines = String.join("\n", deadlines) + "\n";
        Mockito.when(database.getDeadlines(id, absoluteDateString)).thenReturn(deadlines);

        user.processMessage("/deadlines");
        String answer = user.processMessage(dateString);

        Assertions.assertEquals(stringDeadlines, answer);
        Mockito.verify(database).getDeadlines(id, absoluteDateString);
    }
}
