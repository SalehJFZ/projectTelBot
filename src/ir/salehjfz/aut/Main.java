package ir.salehjfz.aut;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ir.salehjfz.aut.Model.Game;
import ir.salehjfz.aut.Model.User;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.SQLException;


public class Main {

    private static final java.lang.String DATABASE_URL = "jdbc:mysql://localhost/new_schema?useUnicode=true&characterEncoding=UTF-8";

    public static void main(String[] args) throws IOException {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        ConnectionSource connectionSource = null;

        try {
            connectionSource = new JdbcConnectionSource(DATABASE_URL,"root","saleh1373");
            setupDatabase(connectionSource);
            botsApi.registerBot(new Test(connectionSource));
        } catch (TelegramApiException | SQLException e) {
            e.printStackTrace();
        } finally {
            // destroy the data source which should close underlying connections
            if (connectionSource != null) {
                connectionSource.close();
            }
        }
    }

    private static void setupDatabase(ConnectionSource connectionSource) throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, User.class);
        TableUtils.createTableIfNotExists(connectionSource, Game.class);
    }
}