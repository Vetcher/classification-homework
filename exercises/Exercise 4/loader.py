import lxml.html
import lxml.etree
import requests
from datetime import timedelta, datetime
from sqlalchemy import func, create_engine
from sqlalchemy import Column, Integer, Text, DateTime
from sqlalchemy.schema import Index
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base

def get(link, cat):
    url = 'https://ria.ru/'+ cat + link
    return lxml.html.fromstring(requests.get(url).text)
	
def daterange(start_date, number_of_days):
    for n in range(number_of_days):
        yield start_date - timedelta(n)
		
if __name__ == '__main__':
	categories = ['politics', 'society', 'economy', 'world', 'incidents', 'science', 'culture', 'religion']
	amount_of_days = 400
	today = datetime.now()
	total = 0
	for cat in categories:
		i = 0
		d = 0
		for day in daterange(today, amount_of_days):
			d += 1
			#print(cat, datetime.strftime(day, '%d.%m.%Y'))
			try:
				tree = get(datetime.strftime(day, '/%Y%m%d/'), cat)
			except Exception as e:
				print('Exeption', e, 'on', cat, datetime.strftime(day, '%d.%m.%Y'))
				continue
			title_items = tree.xpath('//div[@class="b-list__item "]/a/span[@class="b-list__item-title"]/span')
			for title in title_items:
				if title.text is None:
					continue
				news = News(title.text, cat, datetime.strftime(day, '%d.%m.%Y'))
				db_session.add(news)
				i += 1
			db_session.commit()
			if d%50 == 0:
				print('+', i) 
		total += i
		print(cat, total, '+', i)