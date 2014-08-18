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
	
	public Integer calcSumPoints(Integer points) {
		// A slightly advanced formula which causes maxpoints set by the Game object to increase when time passes by.
		long days = (new Date().getTime() - created)/86400000;
		return (int)(points * (1 + 0.5 * days) * Math.pow(1.15, days));
	}
	
	public Integer calcGuesserReward(Integer points) {
		return (int)((calcSumPoints(points) / (mentions / 3.5 + 1)));
	}
	
	public Integer calcSetterReward(Integer points) {
		return calcSumPoints(points) - calcGuesserReward(points);
	}

}
