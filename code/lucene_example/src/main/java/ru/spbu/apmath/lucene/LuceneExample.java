package ru.spbu.apmath.lucene;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

public class LuceneExample {
    // Объявляем статическую переменную - экземпляр логгера. Здесь мы испольуем библиотеку log4j (http://ru.wikipedia.org/wiki/Log4j).
    private static final Logger LOG = Logger.getLogger(LuceneExample.class);

    public static void main(final String[] args) throws Exception {
        // Инициализируем логгер (сам метод ниже)
        initLogger();

        RussianAnalyzer an = new RussianAnalyzer();
        TokenStream stream = an.tokenStream("body",
                "Южная граница Арктики совпадает с северной границей зоны тундры. Площадь — около 27 млн км²;");
        stream.reset();
        while (stream.incrementToken()) {
            System.out.println(stream.reflectAsString(false));
        }

        // Тексты трех документов, которые мы будем индексировать
        final String document1 = "Hello, world!";
        final String document2 = "World is a common name for the whole of human civilization";
        final String document3 = "Antarctica is Earth's southernmost continent, containing the geographic South Pole.";

        // Создаем экземпляр класса Analyzer для Английского языка. Этот класс отвечает за: разделение предложения на слова,
        // стемминг, фильтрацию стоп-слов
        final Analyzer enAnalyzer = new EnglishAnalyzer();

        try {
            LOG.info("Creating index");
            /* Место где будет располагаться наш индекс. Обычно это директория на файловой системе, но для тестирования принято
               использовать RAMDirectory - тогда все данные будут храниться в памяти и исчезнут сразу после завершения программы.
               Для того чтобы создать директорию на файловой системе, нужно использовать метод FSDirectory.open();
            */
            final Directory directory = new RAMDirectory();

            // Будем использовать английский аналайзер
            final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(enAnalyzer);

            //  Создаем новый индекс.
            final IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

            LOG.info("Indexing");

            // Добавляем три документа. Описание метода createDocument ниже.
            indexWriter.addDocument(createDocument("Document #1", document1));
            indexWriter.addDocument(createDocument("Document #2", document2));
            indexWriter.addDocument(createDocument("Document #3", document3));

            //Закрываем индекс.
            indexWriter.close();

            LOG.info("Reading index");

            // Создаем объект для чтения индекса.
            final IndexReader indexReader = DirectoryReader.open(directory);

            // Выводим в консоль:
            // Сколько документов содержат слово 'world'
            System.out.println(indexReader.docFreq(new Term("body", "world")));
            // Сколько документов содержат слово 'hello'
            System.out.println(indexReader.docFreq(new Term("body", "hello")));
            // Сколько документов в заголовке содержат слово 'document'
            System.out.println(indexReader.docFreq(new Term("title", "document")));

            // Выводим все документы, которые содержат слово 'world' и сколько раз слово в них встречается
            final PostingsEnum pe = MultiFields.getTermDocsEnum(indexReader,
                    "body", new BytesRef("world"));
            int doc;
            while((doc = pe.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                System.out.println(indexReader.document(doc).get("title"));
            }
        } catch (IOException e) {
            LOG.error("Error occurred", e);
        }
    }

    private static Document createDocument(final String titleText, final String bodyText) {
        /*
            Документ в Lucene - это набор полей (пар ключ-значение). Ключ - это некоторые данные, которые характеризуют документ,
            например - место расположения, заголовок (title), основной текст (body), и т.д. Значение - это значение данных по ключу.
         */
        // Создаем пустой документ
        final Document doc = new Document();

        final FieldType ft = new FieldType();
        ft.setStored(true);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        ft.setTokenized(true);

        // добавляем в документ поле 'title'. Исходный текст будет храниться в индексе и будет обработан Analyzer'om.
        doc.add(new Field("title", titleText, ft));
        // добавляем в документ поле 'body'. Исходный текст будет храниться в индексе и будет обработан Analyzer'om.
        doc.add(new Field("body", bodyText, ft));

        return doc;
    }

    private static void initLogger() {
        // Инициализируемо логгер. Обычно это делается в специальном конфигурационном файле, где указываться куда и в каком
        // формате писать логи (можно писать в файл, на консоль, слать электронной почтой, слать по сети).
        // Тут мы просто задаем формат - выводить сначала дату, время, тип события, имя логгера, текст события.
        final PatternLayout layout = new PatternLayout("%d{dd.MM.yy HH:mm:ss} %5p [%t] %c{1} - %m%n");
        Logger.getRootLogger().addAppender(new ConsoleAppender(layout));
    }
}
