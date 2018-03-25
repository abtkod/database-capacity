package stackoverflow;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import database.DatabaseConnection;
import database.DatabaseType;

public class StackExperiment {

	private static final Logger LOGGER = Logger.getLogger(StackExperiment.class.getName());

	public static void main(String[] args) throws IOException, SQLException, ParseException {
		StackExperiment se = new StackExperiment();
		List<QuestionDAO> questions = se.loadQueries();
		try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("/data/ghadakcv/stack_index")))) {
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser parser = new QueryParser(StackIndexer.BODY_FIELD, analyzer);
			parser.setDefaultOperator(Operator.OR);
			for (QuestionDAO question : questions) {
				String queryText = question.text.replaceAll("\\(\\)", "");
				System.out.println(queryText);
				System.out.println("rel: " + question.answer);
				Query query = parser.parse(queryText);
				ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;
				for (int i = 0; i < hits.length; i++) {
					Document doc = searcher.doc(hits[i].doc);
					System.out.println(doc.get("Id"));
					if (doc.get(StackIndexer.ID_FIELD).equals(question.answer)) {
						question.resultRank = i;
						break;
					}
				}
			}
		}
		for (QuestionDAO question : questions) {
			System.out.println(question.text + ": " + question.resultRank);
		}
	}

	private List<QuestionDAO> loadQueries() throws IOException, SQLException {
		// setting up database connections
		DatabaseConnection dc = new DatabaseConnection(DatabaseType.STACKOVERFLOW);
		Connection conn = dc.getConnection();
		conn.setAutoCommit(false);
		List<QuestionDAO> result = new ArrayList<QuestionDAO>();
		LOGGER.log(Level.INFO, "retrieving queries..");
		String query = "select Id, Title, AcceptedAnswerId from stack_overflow.questions limit 10;";
		try (Statement stmt = conn.createStatement()) {
			stmt.setFetchSize(Integer.MIN_VALUE);
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				String id = rs.getString("Id");
				String body = rs.getString("Title");
				String acceptedAnswerId = rs.getString("AcceptedAnswerId");
				QuestionDAO dao = new QuestionDAO(id, body, acceptedAnswerId);
				result.add(dao);
			}
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		dc.closeConnection();
		return result;
	}
}