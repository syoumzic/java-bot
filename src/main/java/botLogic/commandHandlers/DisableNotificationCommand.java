package botLogic.commandHandlers;

import botLogic.User;

import java.sql.SQLException;

/**
 * /notification_off
 */
public class DisableNotificationCommand extends AbstractCommand{
    /**
     * Выключить уведомления для конкретного пользователя
     * @param user текущий пользователь
     * @return сообщение об успешной применении команды
     */
    protected String execute(User user) throws SQLException {
        user.setStatusNotifications(0);
        user.disableNotifications();
        return "Уведомления успешно удалены";
    }
}
