package inex09;

public class MsnQueryResult {
	
	public MsnQueryResult(MsnQuery msnQuery) {
		this.msnQuery = msnQuery;
	}
	
	MsnQuery msnQuery;
	int rank = -1;
	
	public double mrr() {
		if (rank != -1) return 1.0 / rank;
		else return 0;
	}

	public double precisionAtK(int k){
		if (rank != -1 && rank <= k){
			return 1.0 / k;
		} else {
			return 0;
		}
	}

}