package ir.salehjfz.aut.Model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import ir.salehjfz.aut.Test;
import org.telegram.telegrambots.api.objects.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@DatabaseTable(tableName = "Game")
public class Game {
    static ArrayList<Game> runningGame = new ArrayList<>();
    public static String COOPERATE = "cop";
    public static String DEFEAT = "def";

    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField
    private String player1Id;
    @DatabaseField
    private String player2Id;
    private HashMap<String,User> players;
    private int roundCount;
    private int currentRound;

    public ArrayList<HashMap<String, String>> getActions() {
        return actions;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private ArrayList<HashMap<String,String>> actions;
    private ArrayList<HashMap<String,Double>> scores;
    private boolean waiting ;
    private HashMap<String,Double> playersScores ;
    private boolean done = false;
    private HashMap<String,Message> gameMessage;



    public Game() {
        scores = new ArrayList<>();
        actions = new ArrayList<>();
        gameMessage = new HashMap<>();
        players = new HashMap<>();
        roundCount = (int) Math.ceil(new Random().nextGaussian()*1 + 5.5);
        currentRound = 1;
        waiting = true;
        playersScores = new HashMap<>();
    }


    public void start() {
        runningGame.add(this);
        waiting = false;
    }

    public static Game findRunningGameByPlayerId(String id) {
        for (Game walk: runningGame) {
            if(walk.players.containsKey(id))
                return walk;
        }
        return null;
    }

    public void finish(){
        double sum1=0,sum2=0;
        for(HashMap walk:scores){
            sum1 += (Double) walk.get(player1Id);
            sum2 += (Double) walk.get(player2Id);
        }

        setP1Score(sum1/scores.size());
        setP2Score(sum2/scores.size());

        done = true;
    }

    public static void removeRunningGmae(Game game){
        runningGame.remove(game);
    }
    public boolean setPlayerAction(User player, String playerAction) {
        HashMap action = actions.get(actions.size()-1);
        action.put(player.getChatId(),playerAction);

        if(action.keySet().size() == 2)
            return true;
        return false;
    }

    public String getPlayerLastAction(String id){
        return (actions.get(actions.size()-1).get(id).equals(COOPERATE))? Test.FULL_SUIT:Test.EMPTY_SUIT;
    }

    public boolean nextRound() {
        currentRound++;
        if(currentRound>roundCount)
            return true;
        actions.add(new HashMap<>());
        scores.add(new HashMap<>());
        return false;
    }

    public void finishTurn() {
        HashMap action = actions.get(actions.size()-1);
        HashMap score = scores.get(scores.size()-1);
        if(action.get(player1Id).equals(COOPERATE)){
            if(action.get(player2Id).equals(COOPERATE)){
                score.put(player1Id,2.0);
                score.put(player2Id,2.0);
            }else{
                score.put(player1Id,-1.0);
                score.put(player2Id,3.0);
            }
        }
        else{

            if(action.get(player2Id).equals(COOPERATE)){
                score.put(player1Id,3.0);
                score.put(player2Id,-1.0);
            }else{
                score.put(player1Id,0.0);
                score.put(player2Id,0.0);
            }
        }
    }


    public void setGameMessage(User player ,Message gameMessage) {
        this.gameMessage.put(player.getChatId(),gameMessage);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Message getGameMessage(User player) {
        return gameMessage.get(player.getChatId());
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public double getP1Score() {
        return playersScores.get(player1Id);
    }

    public void setP1Score(double p1Score) {
        playersScores.replace(player1Id,p1Score);
    }

    public Double getPlayerLastScore(String chatId) {
        return scores.get(scores.size()-1).get(chatId);
    }

    public double getP2Score() {
        return playersScores.get(player2Id);
    }

    public void setP2Score(double p2Score) {
        playersScores.replace(player2Id,p2Score);
    }

    public User getPlayer1() {
        return players.get(player1Id);
    }

    public void setPlayer1(User player1) {
        player1Id = player1.getChatId();
        this.players.put(player1.getChatId(),player1);
        this.playersScores.put(player1.getChatId(),0.0);
    }

    public User getPlayer2() {
        return players.get(player2Id);
    }

    public void setPlayer2(User player2) {
        player2Id = player2.getChatId();
        this.players.put(player2.getChatId(),player2);
        this.playersScores.put(player2.getChatId(),0.0);

    }

    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }


}
