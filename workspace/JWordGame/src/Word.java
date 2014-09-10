import java.io.Serializable;
import java.util.Date;


public class Word implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String word;
	Integer mentions;
	long created;
	

	public Word(String word) {
		this.word = word;
		mentions = 0;
		created = new Date().getTime();
	}
	
	public String toString() {
		return word;
	}
	
	public Integer calcSumPoints(Integer pointsreference) {
		double days = (new Date().getTime() - created)/86400000.0;
        return (int)((((pointsreference-10)/Math.log(13)) *
                Math.log(1 + days + Math.pow(0.1 * days, 2))) + 10);
	}
	
	public Integer calcGuesserReward(Integer points) {
		return (int)((calcSumPoints(points) / (mentions / 4.5 + 1)));
	}
	
	public Integer calcSetterReward(Integer points) {
		return calcSumPoints(points) - calcGuesserReward(points);
	}

}
