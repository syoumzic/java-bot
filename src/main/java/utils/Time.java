package utils;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Интерфейс для управления обработчиком времени
 */
public interface Time {

    /**
     * Возвращает текущую дату в строке
     */
    String getDateString();

    /**
     * Возвращает дату в строке
     */
    String getDateString(LocalDate date);

    /**
     * Извлекает из день, месяц, год из строки
     */
    LocalDate getAbsoluteDate(String stringDate) throws DateTimeException;

    /**
     * Извлекает из день и месяц из строки
     */
    LocalDate getLocalDate(String stringDate) throws DateTimeException;

    /**
     * Вычисляет общее время (в минутах)
     */
    int getSecondsOfDay();

    /**
     * Извлекает из пары общее время (в минутах)
     */
    int getSecondsOfDay(String lesson) throws DateTimeException;

    /**
     * Вычисляет сколько осталось до завтра (в минутах)
     */
    int getSecondsUtilTomorrow();

    /**
     * Возвращает сколько дней прошло с прошлой чётной недели первого дня относительно текущей даты
     * @return смещенная дата
     */
    int getShift();

    /**
     * Возвращает сколько дней прошло с прошлой чётной недели первого дня относительно даты
     * @param date дата
     * @return смещенная дата
     */
    int getShift(final LocalDate date);
}
