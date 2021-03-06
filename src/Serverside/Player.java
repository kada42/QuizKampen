package Serverside;

import java.io.*;
import java.net.Socket;

public class Player extends Thread implements Serializable {
    private String name;
    private Socket s;
    private Game game;
    private ObjectOutputStream out;
    private PlayerStatus playerStatus;

    public Player(Socket socket, String name, Game game) {
        this.name = name;
        this.s = socket;
        this.game = game;
        String welcomeMessage = "Waiting for both players to connect";
        playerStatus = new PlayerStatus();
        playerStatus.setPlayerName(this.name);
        playerStatus.setWaiting(true);

        try {
            out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(welcomeMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserName() {
        return name;
    }


    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return name.equals(player.name);
    }

    public void sendCurrentStatus() {
        try {
            out.reset();
            out.writeObject(playerStatus);
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(s.getInputStream());) {
            game.playerIsConnected(this);
            out.writeObject(getUserName() + " is connected");
            game.giveName(this);

            Object fromClient;

            while ((fromClient = in.readObject()) != null) {
                if (playerStatus.isSelectingCategory()) {
                    if (fromClient instanceof Category) {
                        game.categorySelected((Category) fromClient);
                    } else {
                        System.err.println("Expected category but received " + fromClient);
                    }
                } else if (playerStatus.isSelectingAnswer()) {
                    if (fromClient instanceof String) {
                        try {
                            Thread.sleep(2000);
                            game.answerSelected(this, (String) fromClient);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (playerStatus.isRoundFinished() && !playerStatus.isGameFinished()) {
                    game.playNextRound();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
