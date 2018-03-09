package wiki13.maple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import query.ExperimentQuery;
import query.QueryResult;

public class WikiMapleExperimentHelper {

	private static final Logger LOGGER = Logger.getLogger(WikiMapleExperimentHelper.class.getName());

	protected static void writeResultsListToFile(List<List<QueryResult>> resultsList, String resultDirectoryPath) {
		File resultsDir = new File(resultDirectoryPath);
		if (!resultsDir.exists())
			resultsDir.mkdirs();
		try (FileWriter p20Writer = new FileWriter(resultDirectoryPath + "wiki_p20.csv");
				FileWriter mrrWriter = new FileWriter(resultDirectoryPath + "wiki_mrr.csv");
				FileWriter rec200Writer = new FileWriter(resultDirectoryPath + "wiki_recall200.csv");
				FileWriter recallWriter = new FileWriter(resultDirectoryPath + "wiki_recall.csv")) {
			for (int i = 0; i < resultsList.get(0).size(); i++) {
				ExperimentQuery query = resultsList.get(0).get(i).query;
				p20Writer.write(query.getText());
				mrrWriter.write(query.getText());
				rec200Writer.write(query.getText());
				recallWriter.write(query.getText());
				for (int j = 0; j < resultsList.size(); j++) {
					QueryResult result = resultsList.get(j).get(i);
					p20Writer.write("," + result.precisionAtK(20));
					mrrWriter.write("," + result.mrr());
					rec200Writer.write("," + result.recallAtK(200));
					recallWriter.write("," + result.recallAtK(1000));
				}
				p20Writer.write("\n");
				mrrWriter.write("\n");
				rec200Writer.write("\n");
				recallWriter.write("\n");
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}