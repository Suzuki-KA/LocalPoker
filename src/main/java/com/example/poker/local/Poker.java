import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Poker {
    public List<Card> tableCards = new ArrayList<>();
    public List<Card> deck;
    private List<Player> players = new ArrayList<>();
    private int pot = 0;                  // ポット（賭け金の総額）
    private int currentMaxBet = 0;        // 現在の最大ベット額（レイズで更新される）

    


    // Cardクラス
    public static class Card implements java.io.Serializable {
        public String suit;
        public int rank;

        public Card(String suit, int rank) {
            this.suit = suit;
            this.rank = rank;
        }

        @Override
        public String toString() {
            return suit + rank;
        }
    }

    // Playerクラス
    public static class Player {
        public String name;
        public List<Card> hand = new ArrayList<>();
        public int chips = 10000;          // 所持チップ
        public int currentBet = 0;         // 現在のベット額
        public boolean isFolded = false;   // フォールドしたか

        public Player(String name) {
            this.name = name;
        }
        public List<Poker.Card> getHand(){
            return hand;
        }
    }




    // プレイヤー追加
    public void startGame(List<String> playerNames) {
        players.clear();
        for (String name : playerNames) {
            players.add(new Player(name));
        }
        deck = generateDeck();
        shuffle(deck);
        tableCards.clear();
        pot = 0;
        currentMaxBet = 0;
        dealCards();
    }

    public void resetRound() {
        clearTableCards();
        resetBets();
        pot = 0;
        for (Player player : players) {
            player.hand.clear();
            player.isFolded = false;
        }
    }



    // デッキ生成
    public List<Card> generateDeck() {
        String[] suits = { "スペード", "クラブ", "ダイア", "ハート" };
        List<Card> deck = new ArrayList<>();
        for (String suit : suits) {
            for (int rank = 2; rank <= 14; rank++) {
                deck.add(new Card(suit, rank));
            }
        }
        return deck;
    }

    // シャッフル
    public void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    // プレイヤーの手札2枚ずつ配る
    public void dealCards() {
        shuffle(deck);
        for (int i = 0; i < 2; i++) {
            for (Player player : players) {
                player.hand.add(deck.remove(deck.size() - 1));
            }
        }
    }

    // テーブルに5枚カードを出す
    public void dealTableCards() {
        tableCards.add(deck.remove(deck.size() - 1));
    }

    public void clearTableCards(){
        tableCards.clear();
    }

    public List<Card> getTableCards() {
        return tableCards;
    }

    public void setTableCards(List<Card> cards) {
        tableCards = cards;
    }

    public List<Player> getPlayers() {
        return players;
    }

    // 評価用：7枚から5枚を選ぶ全組み合わせを作る 引数は手札とテーブルのカードは入った配列
    private List<List<Card>> generate5CardCombos(List<Card> cards) {
        List<List<Card>> combinations = new ArrayList<>();               // 全通り分のカード情報がはいっているリスト
        combine(cards, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    // カードの組み合わせを全通り作る
    private void combine(List<Card> cards, int start, List<Card> current, List<List<Card>> result) {
        if (current.size() == 5) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            combine(cards, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // 手札 + テーブル 7枚で評価
    public List<Player> evaluateHands() {
        // プレイヤーごとの最高の評価を記録
        List<Map.Entry<Player, HandEvaluation>> scores = new ArrayList<>();

        for (Player player : players) {
            List<Card> combined = new ArrayList<>();
            combined.addAll(player.hand);
            combined.addAll(tableCards);

            HandEvaluation bestEval = new HandEvaluation(0, 0);

            for (List<Card> combo : generate5CardCombos(combined)) {
                HandEvaluation eval = evaluateHand(combo);

                // より強い役か、同じ役で数字が強ければ更新
                if (eval.rank > bestEval.rank || (eval.rank == bestEval.rank && eval.value > bestEval.value)) {
                    bestEval = eval;
                }
            }

            scores.add(new AbstractMap.SimpleEntry<>(player, bestEval));
        }

        // 最強の評価を見つける
        HandEvaluation maxEval = scores.stream()
            .map(Map.Entry::getValue)
            .max((a, b) -> {
                if (a.rank != b.rank) return Integer.compare(a.rank, b.rank);
                return Integer.compare(a.value, b.value);
            })
            .orElse(new HandEvaluation(0, 0));

        // 最強の評価を持つプレイヤーを抽出
        List<Player> winners = new ArrayList<>();
        for (Map.Entry<Player, HandEvaluation> entry : scores) {
            HandEvaluation eval = entry.getValue();
            if (eval.rank == maxEval.rank && eval.value == maxEval.value) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }


    // 勝敗を決めるクラス
    public class HandEvaluation {
        public int rank;    // 役のランク（例: フルハウス6, ストレート4など）
        public int value;   // 勝負に使う数（例: ストレートの一番高いカード, ペアの数字など）

        public HandEvaluation(int rank, int value) {
            this.rank = rank;
            this.value = value;
        }

        @Override
        public String toString() {
            return "役ランク: " + rank + ", 勝負値: " + value;
        }
        public int getRank() {
            return rank;
        }
        public int getValue() {
            return value;
        }
    }


    // 役の判定
    public HandEvaluation evaluateHand(List<Card> hand) {
         // マークと数字の枚数をカウント
        Map<String, List<Card>> suitMap = new HashMap<>();
        Map<Integer, Integer> rankCount = new HashMap<>();
        for(Card card: hand){
            if (!suitMap.containsKey(card.suit)) {
                suitMap.put(card.suit, new ArrayList<>());
            }
            suitMap.get(card.suit).add(card);
            if(!rankCount.containsKey(card.rank)){
                rankCount.put(card.rank, 0);
            }
            int count = rankCount.get(card.rank);
            rankCount.put(card.rank, count + 1);
        }

        // フラッシュが成立しているかを判定
        List<Card> flushCards = null;
        for (List<Card> cards: suitMap.values()) {  // Collection<V> V = List<Card>
            if (cards.size() >= 5) {
                flushCards = cards;
                break;
            }
        }

        // ストレートは成立しているかを判定
        Set<Integer> rankSet = new HashSet<>(rankCount.keySet());
        List<Integer> ranks = new ArrayList<>(rankSet);
        Collections.sort(ranks);

        boolean isStraight = false;
        List<Integer> bestStraight = new ArrayList<>();

        for (int i = 0; i <= ranks.size() - 5; i++) {
            boolean straight = true;
            for (int j = 0; j < 4; j++) {
                if (ranks.get(i + j) + 1 != ranks.get(i + j + 1)) {
                    straight = false;
                    break;
                }
            }
            if (straight) {
                isStraight = true;
                bestStraight = ranks.subList(i, i + 5); // 5枚のストレート
            }
        }

        // 枚数のカウント
        Collection<Integer> counts = rankCount.values();
        boolean hasThree = counts.contains(3);
        boolean hasFour = counts.contains(4);
        int pairCount = 0;
        int highestPair = 0;
        for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                pairCount++;
                highestPair = Math.max(highestPair, entry.getKey());
            }
        }

        // ロイヤル or ストレートフラッシュ
        if (flushCards != null && isStraight) {
            List<Integer> flushRanks = new ArrayList<>();
            for (Card c : flushCards) flushRanks.add(c.rank);
            if (flushRanks.containsAll(bestStraight)) {
                if (bestStraight.contains(14) && bestStraight.get(0) == 10) {
                    return new HandEvaluation(9, 14); // ロイヤルストレートフラッシュ
                }
                return new HandEvaluation(8, bestStraight.get(4)); // ストレートフラッシュ
            }
        }

        if (hasFour) {
            int fourRank = 0;
            for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
                if (entry.getValue() == 4) {
                    fourRank = entry.getKey();
                    break;
                }
            }
            return new HandEvaluation(7, fourRank);
        }

        if (hasThree && pairCount >= 1) {
            int threeRank = 0;
            for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
                if (entry.getValue() == 3) {
                    threeRank = entry.getKey();
                }
            }
            return new HandEvaluation(6, threeRank); // フルハウス
        }

        if (flushCards != null) {
            int highest = flushCards.stream().mapToInt(c -> c.rank).max().orElse(0);
            return new HandEvaluation(5, highest);
        }

        if (isStraight) {
            return new HandEvaluation(4, bestStraight.get(4)); // 一番高い数字で勝負
        }

        if (hasThree) {
            int threeRank = 0;
            for (Map.Entry<Integer, Integer> entry : rankCount.entrySet()) {
                if (entry.getValue() == 3) {
                    threeRank = Math.max(threeRank, entry.getKey());
                }
            }
            return new HandEvaluation(3, threeRank);
        }

        if (pairCount == 2) {
            return new HandEvaluation(2, highestPair);
        }

        if (pairCount == 1) {
            return new HandEvaluation(1, highestPair);
        }

        // ハイカード
        int highCard = hand.stream().mapToInt(c -> c.rank).max().orElse(0);
        return new HandEvaluation(0, highCard);
    }
    // マルチスレッドで最強のプレイヤーを判定する
    public List<Player> evaluateHandsParallel() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<Map.Entry<Player, HandEvaluation>>> futures = new ArrayList<>();

        for (Player player : players) {
            Future<Map.Entry<Player, HandEvaluation>> future = executor.submit(() -> {
                List<Card> combined = new ArrayList<>();
                combined.addAll(player.hand);
                combined.addAll(tableCards);

                HandEvaluation bestEval = new HandEvaluation(0, 0);
                for (List<Card> combo : generate5CardCombos(combined)) {
                    HandEvaluation eval = evaluateHand(combo);
                    if (eval.rank > bestEval.rank || (eval.rank == bestEval.rank && eval.value > bestEval.value)) {
                        bestEval = eval;
                    }
                }
                return new AbstractMap.SimpleEntry<>(player, bestEval);
            });

            futures.add(future);
        }

        List<Map.Entry<Player, HandEvaluation>> scores = new ArrayList<>();
        for (Future<Map.Entry<Player, HandEvaluation>> future : futures) {
            scores.add(future.get());
        }

        executor.shutdown();

        HandEvaluation maxEval = scores.stream()
            .map(Map.Entry::getValue)
            .max((a, b) -> {
                if (a.rank != b.rank) return Integer.compare(a.rank, b.rank);
                return Integer.compare(a.value, b.value);
            })
            .orElse(new HandEvaluation(0, 0));

        List<Player> winners = new ArrayList<>();
        for (Map.Entry<Player, HandEvaluation> entry : scores) {
            HandEvaluation eval = entry.getValue();
            if (eval.rank == maxEval.rank && eval.value == maxEval.value) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }

    // チップ
    public void bet(Player player, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("ベット額は1以上である必要があります。");
        if (amount > player.chips) {
            amount = player.chips; // オールイン処理
            System.out.println(player.name + " はオールインしました（" + amount + "）");
        }

        player.chips -= amount;
        player.currentBet += amount;
        pot += amount;
        currentMaxBet = Math.max(currentMaxBet, player.currentBet);
    }

    public void call(Player player) {
        int callAmount = currentMaxBet - player.currentBet;
        if (callAmount > player.chips) {
            callAmount = player.chips; // オールイン
        }
        bet(player, callAmount);
    }

    public void raise(Player player, int raiseAmount) {
        if (raiseAmount <= 0) throw new IllegalArgumentException("レイズ額は1以上である必要があります。");
        int totalAmount = currentMaxBet - player.currentBet + raiseAmount;
        bet(player, totalAmount);
    }
    
    public void check(Player player){

    }


    public void fold(Player player) {
        player.isFolded = true;
    }
    public void resetBets() {
        for (Player player : players) {
            player.currentBet = 0;
        }
        currentMaxBet = 0;
    }
    public void printPlayerStatus() {
        for (Player player : players) {
            System.out.println(player.name + ": チップ " + player.chips + ", 現在のベット " + player.currentBet + (player.isFolded ? " (フォールド)" : ""));
        }
        System.out.println("ポット: " + pot);
    }



}