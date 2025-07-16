public class Action {

    private Poker poker = new Poker();

    public Action(Poker poker) {
        this.poker = poker;
    }

    /*
        プレイヤーのアクションを処理します。
    
        player アクションを行うプレイヤー
        action アクション名（"bet", "call", "raise", "fold"）
        amount アクションに使うチップ額（callやfoldでは0）
        成功すればtrue、失敗すればfalse
     */
    public boolean processAction(Poker.Player player, String action, int amount) {
        switch (action) {
            case "check":
                poker.check(player);
                break;
            case "bet":
                poker.bet(player, (int)amount);
                break;
            case "call":
                poker.call(player);
                break;
            case "raise":
                poker.raise(player, (int)amount);
                break;
            case "fold":
                poker.fold(player);
                break;
            default:
                return false;
        }
        return true;
    }


    /*private void log(Poker.Player player, String action, int amount) {
        String entry = player.name + " " + action + (amount > 0 ? " " + amount : "");
        poker.getActionLog().add(entry); // PokerクラスにactionLogがあると仮定
        player.actionHistory.add(entry); // 各プレイヤーの履歴にも追加
        System.out.println("LOG: " + entry);
    }*/
}