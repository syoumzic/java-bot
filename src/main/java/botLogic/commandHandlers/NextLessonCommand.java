package botLogic.commandHandlers;

import botLogic.LogicException;
import botLogic.User;
import utils.Time;

import java.sql.SQLException;
import java.util.List;

/**
 * команда /next_lesson
 */
public class NextLessonCommand extends AbstractCommand {
    private Time time;

    public NextLessonCommand(Time time){
        this.time = time;
    }

    /**
     * выдаёт ближайшую пару
     * @param user текущий пользователь
     * @return ближайшая пара
     * @throws LogicException не удалось узнать ближайшую пару
     */
    protected String execute(User user) throws LogicException, SQLException{
        try{
            user.getUsersGroup();
        }catch(SQLException e){
            throw new LogicException("Для начала укажите свою группу");
        }

        List<String>lessons = user.getSchedule(time.getShift());
        int currentSeconds = time.getSecondsOfDay();

        for(String lesson : lessons)
            if(currentSeconds < time.getSecondsOfDay(lesson))
                return lesson;

        return "Сегодня у вас больше нет пар";
    }
}
