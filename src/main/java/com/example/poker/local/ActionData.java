import java.io.Serializable;

public class ActionData implements Serializable {
    public String action;
    public int amount;

    public ActionData(String action, int amount) {
        this.action = action;
        this.amount = amount;
    }
}