package botLogic.commandHandlers;

import botLogic.*;
import botLogic.parameterHandler.GroupHandler;
import botLogic.parameterHandler.ParameterHandler;

import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class ChangeGroupCommand extends AbstractCommand {
    Reference<String>group = new Reference<>();

    public ChangeGroupCommand(){
        setParameterHandlers(new GroupHandler(group));
    }

    public String execute(User user) throws LogicException {
        user.flushCommand();
        try {
            user.getDatabase().addUserGroup(user.getId(), group.current);
        } catch (SQLException ex) {
            //
        }
        try{
            if(!user.getDatabase().tableIsExist(group.current.toLowerCase())){
                Parser parser = new WebParser();
                List<List<String>> weeksSchedule = parser.parse(user.getDatabase()
                        .getUsersGroup(user.getId())
                        .toUpperCase());
                user
                        .getDatabase()
                        .setSchedule(user.getDatabase().getUsersGroup(user.getId()), weeksSchedule);
            }

            user.getDatabase().deleteSchedule(user.getId(), 0);
        } catch (SQLException ex) {
            throw new LogicException("Внутренняя ошибка");
        } catch (IOException e){
            throw new LogicException("Ошибка считывания расписания. Попробуйте позже");
        } catch (NoSuchElementException e){
            throw new LogicException("Не удалось найти группу с таким номером");
        }

        return "Группа успешно обновлена!";
    }
}
