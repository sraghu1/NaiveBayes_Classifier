import java.util.HashMap;


public class OccuranceProbabilties {
	private String className;
	private HashMap<String,Double> occuranceMap;
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public HashMap<String, Double> getOccuranceMap() {
		return occuranceMap;
	}
	public void setOccuranceMap(HashMap<String, Double> occuranceMap) {
		this.occuranceMap = occuranceMap;
	}
}
