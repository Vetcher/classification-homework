from elasticsearch import Elasticsearch
from sqlalchemy import func, create_engine
from sqlalchemy import Column, Integer, Text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
from tqdm import tqdm

Base = declarative_base()


class Question(Base):
    __tablename__ = 'questions'
    id = Column(Integer, primary_key=True)
    question = Column(Text, nullable=False)
    answer = Column(Text, nullable=False)
    
    def __init__(self, text, answer):
        self.question = text
        self.answer = answer


def init_db(db_url):
    engine = create_engine(db_url)
    Base.metadata.bind = engine
    Base.metadata.create_all()
    return sessionmaker(bind=engine)


db_session = init_db('sqlite:///chgk.db')()

unique_index = 'chgk-index'
es = Elasticsearch()

try:
    es.indices.create(index=unique_index)
    print('create index')
    for instance in tqdm(db_session.query(Question)):
        es.index(index=unique_index, doc_type='chgk', id=instance.id, body={'question': instance.question, 'answer': instance.answer})
    print('new index finished')
except Exception:
    #es.indices.delete(index=unique_index, ignore=[400, 404])
    print('index already exist')