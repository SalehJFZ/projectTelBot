package ir.salehjfz.aut;

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

import java.util.ArrayList;
import java.util.List;


/**
 * Created by SalehJFZ on 03/12/2018.
 */
public class Test extends TelegramLongPollingBot {
    public static final String FULL_SUIT = "چمدان پر";
    public static final String EMPTY_SUIT = "چمدان خالی";
    private static final String  NEW_GAME_STRING="بازی جدید";

    Game gameQueue1,gameQueue2;

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update);
        switch (update.getMessage().getText()){
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
    }

    private void newGame(Update update) {
        User player = User.findById(update.getMessage().getChatId().toString());

        if(gameQueue1 != null && !gameQueue1.getPlayer1().equals(player)){
            gameQueue1.setPlayer2(player);
            Message msg = sendSimpleMessage(update.getMessage().getChatId().toString(),"بازی ایجاد شد منتظر پیدا کردن حریف باشید...");
            gameQueue1.setGameMessage(player,msg);
            start(gameQueue1,update);
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

    private void gameEvent(Update update, String action) {
        User player = User.findById(update.getMessage().getChatId().toString());
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

    private void finishTurn(Update update, Game game) {
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

    private void start(Game game, Update update) {
        String firstGameText = " متنی که توی شروع بازی بنویسم";
        game.start();

        sendSimpleMessage(game.getPlayer1().getChatId(),firstGameText);
        sendSimpleMessage(game.getPlayer2().getChatId(),firstGameText);

        playNextRound(game);
    }

    private void playNextRound(Game game) {
        boolean isGameFinish = game.nextRound();
        if(isGameFinish){
            finishGame(game);
            return;
        }
        String roundText = "در این نوبت شما کدام یک را انتخاب می گنید.";
        sendRoundKeyboard(game.getPlayer1(),roundText);
        sendRoundKeyboard(game.getPlayer2(),roundText);
    }

    private void finishGame(Game game) {
        game.finish();
        String txt = " بازی تموم شد و \n "+game.getPlayer1().getName()+" "+game.getP1Score()+"امتیاز و "
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

    private void handelStart(Update update) {
        new User(update.getMessage().getChatId().toString(),update.getMessage().getFrom().getFirstName());

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


    @Override
    public String getBotUsername() {
        return "JFZ_TEST_BOT";
    }

    @Override
    public String getBotToken() {
        return "741413322:AAHCFVTIyrJ6cW2r_Mh-U_H8E4V1UlDse9o";
    }

}

