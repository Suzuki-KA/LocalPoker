import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class GameRound {
    private final Poker poker;
    private final Action actionHandler;

    public GameRound(Poker poker) {
        this.poker = poker;
        this.actionHandler = new Action(poker);
    }

    
    // サーバー側でラウンドを進行するメソッド
    public boolean runServerRound(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        boolean fold = false;
        // 最初のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }
        // フロップ
        serverFlop(oos);
        // 2回目のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }
        // ターン
        serverDraw(oos, "ターン");
        // 3回目のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }
        // リバー
        serverDraw(oos, "リバー");
        // 最後のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }
        // 勝敗
        judge(oos);
        return false;
    }
    public boolean runServerAction(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException {
        Poker.Player serverPlayer = poker.getPlayers().get(0);
        Poker.Player clientPlayer = poker.getPlayers().get(1);

        boolean actionFinished = false;

        while (!actionFinished) {
            // サーバーのアクション
            System.out.println("あなたの手札:");
            for (Poker.Card card : serverPlayer.getHand()) {
                System.out.println(card);
            }
            System.out.print(serverPlayer.name + "（サーバー） アクションを選んでください（bet / check / fold）: ");
            String serverAction = scanner.next();
            int serverAmount = 0;
            if (serverAction.equals("bet")) {
                System.out.print("ベット額を入力してください: ");
                serverAmount = scanner.nextInt();
            }

            actionHandler.processAction(serverPlayer, serverAction, serverAmount);
            oos.writeObject(new ActionData(serverAction, serverAmount));
            oos.flush();

            
            if (serverAction.equals("fold")) return true;  // フォールドなら終了

            // クライアントのアクション受信
            ActionData clientResponse = (ActionData) ois.readObject();
            actionHandler.processAction(clientPlayer, clientResponse.action, clientResponse.amount);
            System.out.println("クライアントが " + clientResponse.action + " しました");

            
            if (clientResponse.action.equals("fold")) return true;

            if (serverAction.equals("check") && clientResponse.action.equals("check")) {
                actionFinished = true;
            }

            if (clientResponse.action.equals("raise")) {
                // サーバーが応答する
                System.out.print("クライアントがレイズしました。サーバー側の応答（call / raise / fold）: ");
                String responseAction = scanner.next();
                int responseAmount = 0;
                if (responseAction.equals("raise")) {
                    System.out.print("追加レイズ額を入力: ");
                    responseAmount = scanner.nextInt();
                }

                actionHandler.processAction(serverPlayer, responseAction, responseAmount);
                oos.writeObject(new ActionData(responseAction, responseAmount));
                oos.flush();

                if (responseAction.equals("fold")) return true;

                if (responseAction.equals("call") || responseAction.equals("fold")) {
                    actionFinished = true;
                }
            } else {
                actionFinished = true;
            }
        }

        return false; // fold されなかった
    }



    // クライアント側でラウンドを進行するメソッド
    public boolean runClientRound(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos, List<Poker.Card> hand) throws ClassNotFoundException, IOException{
        boolean fold = false;
        // 最初のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }
        // フロップ
        clientFlop(ois);
        // 2回目のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }
        // ターン
        clientDraw(ois, "ターン");
        // 3回目のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }
        // リバー
        clientRiver(ois, "リバー");
        // 最後のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }
        return false;
    }
    public boolean runClientAction(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos, List<Poker.Card> hand) throws IOException, ClassNotFoundException {
        ActionData serverAction = (ActionData) ois.readObject();
        System.out.println("サーバーが " + serverAction.action + (serverAction.amount > 0 ? "（" + serverAction.amount + "）" : "") + " しました");

        if (serverAction.action.equals("fold")) return true;

        System.out.println("あなたの手札:");
        for (Poker.Card card : hand) {
            System.out.println(card);
        }

        String clientAction = "";
        int amount = 0;

        if (serverAction.action.equals("check")) {
            // クライアントは check / bet / fold ができる
            while (true) {
                System.out.print("アクションを選んでください（check / bet / fold）: ");
                clientAction = scanner.next();
                if (clientAction.equals("check") || clientAction.equals("bet") || clientAction.equals("fold")) break;
                System.out.println("無効なアクションです。");
            }

            if (clientAction.equals("bet")) {
                System.out.print("ベット額を入力: ");
                amount = scanner.nextInt();
            }

        } else {
            // サーバーが bet または raise をしてきた場合、call / raise / fold ができる
            while (true) {
                System.out.print("アクションを選んでください（call / raise / fold）: ");
                clientAction = scanner.next();
                if (clientAction.equals("call") || clientAction.equals("raise") || clientAction.equals("fold")) break;
                System.out.println("無効なアクションです。");
            }

            if (clientAction.equals("raise")) {
                System.out.print("レイズ額を入力: ");
                amount = scanner.nextInt();
            }
        }

        oos.writeObject(new ActionData(clientAction, amount));
        oos.flush();

        if (clientAction.equals("fold")) return true;

        // クライアントが bet または raise した場合は、サーバーの返答を受け取る
        if (clientAction.equals("bet") || clientAction.equals("raise")) {
            ActionData serverResponse = (ActionData) ois.readObject();
            System.out.println("サーバーが " + serverResponse.action + (serverResponse.amount > 0 ? "（" + serverResponse.amount + "）" : "") + " しました");
            if (serverResponse.action.equals("fold")) return true;
        }

        return false;
    }


    public void serverFlop(ObjectOutputStream oos) throws IOException{
        for(int i = 0; i < 3; i++){
            poker.dealTableCards();
        }
        List<Poker.Card> flop = poker.getTableCards();
        oos.writeObject(flop);
        System.out.println("フロップ:");
        for (Poker.Card card : flop) {
            System.out.println(card);
        }
    }
    public void clientFlop(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        // フロップを受信して表示
        List<Poker.Card> flop = (List<Poker.Card>) ois.readObject();
        System.out.println("フロップ:");
        for (Poker.Card card : flop) {
            System.out.println(card);
        }
    }

    public void serverDraw(ObjectOutputStream oos, String action) throws IOException{
        poker.dealTableCards();
        List<Poker.Card> flop = poker.getTableCards();
        System.out.println(action+":");
        for (Poker.Card card : flop) {
            System.out.println(card);
            oos.writeObject(card);
        }
    }

    public void clientDraw(ObjectInputStream ois, String action) throws ClassNotFoundException, IOException{
        // フロップを受信して表示
        List<Poker.Card> flop = new ArrayList<>();
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        System.out.println(action+":");
        for (Poker.Card card : flop) {
            System.out.println(card);
        }
    }
    public void clientRiver(ObjectInputStream ois, String action) throws ClassNotFoundException, IOException{
        // フロップを受信して表示
        List<Poker.Card> flop = new ArrayList<>();
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        System.out.println(action+":");
        for (Poker.Card card : flop) {
            System.out.println(card);
        }
    }

    public void judge(ObjectOutputStream oos) throws IOException, InterruptedException, ExecutionException {
        List<Poker.Player> winners = poker.evaluateHandsParallel(); // 勝者リスト（複数可）

        // 勝者の名前を文字列で送信（複数名の場合は連結）
        StringBuilder sb = new StringBuilder();
        if (winners.size() == 1) {
            sb.append("勝者: ").append(winners.get(0).name);
        } else {
            sb.append("引き分け: ");
            for (int i = 0; i < winners.size(); i++) {
                sb.append(winners.get(i).name);
                if (i < winners.size() - 1) sb.append(", ");
            }
        }
        // foldではないことを通知
        oos.writeObject(false);  // ← boolean 型
        oos.writeObject(sb.toString()); // ← 勝敗のメッセージ
        oos.flush(); // ← 忘れず flush！
        System.out.println("[サーバー] " + sb.toString());
    }

}
