#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

from flask import render_template, request

from app import app
from index import es

@app.route('/', methods=['GET', 'POST'])
def index():
    results = {}
    for k_form, v_form in request.form.items(): # It's a trick, there should be only 1 iteration
        found = es.search(index='chgk-index', doc_type='chgk', q=v_form)
        results[v_form] = [hit["_source"] for hit in found['hits']['hits']]

    return render_template('index.html', results=results)
