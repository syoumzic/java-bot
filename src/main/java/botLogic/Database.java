package botLogic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Database implements Data {
    private final String url = System.getenv("URL");
    private final String user = System.getenv("NAMEUSER");
    private final String password = System.getenv("PASSUSER");

    // JDBC variables for opening and managing connection
    private Connection connect;
    private Statement state;
    private ResultSet result;

    public List<String> getSchedule(String id, int day){
        String group = "";
        List<String> schedule = null;
        try {
            connect = DriverManager.getConnection(url, user, password);
            state = connect.createStatement();
            result = state.executeQuery("SELECT `group` FROM `users` WHERE id='12839392'");
            if (result.next()) group = result.getString(1);
            result = state.executeQuery("SELECT * FROM `" + group + "`");
            schedule = new ArrayList<String>();
            while (result.next()){
                schedule.add(result.getString(day));
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close Connection, Statement and resultSet here
            try {
                connect.close();
                state.close();
                result.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
        return schedule;
    }

    public void setSchedule(String group, List<List<String>> schedule){
        try {
            connect = DriverManager.getConnection(url, user, password);
            state = connect.createStatement();
            String query = "CREATE TABLE `" + group + "` (`id` INT NOT NULL AUTO_INCREMENT,";
            for (int i = 0; i < 14; i ++){
                query += " `" + Integer.toString(i + 1) +
                        "` VARCHAR(64) CHARACTER SET 'utf8' COLLATE 'utf8_general_ci' NOT NULL DEFAULT '-',";
            }
            query += " primary key (id))" + " ENGINE = InnoDB" + " DEFAULT CHARACTER SET = utf8;";
            state.executeUpdate(query);
            Iterator<String> iter;
            iter = schedule.get(0).iterator();
            int max_iter;
            max_iter = 0;
            int index;
            for (int i = 0; i < 14; i++){
                iter = schedule.get(i).iterator();
                index = 0;
                while (iter.hasNext() && index < max_iter){
                    index++;
                    state.executeUpdate("UPDATE `" + group + "` SET `" +
                            Integer.toString(i + 1) + "` = '" + iter.next() +
                            "' WHERE `id` = " + Integer.toString(index));
                }
                while (iter.hasNext()){
                    state.executeUpdate("INSERT INTO `" + group +
                            "` (`"+ Integer.toString(i + 1) +
                            "`) VALUES ('" + iter.next() + "')");

                }
                max_iter = Integer.max(max_iter, schedule.get(i).size());
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close Connection, Statement and resultSet here
            try {
                connect.close();
                state.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
    }

    public void addUser(String id, String group){
        try {
            connect = DriverManager.getConnection(url, user, password);

            state = connect.createStatement();

            state.executeUpdate("INSERT INTO `users` (`id`, `group`) VALUES" +
                    " ('" + id + "', '" + group + "')");

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close Connection, Statement and resultSet here
            try {
                connect.close();
                state.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
    }

    public void updateUser(String id, String group){
        try {
            connect = DriverManager.getConnection(url, user, password);

            state = connect.createStatement();

            state.executeUpdate("UPDATE `users` SET `group` = '"
                    + group + "' WHERE `id` = " + id);

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close Connection, Statement and resultSet here
            try {
                connect.close();
                state.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
    }
    public void dropTable(String name_table){
        try {
            connect = DriverManager.getConnection(url, user, password);

            state = connect.createStatement();

            state.executeUpdate("DROP TABLE `" + name_table + "`");

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close Connection, Statement and resultSet here
            try {
                connect.close();
                state.close();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        }
    }
}
