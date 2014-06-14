package edu.uiowa.cs.itpwrapping;

/**
 *
 * @author bnjmnbrmn and Harley Eades
 */
public class ITPOutputEvent {
    private String payload;
    private int state;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
    
    public ITPOutputEvent() {}

    public ITPOutputEvent(String payload, int state) {
        this.payload = payload;
        this.state = state;
    }
    
    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    
}
