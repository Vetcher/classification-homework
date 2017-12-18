#!/usr/bin/env python
# -*- coding: utf-8 -*-

import logging
import os

from flask import render_template, request, redirect, url_for, session, abort

from app import app

LOG = logging.getLogger('access')


@app.route('/', methods=['GET', 'POST'])
def index():     
    LOG.info('Access: %s, %s, %s' % (request.remote_addr, request.args, request.form))

    results = {}
    
    for k_get, v_get in request.args.items():
        results[k_get] = v_get
        
    for k_form, v_form in request.form.items():
        results[k_form] = v_form
    
    return render_template('index.html', results=results)
    
    
    