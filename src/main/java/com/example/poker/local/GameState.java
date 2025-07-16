
import java.util.ArrayList;
import java.util.List;
import java.util.*;

class GameState {
    private final List<String> communityCards;
    private final List<String> playerCards;
    private final Set<Integer> selectedCardIndexes;
    private int pot;
    private String lastAction;
    private String selectedAction;

    public GameState() {
        communityCards = new ArrayList<>();
        playerCards = new ArrayList<>();
        selectedCardIndexes = new HashSet<>();
        pot = 150;
        lastAction = "None";
        selectedAction = "";

        communityCards.add("A♠");
        communityCards.add("10♦");
        communityCards.add("J♣");
        communityCards.add("Q♥");
        communityCards.add("K♠");

        playerCards.add("7♠");
        playerCards.add("7♦");
    }

    public List<String> getCommunityCards() {
        return communityCards;
    }

    public List<String> getPlayerCards() {
        return playerCards;
    }

    public int getPot() {
        return pot;
    }

    public Set<Integer> getSelectedCardIndexes() {
        return selectedCardIndexes;
    }

    public void toggleCardSelection(int index) {
        if (selectedCardIndexes.contains(index)) {
            selectedCardIndexes.remove(index);
        } else {
            selectedCardIndexes.add(index);
        }
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String action) {
        this.lastAction = action;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(String selectedAction) {
        this.selectedAction = selectedAction;
    }
}