package cache_enhancement;

import indexing.InexFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import wiki13.WikiFileIndexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WikiIndexUpdater extends IndexManipulator{

    public static final Logger LOGGER = Logger.getLogger(WikiIndexUpdater.class.getName());
    final public Map<String, String> docNumberInfo;

    public WikiIndexUpdater(String indexPath, String wikiCountPathFilePath) {
        super(indexPath);
        docNumberInfo = new HashMap<>();
        File countPath = new File(wikiCountPathFilePath);
        try {
            FileReader fileReader = new FileReader(countPath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.contains(","))
                    continue;

                String path = line.split(",", 1)[0];
                File file =  new File(path);
                String docNum = FilenameUtils.removeExtension(file.getName());
                docNumberInfo.put(docNum, line);
            }
        }
        catch (Exception e){
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void search(String queryStr, int hitsPerPage) throws ParseException, IOException {
        IndexReader indexReader = DirectoryReader.open(indexWriter.getDirectory());
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q = new QueryParser(WikiFileIndexer.CONTENT_ATTRIB, new StandardAnalyzer()).parse(queryStr);
        TopDocs docs = indexSearcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0; i<hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = indexSearcher.doc(docId);
            System.out.println((i + 1) + ". " +
                    d.get(WikiFileIndexer.DOCNAME_ATTRIB) + "\t" +
                    d.get(WikiFileIndexer.TITLE_ATTRIB) + "\t" +
                    d.get(WikiFileIndexer.WEIGHT_ATTRIB));
        }
    }

    protected InexFile makeInexFile(String csvTextLine) {

        String[] fields = csvTextLine.split(",");
        String path = fields[0];
        Double count = Double.parseDouble(fields[1].trim());

        if (fields.length == 3) {
            String title = fields[2].trim();
            return (new InexFile(path, count, title));
        } else {
            return (new InexFile(path, count));
        }
    }

    @Override
    public boolean addDoc(String docId) {
        InexFile inexFile = makeInexFile(docNumberInfo.get(docId));
        WikiFileIndexer wikiFileIndexer = new WikiFileIndexer();
        boolean failed = !wikiFileIndexer.index(inexFile, indexWriter);
        return failed;
    }

    @Override
    public int addDoc(List<String> docIds) {
        int failed = 0;
        for(String docId : docIds){
            if(addDoc(docId))
                failed += 1;
        }
        return failed;
    }

    @Override
    public boolean removeDoc(String docId) {
        boolean failed = false;
        try {
            Query q = new QueryParser(WikiFileIndexer.DOCNAME_ATTRIB, new StandardAnalyzer()).parse(docId);
            indexWriter.deleteDocuments(q);
        } catch (Exception e) {
            failed = true;
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return failed;
    }

    @Override
    public int removeDoc(List<String> docIds) {
        int failed = 0;
        for(String docId: docIds) {
            if(removeDoc(docId))
                failed += 1;
        }
        return failed;
    }

}
