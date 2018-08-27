package irstyle;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import database.DatabaseConnection;
import database.DatabaseType;
import indexing.BiwordAnalyzer;
import irstyle.api.Indexer;

public class BuildIndexForLM {

	public static void main(String[] args) throws SQLException, IOException {
		List<String> argsList = Arrays.asList(args);
		String suffix;
		int[] limit;
		if (argsList.contains("-inexp")) {
			limit = WikiConstants.precisionLimit;
			suffix = "p20";
		} else if (argsList.contains("-inexr")) {
			limit = WikiConstants.recallLimit;
			suffix = "rec";
		} else {
			limit = WikiConstants.mrrLimit;
			suffix = "mrr";
		}
		Analyzer analyzer = null;
		if (argsList.contains("-bi")) {
			suffix += "_bi";
			analyzer = new BiwordAnalyzer();
		} else {
			analyzer = new StandardAnalyzer();
		}
		String[] tableName = { "tbl_article_wiki13", "tbl_image_pop", "tbl_link_pop" };
		String[][] textAttribs = new String[][] { { "title", "text" }, { "src" }, { "url" } };
		try (DatabaseConnection dc = new DatabaseConnection(DatabaseType.WIKIPEDIA)) {
			// building index for LM
			Directory cacheDirectory = FSDirectory
					.open(Paths.get(WikiIndexer.DATA_WIKIPEDIA + "lm_cache_" + suffix));
			IndexWriterConfig cacheConfig = Indexer.getIndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE);
			Directory restDirectory = FSDirectory
					.open(Paths.get(WikiIndexer.DATA_WIKIPEDIA + "lm_rest_" + suffix));
			Directory allDirectory = FSDirectory
					.open(Paths.get(WikiIndexer.DATA_WIKIPEDIA + "lm_all_" + suffix));
			IndexWriterConfig restConfig = Indexer.getIndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE);
			IndexWriterConfig allConfig = Indexer.getIndexWriterConfig(analyzer);
			try (IndexWriter cacheWriter = new IndexWriter(cacheDirectory, cacheConfig);
					IndexWriter restWriter = new IndexWriter(restDirectory, restConfig);
					IndexWriter allWriter = new IndexWriter(allDirectory, allConfig)) {
				for (int i = 0; i < tableName.length; i++) {
					System.out.println("Indexing table " + tableName[i]);
					Indexer.indexTable(dc, cacheWriter, tableName[i], textAttribs[i], limit[i], "popularity", false);
					Indexer.indexTable(dc, restWriter, tableName[i], textAttribs[i],
							WikiConstants.size[i] - limit[i], "popularity", true);
					// Indexer.indexTable(dc, allWriter, tableName[i], textAttribs[i],
					// ExperimentConstants.size[i], "popularity", false);
				}
			}
		}
		analyzer.close();
	}
}
