from sqlalchemy import func, create_engine
from sqlalchemy import Column, Integer, Text, DateTime
from sqlalchemy.schema import Index
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.linear_model import SGDClassifier
from sklearn.pipeline import Pipeline

Base = declarative_base()

class News(Base):
    __tablename__ = 'news'
    id = Column(Integer, primary_key=True)
    title = Column(Text, nullable=False)
    category = Column(Text, nullable=False)
    date = Column(Text, nullable=False)
    
    def __init__(self, title, category, date):
        self.title = title
        self.category = category
        self.date = date


def init_db(db_url):
    engine = create_engine(db_url)
    Base.metadata.bind = engine
    Base.metadata.create_all()
    return sessionmaker(bind=engine)

db_session = init_db('sqlite:///ria.ru.db')()

classifier = Pipeline([('vectorizer', CountVectorizer(max_df=0.2, ngram_range=(1,2))),
                         ('tfidf', TfidfTransformer(norm='l2')),
                         ('clf', SGDClassifier(penalty='l2'))])

classifier = classifier.fit([n.title.lower() for n in db_session.query(News).order_by(News.id)],
                       [n.category for n in db_session.query(News).order_by(News.id)])