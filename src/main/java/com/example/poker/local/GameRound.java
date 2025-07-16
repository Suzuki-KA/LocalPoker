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

        System.out.println("");
        // 最初のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }

        System.out.println("");
        // フロップ
        serverFlop(oos);
        // 2回目のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }

        System.out.println("");
        // ターン
        serverDraw(oos, "ターン");

        System.out.println("");
        // 3回目のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }

        System.out.println("");
        // リバー
        serverDraw(oos, "リバー");

        System.out.println("");
        // 最後のcall,bet
        fold = runServerAction(scanner, ois, oos);
        if(fold){
            return true;
        }

        System.out.println("");
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
            List<Poker.Card> clientHand = clientPlayer.getHand();
            for(Poker.Card card: clientHand){
                oos.writeObject(card);
            }
            oos.writeObject(new ActionData(serverAction, serverAmount));
            oos.flush();

            
            if (serverAction.equals("fold")){// フォールドなら終了
                rewardRemainingPlayer(clientPlayer, oos);
                return true;
                }

            // クライアントのアクション受信
            ActionData clientResponse = (ActionData) ois.readObject();
            actionHandler.processAction(clientPlayer, clientResponse.action, clientResponse.amount);
            System.out.println("クライアントが " + clientResponse.action + " しました");

            
            if (clientResponse.action.equals("fold")) {
                rewardRemainingPlayer(serverPlayer, oos);
                return true;
            }

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
            String[] pot = poker.printPlayerStatus();
            for(String a: pot){
                System.out.println(a);
            }
            oos.writeObject(pot);
            oos.flush();
        }

        return false; // fold されなかった
    }



    // クライアント側でラウンドを進行するメソッド
    public boolean runClientRound(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos, List<Poker.Card> hand) throws ClassNotFoundException, IOException{
        boolean fold = false;

        System.out.println("");
        // 最初のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }

        System.out.println("");
        // フロップ
        clientFlop(ois);

        System.out.println("");
        // 2回目のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }

        System.out.println("");
        // ターン
        clientDraw(ois, "ターン");

        System.out.println("");
        // 3回目のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }

        System.out.println("");
        // リバー
        clientRiver(ois, "リバー");

        System.out.println("");
        // 最後のbet,call
        fold = runClientAction(scanner, ois, oos, hand);
        if(fold){
            return true;
        }
        return false;
    }
    public boolean runClientAction(Scanner scanner, ObjectInputStream ois, ObjectOutputStream oos, List<Poker.Card> hand) throws IOException, ClassNotFoundException {
        List<Poker.Card> clientHand = new ArrayList<Poker.Card>();
        clientHand.add((Poker.Card)ois.readObject());
        clientHand.add((Poker.Card)ois.readObject());
        ActionData serverAction = (ActionData) ois.readObject();
        System.out.println("サーバーが " + serverAction.action + (serverAction.amount > 0 ? "（" + serverAction.amount + "）" : "") + " しました");

        if (serverAction.action.equals("fold")){
            String message = (String) ois.readObject();
            System.out.println(message);
            String[] pot = (String[]) ois.readObject();
            for (String a : pot) {
                System.out.println(a);
            }
            return true;
        }

        System.out.println("あなたの手札:");
        for (Poker.Card card : clientHand) {
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

        if (clientAction.equals("fold")) {
            String message = (String)ois.readObject();
            System.out.println(message);
            String[] pot = (String[]) ois.readObject();
            for(String a: pot){
                System.out.println(a);
            }
            return true;
        }

        // クライアントが bet または raise した場合は、サーバーの返答を受け取る
        if (clientAction.equals("bet") || clientAction.equals("raise")) {
            ActionData serverResponse = (ActionData) ois.readObject();
            System.out.println("サーバーが " + serverResponse.action + (serverResponse.amount > 0 ? "（" + serverResponse.amount + "）" : "") + " しました");
            if (serverResponse.action.equals("fold")) return true;
        }

        String[] pot = (String[]) ois.readObject();
        for(String a: pot){
            System.out.println(a);
        }
        return false;
    }


    public void serverFlop(ObjectOutputStream oos) throws IOException{
        for(int i = 0; i < 3; i++){
            poker.dealTableCards();
        }
        List<Poker.Card> flop = poker.getTableCards();
        System.out.println("フロップ:");
        for (Poker.Card card : flop) {
            System.out.println(card);
            oos.writeObject(card);
        }
    }
    public void clientFlop(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        // フロップを受信して表示
        List<Poker.Card> flop = new ArrayList<>();
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
        flop.add((Poker.Card) ois.readObject());
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

    // ポット分配処理
    int totalPot = poker.getPot();
    int share = totalPot / winners.size();
    int remainder = totalPot % winners.size();

    for (int i = 0; i < winners.size(); i++) {
        Poker.Player winner = winners.get(i);
        winner.chips += share;
        if (i == 0) {
            winner.chips += remainder; // 端数は最初の勝者に加算（またはランダムでもOK）
        }
    }

    // ポットをリセット（ゲーム継続前提）
    poker.resetPot();

    // 勝者の名前を文字列で送信
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

        // 勝敗メッセージ送信
        oos.writeObject(false);  // ← foldではない通知
        oos.writeObject(sb.toString());
        oos.flush();

        // 勝敗結果を表示
        System.out.println("[サーバー] " + sb.toString());

        // チップの最終状態表示
        System.out.println("[サーバー] ラウンド終了後のチップ残高:");
        String[] playerChips = new String[poker.getPlayers().size()];
        int index = 0;
        for (Poker.Player player : poker.getPlayers()) {
            System.out.println(player.name + ": " + player.chips + " チップ");
            playerChips[index] = player.name + ": " + player.chips + " チップ";

            poker.resetRound();
            index++;
        }
        oos.writeObject(playerChips);
        poker.clearTableCards();
    }

    private void rewardRemainingPlayer(Poker.Player winner, ObjectOutputStream oos) throws IOException {
        int pot = poker.getPot();
        winner.chips += pot;
        poker.resetPot();

        String message = "勝者: " + winner.name + "（フォールドによる勝利）";
        oos.writeObject(message);
        oos.writeObject(poker.printPlayerStatus()); // チップ表示
        oos.flush();
        poker.resetRound();
        System.out.println("[サーバー] " + message);
    }
}
