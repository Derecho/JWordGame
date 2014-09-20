import java.io.Serializable;
import java.util.Date;


public class UnsetWord implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    long created;
    long validity;
    

    public UnsetWord() {
        created = new Date().getTime();
        //         HH * MM * SS * MSMS
        validity = 24 * 60 * 60 * 1000;
    }

    public boolean isExpired() {
        long now = new Date().getTime();
        return now > (created + validity);
    }

}

