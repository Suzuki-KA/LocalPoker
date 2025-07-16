import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PokerServer {
    private static final int PORT = 5000;
    private static List<Poker.Player> connectedPlayers = new ArrayList<>();
    private static List<String> playerNames;

    public static void main(String arg[]) {
        String serverName = "Server";

        try (Scanner scanner = new Scanner(System.in)) {
            List<String> playerNames = new ArrayList<>();
            try {
                /* 通信の準備をする */
                ServerSocket server = new ServerSocket(PORT); // ポート番号を指定し、クライアントとの接続の準備を行う


                
                playerNames.add(serverName);


                Socket socket = server.accept(); // クライアントからの接続要求を待つ

                System.out.println("接続しました。相手の入力を待っています......");

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                try {
                    String clientPlayerName = (String) ois.readObject();
                    if (clientPlayerName.equals("exit") || clientPlayerName.equals("quit")) {
                        ois.close();
                        oos.close();
                        socket.close();
                        server.close();
                        return;
                    }
                    playerNames.add(clientPlayerName);
                    System.out.println("クライアント " + clientPlayerName + " が参加しました。");
                } catch (Exception e) {
                    System.err.println("通信中にエラーが発生しました");
                    e.printStackTrace();
                }

                // サーバーとクライアントの2人でゲーム開始
                System.out.println("2人揃いました。ゲームを開始します。");
                
                Poker poker = new Poker();
                poker.startGame(playerNames);
                Action actionProcessor = new Action(poker);
                

                // クライアントの手札を取得し送信
                // サーバーのプレイヤーとクライアントのプレイヤーを取得
                Poker.Player serverPlayer = poker.getPlayers().get(0);
                Poker.Player clientPlayer = poker.getPlayers().get(1);
                oos.writeObject(clientPlayer.getHand());
                oos.flush();


                GameRound round = new GameRound(poker);

                String serverContinue = "y";
                String clientContinue = "y";
                boolean fold = false;

                while (serverContinue.equals("y") && clientContinue.equals("y")) {
                    fold = round.runServerRound(scanner, ois, oos);
                    if(fold){
                        oos.writeObject(fold);
                    }

                    // サーバーの意思確認
                    System.out.println("続ける場合は y を、終わらせる場合は n を入力してください。");
                    serverContinue = scanner.next();

                    // サーバーの意思をクライアントへ送信
                    oos.writeObject(serverContinue);
                    oos.flush();

                    // クライアントの意思を受信
                    clientContinue = (String) ois.readObject();
                }
                


                poker.printPlayerStatus();

                ois.close();
                oos.close();
                socket.close();
                server.close();
            } catch (BindException be) {
                be.printStackTrace();
                System.out.println("ポート番号が不正、ポートが使用中です");
                System.err.println("別のポート番号を指定してください(6000など)");
            } catch (Exception e) {
                System.err.println("エラーが発生したのでプログラムを終了します");
                throw new RuntimeException(e);
            }
        }
    }
}