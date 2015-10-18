import java.util.ArrayList;

public class TestRecord {
private int recordId;
private String fullRecord;

private ArrayList<String> words;
public int getRecordId() {
	return recordId;
}
public ArrayList<String> getWords() {
	return words;
}
public void setWords(ArrayList<String> words) {
	this.words = words;
}
public void setRecordId(int recordId) {
	this.recordId = recordId;
}
public String getFullRecord() {
	return fullRecord;
}
public void setFullRecord(String fullRecord) {
	this.fullRecord = fullRecord;
}
}
