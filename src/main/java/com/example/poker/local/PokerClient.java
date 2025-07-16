import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class PokerClient {
    public static void main(String[] args) {
        try (
            Scanner scanner = new Scanner(System.in);
        ) {
            System.out.print("ポートを入力してください(5000など) → ");
            int port = scanner.nextInt();

            Socket socket = new Socket("localhost", port);
            System.out.println("接続されました");

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            System.out.print("名前を入力してください: ");
            String name = scanner.next();
            if (name.equalsIgnoreCase("exit") || name.equalsIgnoreCase("quit")) {
                socket.close();
                return;
            }

            oos.writeObject(name);
            oos.flush();

            // 手札の受信
            List<Poker.Card> hand = (List<Poker.Card>) ois.readObject();
            System.out.println("あなたの手札:");
            for (Poker.Card card : hand) {
                System.out.println(card);
            }
            
            GameRound round = new GameRound(null);
            String serverContinue = "y";
            String clientContinue = "y";

            while (serverContinue.equals("y") && clientContinue.equals("y")) {
                round.runClientRound(scanner, ois, oos, hand);

                // フラグを受信
                boolean fold = (boolean) ois.readObject();

                // 勝敗結果が通常の勝負によるものであれば受信・表示
                if (fold) {
                } else {
                    String result = (String) ois.readObject();
                    System.out.println("【結果】" + result);
                    String[] chips = (String[]) ois.readObject();
                    for(String chip: chips){
                        System.out.println(chip);
                    }
                }

                // サーバーの意思を受信
                serverContinue = (String) ois.readObject();

                // クライアントの意思確認
                System.out.print("続ける場合は y を、終わらせる場合は n を入力してください: ");
                clientContinue = scanner.next();

                // クライアントの意思をサーバーに送信
                oos.writeObject(clientContinue);
                oos.flush();
            }


            

            // 終了処理
            ois.close();
            oos.close();
            socket.close();
        } catch (Exception e) {
            System.err.println("エラーが発生したのでプログラムを終了します");
            e.printStackTrace();
        }
    }
}