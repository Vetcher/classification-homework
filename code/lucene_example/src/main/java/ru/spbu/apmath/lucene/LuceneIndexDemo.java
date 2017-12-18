package ru.spbu.apmath.lucene;


import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LuceneIndexDemo {

    private static final Logger LOG = Logger.getLogger(LuceneExample.class);

    private static final MyAnalyzer ANALYZER = new MyAnalyzer(new HashSet<String>(), false);

    private static void tokenizerDemo() throws IOException {
        final String document = "Привет к миру";

        final Analyzer an = new RussianAnalyzer();
        final TokenStream stream = an.tokenStream(null, document);
        stream.reset();
        while (stream.incrementToken()) {
            final OffsetAttribute off = stream.addAttribute(OffsetAttribute.class);
            final CharTermAttribute text = stream.addAttribute(CharTermAttribute.class);

            //System.out.println(stream.reflectAsString(false));
            System.out.println(text.toString() + " " + off.startOffset());
        }

        stream.close();
    }


    public static void main(final String[] args) throws IOException {
        final Directory directory = makeToyIndex();
        final IndexReader indexReader = DirectoryReader.open(directory);

        //tokenizerDemo();
        searh(indexReader);
        printTerms("body", indexReader);
        printDocsTitles("title", indexReader);
        printDocVector(2, "body", indexReader);
    }

    private static Directory makeToyIndex() throws IOException {
        final Directory directory = new RAMDirectory();

        final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(ANALYZER);
        final IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        int id = 0;
        for (final String text : IOUtils.readLines(LuceneIndexDemo.class.getResourceAsStream("/texts.txt"))) {
            indexWriter.addDocument(makeDocument(id, "Document #" + id, text));
            id++;
        }

        indexWriter.close();

        return directory;
    }

    private static void printTerms(final String field, final IndexReader indexReader) throws IOException {
        final Terms terms = MultiFields.getTerms(indexReader, field);
        final TermsEnum iterator = terms.iterator();

        BytesRef byteRef = null;

        System.out.println("\n\nПечатаем все проиндексированные токены: ");
        while ((byteRef = iterator.next()) != null) {
            final String docTerm = byteRef.utf8ToString();
            final PostingsEnum postings = iterator.postings(null, PostingsEnum.ALL);

            int docId = 0;
            while ((docId = postings.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                final int freq = postings.freq();
                for (int i = 0; i < freq; i++) {
                    final int position = postings.nextPosition();
                    final int start = postings.startOffset();
                    final int end = postings.endOffset();
                    System.out.format("%s [luceneDoc=%d, pos=%d, start=%d, end=%d]\n", docTerm, docId, position, start, end);
                }
            }
        }
    }

    private static void searh(final IndexReader reader) throws IOException {
        final IndexSearcher is = new IndexSearcher(reader);

        final Query q = new TermQuery(new Term("body", "antarctic"));
        is.setSimilarity(new BM25Similarity());

        System.out.println("\n\nДокументы, содержащие 'antarctic': ");
        final TopDocs td = is.search(q, 10);
        for (final ScoreDoc scoreDoc : td.scoreDocs) {
            System.out.println(scoreDoc.toString());
        }
    }

    private static void printDocsTitles(final String field, final IndexReader indexReader) throws IOException {
        final BinaryDocValues dv = MultiDocValues.getBinaryValues(indexReader, field);

        System.out.println("\n\nПечатаем заголовки всех документов: ");
        int docId = 0;
        while((docId = dv.nextDoc()) != BinaryDocValues.NO_MORE_DOCS) {
            final BytesRef br = dv.binaryValue();
            System.out.println(br.utf8ToString());
        }
    }

    private static void printDocVector(final int docId, final String field, final IndexReader indexReader) throws IOException {
        final Terms terms = indexReader.getTermVector(docId, field);
        final TermsEnum te = terms.iterator();

        System.out.println("\n\nПечатаем содержимое одного поля одного документа: ");
        BytesRef term;
        while((term = te.next()) != null) {
            final PostingsEnum docPosEnum = te.postings(null, PostingsEnum.ALL);
            final String docTerm = term.utf8ToString();

            // В действительности цикл сработает только один раз
            while (docPosEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                for (int i = 0; i < docPosEnum.freq(); i++) {
                    final int position = docPosEnum.nextPosition();
                    final int start = docPosEnum.startOffset();
                    final int end = docPosEnum.endOffset();

                    System.out.format("%s [luceneDoc=%d, pos=%d, start=%d, end=%d]\n", docTerm, docId, position, start, end);
                }
            }
        }
    }

    private static final FieldType textsFld = new FieldType();
    static {
        textsFld.setTokenized(true);
        textsFld.setStored(true);
        textsFld.setStoreTermVectors(true);
        textsFld.setStoreTermVectorPositions(true);
        textsFld.setStoreTermVectorOffsets(true);
        textsFld.setStoreTermVectorPayloads(true);
        // !!!
        textsFld.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        // !!!
        textsFld.freeze();
    }

    private static Document makeDocument(final int id, final String title, final String body) throws IOException {
        final Document doc = new Document();

        doc.add(new StoredField("id", id));

        doc.add(new Field("body", body, textsFld));

        doc.add(new Field("title", title, textsFld));
        doc.add(new BinaryDocValuesField("title", new BytesRef(title)));

        doc.add(new Field("content", title + " " + body, textsFld));

        /*
         Если нужно использовать нестандартный аналайзер (не тот, что в IndexWriterConfig), то можно просто писать в Field TokenStream:

         doc.add(new Field("bodyText", ANALYZER.tokenStream("bodyText", body), textsFld))
         */

        return doc;
    }


    public static class MyAnalyzer extends StopwordAnalyzerBase {

        private final boolean isStem;

        public MyAnalyzer(final Set<String> stopWords, final boolean isStem) {
            super( new CharArraySet(stopWords, true));
            this.isStem = isStem;
        }

        @Override
        protected TokenStreamComponents createComponents(final String fieldName) {
            final Tokenizer source = new StandardTokenizer();
            TokenStream result = new StandardFilter(source);

            result = new EnglishPossessiveFilter(result);

            result = new LowerCaseFilter(result);
            result = new StopFilter(result, stopwords);

            if (isStem) {
                result = new PorterStemFilter(result);
            }
            return new TokenStreamComponents(source, result);
        }
    }

    static {
        final PatternLayout layout = new PatternLayout("%d{dd.MM.yy HH:mm:ss} %5p [%t] %c{1} - %m%n");
        Logger.getRootLogger().addAppender(new ConsoleAppender(layout));
    }
}
