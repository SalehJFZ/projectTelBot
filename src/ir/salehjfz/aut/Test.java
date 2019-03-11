package ir.salehjfz.aut;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import ir.salehjfz.aut.Model.Game;
import ir.salehjfz.aut.Model.User;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by SalehJFZ on 03/12/2018.
 */
public class Test extends TelegramLongPollingBot {
    public static final String FULL_SUIT = "کالا را میخرم و تحویل می دهم";
    public static final String EMPTY_SUIT = "تقلب میکنم و چمدان خالی را تحویل می دهم";
    private static final String  NEW_GAME_STRING="بازی جدید";
    private static final long ADMIN_ID = 54871319;
    private Dao<User,Integer> userDao;
    private Dao<Game,Integer> gameDao;

    Game gameQueue1,gameQueue2;
    ConnectionSource connectionSource;


    public Test(ConnectionSource connectionSource) throws SQLException {
        this.connectionSource = connectionSource;
        userDao = DaoManager.createDao(connectionSource ,User.class);
        gameDao = DaoManager.createDao(connectionSource,Game.class);
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update);
        try{
            switch (update.getMessage().getText()){
                case "/getdata":
                    handleExport(update);break;
                case "/start":
                    handelStart(update);break;
                case NEW_GAME_STRING:
                    newGame(update);break;
                case EMPTY_SUIT:
                    gameEvent(update,EMPTY_SUIT);break;
                case FULL_SUIT:
                    gameEvent(update,FULL_SUIT);break;
                default:
                    System.out.println(update);
            }
        }catch (Exception e){
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private void handleExport(Update update) {
        if(amI(update.getMessage().getChatId())){
            String msg  = "";
            for(Game walk: gameDao){
                String temp = "";
                String p1 = walk.getPlayer1Id();
                String p2 = walk.getPlayer2Id();
                temp += p1 +","+p2+",";
                for (HashMap<String,String> action: walk.getActions()){
                    temp+= action.get(p1) + ";" + action.get(p2) +",";
                }
                temp = temp.substring(0,temp.length()-1);
                msg += temp + "\n";
            }
            sendSimpleMessage(ADMIN_ID+"",msg);
        }
    }

    private boolean amI(Long chatId) {
        return chatId == ADMIN_ID;
    }

    private void newGame(Update update) throws SQLException {
        User player = findUserById(update.getMessage().getChatId().toString());
        if(player == null){
            player= new User(update.getMessage().getChatId().toString(),update.getMessage().getFrom().getFirstName());
            userDao.create(player);

        }
        if(gameQueue1 != null && !gameQueue1.getPlayer1().equals(player)){
            gameQueue1.setPlayer2(player);
            Message msg = sendSimpleMessage(update.getMessage().getChatId().toString(),"بازی ایجاد شد منتظر پیدا کردن حریف باشید...");
            gameQueue1.setGameMessage(player,msg);
            start(gameQueue1,update);
            gameQueue1 = null;
        }
        else if (gameQueue1 != null && gameQueue1.getPlayer1().equals(player)){
            sendSimpleMessage(update.getMessage().getChatId().toString(),"بازی ایجاد شد منتظر پیدا کردن حریف باشید...");
        }
        else{
            Game game = new Game();
            game.setPlayer1(player);
            gameQueue1 = game;

            Message msg = sendSimpleMessage(update.getMessage().getChatId().toString(),"بازی ایجاد شد منتظر پیدا کردن حریف باشید...");
            game.setGameMessage(player,msg);
        }
        
    }


    private void gameEvent(Update update, String action) throws SQLException {
        User player = findUserById(update.getMessage().getChatId().toString());
        Game game = Game.findRunningGameByPlayerId(player.getChatId());
        boolean isTurnFinished = false;
        switch (action){
            case FULL_SUIT:
                isTurnFinished = game.setPlayerAction(player,Game.COOPERATE);break;
            case EMPTY_SUIT:
                isTurnFinished = game.setPlayerAction(player,Game.DEFEAT);break;
        }

        if(isTurnFinished){
            finishTurn(update,game);
        }
    }

    private void finishTurn(Update update, Game game) throws SQLException {
        game.finishTurn();

        String endTurnText = "در این نوبت \n"+
                game.getPlayer1().getName() + ":" + game.getPlayerLastAction(game.getPlayer1().getChatId())+"\n"+
                game.getPlayer2().getName() + ":" + game.getPlayerLastAction(game.getPlayer2().getChatId())+"\n"+
                "را بازی کردند و بدین ترتیب \n"+
                game.getPlayer1().getName() + ":" + game.getPlayerLastScore(game.getPlayer1().getChatId())+"\n"+
                game.getPlayer2().getName() + ":" + game.getPlayerLastScore(game.getPlayer2().getChatId())+"\n"+
                "گرفتند";

        sendSimpleMessage(game.getPlayer1().getChatId(),endTurnText);
        sendSimpleMessage(game.getPlayer2().getChatId(),endTurnText);

        playNextRound(game);
    }

    private void start(Game game, Update update) throws SQLException {
        String firstGameText = "\n آماده تجارت با شماست!";
        game.start();


        User player1 = findUserById(game.getPlayer1Id());
        User player2 = findUserById(game.getPlayer2Id());
        sendSimpleMessage(player1.getChatId(),player2.getName() + firstGameText);
        sendSimpleMessage(player2.getChatId(),player1.getName() + firstGameText);

        playNextRound(game);
    }

    private void playNextRound(Game game) throws SQLException {
        boolean isGameFinish = game.nextRound();
        if(isGameFinish){
            finishGame(game);
            return;
        }
        String roundText = "در این نوبت شما کدام یک را انتخاب می گنید.";
        sendRoundKeyboard(game.getPlayer1(),roundText);
        sendRoundKeyboard(game.getPlayer2(),roundText);
    }

    private void finishGame(Game game) throws SQLException {
        game.finish();
        gameDao.create(game);
        userDao.update(game.getPlayer1());
        userDao.update(game.getPlayer2());
        Game.removeRunningGmae(game);

        String txt = " بازی تموم شد و \n "+
                game.getPlayer1().getName()+" "+game.getP1Score()+"امتیاز و "
                + game.getPlayer2().getName()+" "+game.getP2Score()+"امتیاز گرفتند.";

        sendSimpleMessage(game.getPlayer1().getChatId(),txt);
        sendSimpleMessage(game.getPlayer2().getChatId(),txt);

        mainMenu(game.getPlayer1().getChatId());
        mainMenu(game.getPlayer2().getChatId());
    }

    private void sendRoundKeyboard(User player,String roundText) {
        SendMessage msg = new SendMessage();
        msg.setChatId(player.getChatId());
        msg.setReplyMarkup(getRoundKeyboard());
        msg.setText(roundText);

        try {
            sendMessage(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handelStart(Update update) throws SQLException {
        if(findUserById(update.getMessage().getChatId().toString()) != null){
            return;
        }
        User  u= new User(update.getMessage().getChatId().toString(),update.getMessage().getFrom().getFirstName());
        userDao.create(u);
        mainMenu(update.getMessage().getChatId().toString());
    }

    private void mainMenu(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getMainKeyboard());
        sendMessage.setText("خوش آمدید ، لطفا نحوه بازی را مطالعه کنید ، امیدوارم از بازی لذت ببرید!");

        try {
            sendMessage(sendMessage);
        }catch (TelegramApiException e){
            System.err.println(e);
        }
    }

    public ReplyKeyboardMarkup getMainKeyboard(){
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(NEW_GAME_STRING);
        keyboard.add(row);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup getRoundKeyboard(){
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(EMPTY_SUIT);
        row.add(FULL_SUIT);
        keyboard.add(row);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }


    private Message sendSimpleMessage(String chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setReplyMarkup(new ReplyKeyboardRemove());
        try {
            return sendMessage(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

//    private void editSimpleMessage(Message gameMessage, String chatId, String text) {
//        EditMessageText editMsg = new EditMessageText();
//        editMsg.setMessageId(gameMessage.getMessageId());
//        editMsg.setText(text);
//        editMsg.setChatId(chatId);
//
//        try {
//            editMessageText(editMsg);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//
//    }

    private User findUserById(String chatID) throws SQLException {
        Map map = new HashMap<String,String>();
        map.put("ChatID",chatID);
        List res = userDao.queryForFieldValues(map);
        if(res.size() == 0)
            return null;
        return (User) res.get(0);
    }

    @Override
    public String getBotUsername() {
        return "dadosetadgamebot";
    }

    @Override
    public String getBotToken() {
        return "748681391:AAEUsuwsyyxnIx4y92KY9Jajfap6Fn9k52Y";
    }

}

