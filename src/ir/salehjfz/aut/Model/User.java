package ir.salehjfz.aut.Model;

import java.util.ArrayList;

/**
 * Created by SalehJFZ on 05/12/2018.
 */
public class User {
    static ArrayList<User> list = new ArrayList<>();

//    private Long id;
    private String ChatId;
    private String name;

    int gamesCount;
    double points;

    public User(String chatId, String name) {

        ChatId = chatId;
        this.name = name;
        points = 0.0;
        gamesCount = 0;
        list.add(this);
    }

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }

    public String getChatId() {
        return ChatId;
    }

    public void setChatId(String chatId) {
        ChatId = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGamesCount() {
        return gamesCount;
    }

    public void setGamesCount(int gamesCount) {
        this.gamesCount = gamesCount;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public static User findById(String chatId) {
        for (User walk:list)
            if(walk.ChatId.equals(chatId))
                return walk;
        return null;

    }

}
