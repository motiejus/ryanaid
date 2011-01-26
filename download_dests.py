#!/usr/bin/env python

try:
    import json # 2.6 and above
except ImportError:
    import simplejson as json # 2.5 and below

import httplib, re, logging

def get_dests(logger=None):
    if logger is None:
        logging.basicConfig(level=logging.DEBUG)
        logger = logging.getLogger('RyanairDest')

    conn = httplib.HTTPConnection('www.ryanair.com')
    logger.debug("Sending request for destinations array")
    conn.request("GET", '/en/booking/form')
    resp = conn.getresponse()
    logger.info("Request success, response code: %d" % resp.status)
    if (resp.status != 200):
        raise RyanairException("Invalid return status from Ryanair. \
                Expected: 200, got: %d" % resp.status)
    html = resp.read()
    airport_part = re.search('var Dests=.*\s*([\S\s]*?);', html)
    if airport_part is None:
        raise RyanairException("Parsing of ryanair airports changed.")

    airport_arr = re.split(",\n", airport_part.group(1))
    logger.info("%d airports found" % int(len(airport_arr)/2))
    airport_dirs, airport_names = {}, {}
    for it in airport_arr:
        # Example: sKUN='Kaunas'
        if it[0] == 's':
            airport_names[it[1:4]] = it[6:-1]
        # Example line: aKUN=aKUN=['STN','BGY','RYG','BVA','TMP']
        else:
            short, dirs = it[1:4], it[5:]
            # Expand dirs (this looks like json array, but improper)
            airport_dirs[short] = json.loads(dirs.replace("'", '"'))
    return airport_names, airport_dirs

if __name__ == "__main__":
    logging.basicConfig(level=logging.ERROR)
    logger = logging.getLogger('RyanairDest')

    names, dests = get_dests(logger)
    for k, v in dests.iteritems():
        for dest in v:
            print ("%s-%s" % (k, dest) )
